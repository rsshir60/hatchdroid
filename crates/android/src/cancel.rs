use std::fs;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

#[derive(uniffi::Object)]
pub struct CancellationToken {
    cancelled: AtomicBool,
}

#[uniffi::export]
impl CancellationToken {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            cancelled: AtomicBool::new(false),
        })
    }

    pub fn cancel(&self) {
        self.cancelled.store(true, Ordering::SeqCst);
    }

    pub fn is_cancelled(&self) -> bool {
        self.cancelled.load(Ordering::Relaxed)
    }
}

impl CancellationToken {
    pub(crate) fn atomic_bool(&self) -> &AtomicBool {
        &self.cancelled
    }
}

/// RAII guard that automatically removes a partially extracted output file
/// if an error, panic, or user cancellation occurs before `commit()` is called.
pub struct PartialFileGuard {
    path: PathBuf,
    committed: bool,
}

impl PartialFileGuard {
    pub fn new(path: &Path) -> Self {
        Self {
            path: path.to_path_buf(),
            committed: false,
        }
    }

    /// Call ONLY after checksum validation and durable flush succeed.
    pub fn commit(mut self) {
        self.committed = true;
    }
}

impl Drop for PartialFileGuard {
    fn drop(&mut self) {
        if !self.committed && self.path.exists() {
            // Cleanly unlink the incomplete artifact so no corrupt files remain on disk
            let _ = fs::remove_file(&self.path);
        }
    }
}
