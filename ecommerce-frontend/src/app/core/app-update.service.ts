import { Injectable, inject } from '@angular/core';
import { SwUpdate } from '@angular/service-worker';
import { filter } from 'rxjs';

/** A new version found this soon after a launch is applied at once — the shopper hasn't started anything. */
const LAUNCH_WINDOW_MS = 10_000;
/** Coming back after this long in the background counts as a fresh launch. */
const LONG_BREAK_MS = 15 * 60_000;

/**
 * Keeps the installed PWA on the latest release. The service worker starts the app from its cache and
 * downloads a new release in the background; this switches to it only when the shopper can't lose
 * anything — right after a launch, or on return from a long break. Otherwise the downloaded release
 * simply starts on the next launch (never a reload mid-checkout).
 */
@Injectable({ providedIn: 'root' })
export class AppUpdateService {
  private readonly updates = inject(SwUpdate);
  private launchedAt = Date.now();
  private hiddenAt = 0;
  private updateReady = false;

  start(): void {
    if (!this.updates.isEnabled) {
      return;
    }

    this.updates.versionUpdates.pipe(filter((e) => e.type === 'VERSION_READY')).subscribe(() => {
      this.updateReady = true;
      if (Date.now() - this.launchedAt < LAUNCH_WINDOW_MS) {
        this.applyUpdate();
      }
    });

    // The cached release references files the server no longer has — only a reload recovers.
    this.updates.unrecoverable.subscribe(() => location.reload());

    // iOS resumes a home-screen app from memory instead of relaunching it (no navigation, so the
    // service worker never looks for a new release): check on every return to the foreground.
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.hiddenAt = Date.now();
        return;
      }
      if (this.hiddenAt && Date.now() - this.hiddenAt > LONG_BREAK_MS) {
        this.launchedAt = Date.now();
        if (this.updateReady) {
          this.applyUpdate();
          return;
        }
      }
      this.updates.checkForUpdate().catch(() => undefined);
    });
  }

  private applyUpdate(): void {
    this.updateReady = false;
    this.updates.activateUpdate().then(
      () => location.reload(),
      () => location.reload()
    );
  }
}
