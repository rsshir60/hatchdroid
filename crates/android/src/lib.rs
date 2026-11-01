use std::path::PathBuf;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use hatchzip_core::archive::storage::Reads;
use hatchzip_core::engine::{self, EnginePhase, Job, Outcome, Progress, Run};
use hatchzip_core::session::{self, State};
use zeroize::Zeroize;

mod cancel;
pub use cancel::*;

uniffi::setup_scaffolding!();

// ── Data types exposed to Kotlin ──

#[derive(Clone, Debug, uniffi::Record)]
pub struct SpacePlanResult {
    pub free_bytes: u64,
    pub needed_bytes: u64,
    pub keep_free_bytes: u64,
    pub fits: bool,
    pub free_after_bytes: u64,
    pub keep_archive: bool,
    pub in_place_needs: u64,
    pub normal_needs: u64,
    pub reclaims: bool,
}

#[derive(Clone, Debug, uniffi::Record)]
pub struct ArchiveInfo {
    pub format: String,
    pub item_count: u32,
    pub total_size: u64,
    pub encrypted: bool,
    pub single_root: bool,
    pub display_name: String,
    pub default_output_dir: String,
    pub in_place_refusal: Option<String>,
}

#[derive(Clone, Debug, PartialEq, Eq, uniffi::Enum)]
pub enum ExtractionPhase {
    ScanningIndex,
    DecodingEntry,
    FlushingFsync,
    TruncatingTail,
    Completed,
    Paused,
    Failed { message: String },
}

#[derive(Clone, Debug, uniffi::Record)]
pub struct ExtractionTelemetry {
    pub phase: ExtractionPhase,
    pub current_entry_name: String,
    pub entries_completed: u64,
    pub entries_total: u64,
    pub bytes_hatched: u64,
    pub bytes_uncompressed_total: u64,
    pub bytes_batch_staged: u64,
    pub bytes_archive_reclaimed: u64,
    pub bytes_archive_initial: u64,
}

// ── Callback interface for progress telemetry ──

#[uniffi::export(callback_interface)]
pub trait ExtractionProgressListener: Send + Sync {
    fn on_telemetry(&self, snapshot: ExtractionTelemetry);
}

// ── Extraction handle for cancellation ──

#[derive(uniffi::Object)]
pub struct ExtractionHandle {
    pub(crate) token: Arc<CancellationToken>,
}

#[uniffi::export]
impl ExtractionHandle {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            token: CancellationToken::new(),
        })
    }

    pub fn cancel(&self) {
        self.token.cancel();
    }

    pub fn is_cancelled(&self) -> bool {
        self.token.is_cancelled()
    }

    pub fn token(&self) -> Arc<CancellationToken> {
        self.token.clone()
    }
}

// ── Error types ──

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum HatchZipError {
    #[error("Archive error: {detail}")]
    ArchiveError { detail: String },
    #[error("Space error: {detail}")]
    SpaceError { detail: String },
    #[error("Permission error: {detail}")]
    PermissionError { detail: String },
    #[error("Password required: {detail}")]
    PasswordRequired { detail: String },
    #[error("Invalid password: {detail}")]
    InvalidPassword { detail: String },
    #[error("Operation cancelled")]
    Cancelled,
}

// ── Top-level functions ──

/// Format byte counts into human-readable strings (e.g., "1.4 GB")
#[uniffi::export]
pub fn format_bytes(bytes: u64) -> String {
    engine::human(bytes)
}

/// Pre-flight: inspect an archive without modifying disk contents.
#[uniffi::export]
pub fn check_archive(path: String, password: Option<String>) -> Result<ArchiveInfo, HatchZipError> {
    let p = PathBuf::from(&path);
    let (primary, _paths) = session::locate(&p)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
    let reads = Arc::new(Reads::default());
    let job = match Job::open(&primary, false, password.as_deref(), reads) {
        Ok(job) => job,
        Err(e) => {
            if e.chain().any(|c| c.is::<hatchzip_core::archive::WrongPassword>()) {
                return Err(HatchZipError::InvalidPassword { detail: "Incorrect password for archive".into() });
            }
            if e.chain().any(|c| c.is::<hatchzip_core::archive::PasswordNeeded>()) {
                return Err(HatchZipError::PasswordRequired { detail: "Archive is encrypted and requires a password".into() });
            }
            return Err(HatchZipError::ArchiveError { detail: format!("{e:#}") });
        }
    };

    let default_output = session::default_output(&primary);

    Ok(ArchiveInfo {
        format: job.format_name.to_string(),
        item_count: job.item_count() as u32,
        total_size: job.remaining_size(),
        encrypted: job.encrypted(),
        single_root: session::single_root(&job),
        display_name: session::display_name(&job),
        default_output_dir: default_output.to_string_lossy().into_owned(),
        in_place_refusal: job.in_place_refusal(),
    })
}

