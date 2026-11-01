use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

use hatchzip_core::archive::storage::Reads;
use hatchzip_core::engine::{self, Job, Outcome, Progress, Run};
use hatchzip_core::session::{self, State};

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

#[derive(Clone, Debug, uniffi::Enum)]
pub enum ExtractionPhase {
    Checking,
    Planning,
    Extracting,
    Paused,
    Finished,
    Failed { message: String },
}

// ── Callback interface for progress ──

#[uniffi::export(callback_interface)]
pub trait ProgressCallback: Send + Sync {
    fn on_bytes_extracted(&self, bytes: u64);
    fn on_archive_shrunk(&self, new_archive_len: u64);
    fn on_phase_changed(&self, phase: ExtractionPhase);
    fn on_error(&self, message: String);
}

// ── Extraction handle for cancellation ──

#[derive(uniffi::Object)]
pub struct ExtractionHandle {
    pub(crate) stop: Arc<AtomicBool>,
}

#[uniffi::export]
impl ExtractionHandle {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            stop: Arc::new(AtomicBool::new(false)),
        })
    }

    pub fn cancel(&self) {
        self.stop.store(true, Ordering::Relaxed);
    }

    pub fn is_cancelled(&self) -> bool {
        self.stop.load(Ordering::Relaxed)
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
pub fn check_archive(path: String) -> Result<ArchiveInfo, HatchZipError> {
    let p = PathBuf::from(&path);
    let (primary, _paths) = session::locate(&p)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
    let reads = Arc::new(Reads::default());
    let job = Job::open(&primary, false, None, reads)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

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

/// Run extraction with the provided handle and progress callback.
/// Blocks the calling thread until completed or paused/cancelled.
#[uniffi::export]
pub fn extract_archive(
    handle: Arc<ExtractionHandle>,
    path: String,
    output_dir: Option<String>,
    keep_free_bytes: u64,
    keep_archive: bool,
    password: Option<String>,
    callback: Box<dyn ProgressCallback>,
) -> Result<(), HatchZipError> {
    callback.on_phase_changed(ExtractionPhase::Checking);

    let p = PathBuf::from(&path);
    let (primary, paths) = session::locate(&p)
        .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

    let out = output_dir.map(PathBuf::from);
    let reads = Arc::new(Reads::default());
    let state = session::open(
        &primary,
        &paths,
        out,
        true,
        password.as_deref(),
        &reads,
    ).map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;

    let (job, journal) = match state {
        State::Fresh(job, target_out) => {
            callback.on_phase_changed(ExtractionPhase::Planning);
            let plan = session::plan(&job, &target_out, false, keep_free_bytes, keep_archive)
                .map_err(|e| HatchZipError::SpaceError { detail: format!("{e:#}") })?;
            if !plan.fits {
                let err_msg = format!(
                    "Not enough space. Required {} (with {} buffer) but only {} is free.",
                    engine::human(plan.needed),
                    engine::human(keep_free_bytes),
                    engine::human(plan.free),
                );
                callback.on_error(err_msg.clone());
                return Err(HatchZipError::SpaceError { detail: err_msg });
            }
            let journal = session::begin(&job, &paths, target_out, keep_archive, None)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            (job, journal)
        }
        State::Resume(job, journal) => {
            callback.on_phase_changed(ExtractionPhase::Planning);
            session::clear_uncommitted(&job, &journal.output)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            (job, journal)
        }
        State::Orphan(journal) => {
            engine::finish_orphaned(&journal, &paths)
                .map_err(|e| HatchZipError::ArchiveError { detail: format!("{e:#}") })?;
            callback.on_phase_changed(ExtractionPhase::Finished);
            return Ok(());
        }
    };

    if handle.is_cancelled() {
        callback.on_phase_changed(ExtractionPhase::Paused);
        return Ok(());
    }

    callback.on_phase_changed(ExtractionPhase::Extracting);

    let cb = Arc::new(callback);
    let cb1 = cb.clone();
    let cb2 = cb.clone();

    let run = Run {
        job,
        journal,
        paths,
        stop: &handle.stop,
        progress: Progress {
            on_bytes: Box::new(move |n| {
                cb1.on_bytes_extracted(n);
            }),
            on_commit: Box::new(move |len| {
                cb2.on_archive_shrunk(len);
            }),
        },
        keep_free: keep_free_bytes,
    };

    match run.execute() {
        Ok(Outcome::Finished) => {
            cb.on_phase_changed(ExtractionPhase::Finished);
            Ok(())
        }
        Ok(Outcome::Paused) => {
            cb.on_phase_changed(ExtractionPhase::Paused);
            Ok(())
        }
        Err(e) => {
            let msg = format!("{e:#}");
            cb.on_error(msg.clone());
            cb.on_phase_changed(ExtractionPhase::Failed { message: msg.clone() });
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