/// Pre-flight: compute the space plan.
#[uniffi::export]
pub fn plan_extraction(
    path: String,
    output_dir: Option<String>,
    keep_free_bytes: u64,
    keep_archive: bool,
) -> Result<SpacePlanResult, HatchZipError> {
    let p = PathBuf::from(&path);
    let (primary, paths) = session::locate(&p)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
    let out = output_dir.map(PathBuf::from);
    let reads = Arc::new(Reads::default());
    let state = session::open(&primary, &paths, out.clone(), false, None, &reads)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

    let (job, target_dir, resuming) = match state {
        State::Fresh(job, out_path) => (job, out_path, false),
        State::Resume(job, journal) => (job, journal.output.clone(), true),
        State::Orphan(journal) => {
            return Err(HatchZipError::ArchiveError {
                detail: format!("Archive already extracted to {}", journal.output.display()),
            });
        }
    };

    let plan = session::plan(&job, &target_dir, resuming, keep_free_bytes, keep_archive)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

    Ok(SpacePlanResult {
        free_bytes: plan.free,
        needed_bytes: plan.needed,
        keep_free_bytes: plan.keep_free,
        fits: plan.fits,
        free_after_bytes: plan.free_after,
        keep_archive: plan.keep_archive,
        in_place_needs: plan.in_place_needs,
        normal_needs: plan.normal_needs,
        reclaims: plan.reclaims,
    })
}

// ── Throttled Telemetry Reporter ──

struct TelemetryReporter {
    listener: Box<dyn ExtractionProgressListener>,
    state: Mutex<ExtractionTelemetry>,
    last_emit: Mutex<Instant>,
    bytes_since_emit: AtomicU64,
    compression_ratio: f64,
}

impl TelemetryReporter {
    const MIN_EMIT_INTERVAL: Duration = Duration::from_millis(16); // ~60 FPS
    const MIN_EMIT_BYTES: u64 = 512 * 1024; // 512 KB

    fn new(listener: Box<dyn ExtractionProgressListener>, initial_state: ExtractionTelemetry, compression_ratio: f64) -> Self {
        Self {
            listener,
            state: Mutex::new(initial_state),
            last_emit: Mutex::new(Instant::now() - Duration::from_secs(1)),
            bytes_since_emit: AtomicU64::new(0),
            compression_ratio,
        }
    }

    fn emit_phase(&self, phase: ExtractionPhase) {
        let mut st = self.state.lock().unwrap();
        st.phase = phase;
        self.listener.on_telemetry(st.clone());
        *self.last_emit.lock().unwrap() = Instant::now();
        self.bytes_since_emit.store(0, Ordering::Relaxed);
    }

    fn emit_entry(&self, name: &str, _size: u64) {
        let mut st = self.state.lock().unwrap();
        st.current_entry_name = name.to_string();
        self.listener.on_telemetry(st.clone());
    }

    fn on_bytes(&self, delta: u64) {
        let since = self.bytes_since_emit.fetch_add(delta, Ordering::Relaxed) + delta;
        let should_emit = since >= Self::MIN_EMIT_BYTES || {
            let last = *self.last_emit.lock().unwrap();
            last.elapsed() >= Self::MIN_EMIT_INTERVAL
        };

        if should_emit {
            let mut st = self.state.lock().unwrap();
            st.bytes_hatched = st.bytes_hatched.saturating_add(since).min(st.bytes_uncompressed_total);
            let staged_delta = ((since as f64) * self.compression_ratio) as u64;
            st.bytes_batch_staged = st.bytes_batch_staged.saturating_add(staged_delta).min(
                st.bytes_archive_initial.saturating_sub(st.bytes_archive_reclaimed)
            );
            self.listener.on_telemetry(st.clone());
            *self.last_emit.lock().unwrap() = Instant::now();
            self.bytes_since_emit.store(0, Ordering::Relaxed);
        }
    }

    fn on_commit(&self, new_archive_len: u64) {
        let mut st = self.state.lock().unwrap();
        st.bytes_archive_reclaimed = st.bytes_archive_initial.saturating_sub(new_archive_len);
        st.bytes_batch_staged = 0;
        st.entries_completed = st.entries_completed.saturating_add(1);
        st.phase = ExtractionPhase::DecodingEntry;
        self.listener.on_telemetry(st.clone());
        *self.last_emit.lock().unwrap() = Instant::now();
        self.bytes_since_emit.store(0, Ordering::Relaxed);
    }
}

/// Executes an extraction with 60 FPS throttled telemetry and password validation.
#[uniffi::export]
pub fn extract_archive(
    handle: Arc<ExtractionHandle>,
    path: String,
    output_dir: Option<String>,
    keep_free_bytes: u64,
    keep_archive: bool,
    mut password: Option<String>,
    listener: Box<dyn ExtractionProgressListener>,
) -> Result<(), HatchZipError> {
    let p = PathBuf::from(&path);
    let (primary, paths) = session::locate(&p)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

    let out = output_dir.map(PathBuf::from);
    let reads = Arc::new(Reads::default());
    let state = match session::open(
        &primary,
        &paths,
        out,
        true,
        password.as_deref(),
        &reads,
    ) {
        Ok(s) => s,
        Err(e) => {
            if e.chain().any(|c| c.is::<hatchzip_core::archive::WrongPassword>()) {
                return Err(HatchZipError::InvalidPassword { detail: "Incorrect password provided".into() });
            }
            if e.chain().any(|c| c.is::<hatchzip_core::archive::PasswordNeeded>()) {
                return Err(HatchZipError::PasswordRequired { detail: "This archive is encrypted and requires a password".into() });
            }
            return Err(HatchZipError::ArchiveError { detail: format!("{e:#}") });
        }
    };

    let (job, journal) = match state {
        State::Fresh(job, target_out) => {
            // Verify password if encrypted BEFORE any modification or ftruncate
            if job.encrypted() {
                if password.is_none() {
                    return Err(HatchZipError::PasswordRequired {
                        detail: "This archive is encrypted and requires a password".into(),
                    });
                }
                if let Err(e) = job.check_password() {
                    if e.chain().any(|c| c.is::<hatchzip_core::archive::WrongPassword>()) {
                        return Err(HatchZipError::InvalidPassword {
                            detail: "Incorrect password provided".into(),
                        });
                    }
                    return Err(HatchZipError::ArchiveError { detail: format!("{e:#}") });
                }
            }

            let plan = session::plan(&job, &target_out, false, keep_free_bytes, keep_archive)
                .map_err(|e| HatchZipError::SpaceError { detail: format!("{e:#}") })?;
            if !plan.fits {
                let err_msg = format!(
                    "Not enough space. Required {} (with {} buffer) but only {} is free.",
                    engine::human(plan.needed),
                    engine::human(keep_free_bytes),
                    engine::human(plan.free),
                );
                return Err(HatchZipError::SpaceError { detail: err_msg });
            }
            let journal = session::begin(&job, &paths, target_out, keep_archive, None)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            (job, journal)
        }
        State::Resume(job, journal) => {
            if job.encrypted() {
                if password.is_none() {
                    return Err(HatchZipError::PasswordRequired {
                        detail: "This archive is encrypted and requires a password to resume".into(),
                    });
                }
                if let Err(e) = job.check_password() {
                    if e.chain().any(|c| c.is::<hatchzip_core::archive::WrongPassword>()) {
                        return Err(HatchZipError::InvalidPassword {
                            detail: "Incorrect password provided".into(),
                        });
                    }
                    return Err(HatchZipError::ArchiveError { detail: format!("{e:#}") });
                }
            }
            session::clear_uncommitted(&job, &journal.output)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            (job, journal)
        }
        State::Orphan(journal) => {
            engine::finish_orphaned(&journal, &paths)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            listener.on_telemetry(ExtractionTelemetry {
                phase: ExtractionPhase::Completed,
                current_entry_name: "Completed".into(),
                entries_completed: 0,
                entries_total: 0,
                bytes_hatched: 0,
                bytes_uncompressed_total: 0,
                bytes_batch_staged: 0,
                bytes_archive_reclaimed: 0,
                bytes_archive_initial: 0,
            });
            return Ok(());
        }
    };

    if handle.is_cancelled() {
        return Ok(());
    }

    let archive_len = job.storage.original_len();
    let total_uncompressed = job.remaining_size().max(1);
    let compression_ratio = if total_uncompressed == 0 {
        1.0
    } else {
        (archive_len as f64) / (total_uncompressed as f64)
    };

    let initial_telemetry = ExtractionTelemetry {
        phase: ExtractionPhase::DecodingEntry,
        current_entry_name: "Preparing...".into(),
        entries_completed: 0,
        entries_total: job.item_count() as u64,
        bytes_hatched: 0,
        bytes_uncompressed_total: total_uncompressed,
        bytes_batch_staged: 0,
        bytes_archive_reclaimed: 0,
        bytes_archive_initial: archive_len,
    };

    let reporter = Arc::new(TelemetryReporter::new(listener, initial_telemetry, compression_ratio));
    let r1 = reporter.clone();
    let r2 = reporter.clone();
    let r3 = reporter.clone();
    let r4 = reporter.clone();

    let run = Run {
        job,
        journal,
        paths,
        stop: handle.token.atomic_bool(),
        progress: Progress {
            on_bytes: Box::new(move |n| {
                r1.on_bytes(n);
            }),
            on_commit: Box::new(move |len| {
                r2.on_commit(len);
            }),
            on_phase: Some(Box::new(move |ep| {
                let phase = match ep {
                    EnginePhase::DecodingEntry => ExtractionPhase::DecodingEntry,
                    EnginePhase::FlushingFsync => ExtractionPhase::FlushingFsync,
                    EnginePhase::TruncatingTail => ExtractionPhase::TruncatingTail,
                };
                r3.emit_phase(phase);
            })),
            on_entry: Some(Box::new(move |name, size| {
                r4.emit_entry(name, size);
            })),
        },
        keep_free: keep_free_bytes,
    };

    let outcome = run.execute();

    // Memory scrubbing: zeroize password from RAM
    if let Some(ref mut s) = password {
        s.zeroize();
    }

    match outcome {
        Ok(Outcome::Finished) => {
            reporter.emit_phase(ExtractionPhase::Completed);
            Ok(())
        }
        Ok(Outcome::Paused) => {
            reporter.emit_phase(ExtractionPhase::Paused);
            Ok(())
        }
        Err(e) => {
            let msg = format!("{e:#}");
            reporter.emit_phase(ExtractionPhase::Failed { message: msg.clone() });
            Err(HatchZipError::ArchiveError { detail: msg })
        }
    }
}

/// Check for interrupted extractions in a directory (`.hatchzip` files).
#[uniffi::export]
pub fn find_interrupted_jobs(directory: String) -> Vec<String> {
    let dir = PathBuf::from(&directory);
    let mut results = Vec::new();
    if let Ok(entries) = std::fs::read_dir(&dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
                if file_name.ends_with(".hatchzip") && !file_name.ends_with(".tmp") {
                    let base_name = &file_name[..file_name.len() - ".hatchzip".len()];
                    let archive_path = dir.join(base_name);
                    if archive_path.exists() {
                        results.push(archive_path.to_string_lossy().into_owned());
                    }
                }
            }
        }
    }
    results
}

/// Restore a half-extracted archive back to normal using HatchZip's restore logic.
#[uniffi::export]
pub fn restore_archive(path: String, output_dir: Option<String>) -> Result<u32, HatchZipError> {
    let p = PathBuf::from(&path);
    let out = output_dir.as_deref().map(std::path::Path::new);
    let restored = hatchzip_core::restore::restore(&p, out)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
    Ok(restored.entries as u32)
}
