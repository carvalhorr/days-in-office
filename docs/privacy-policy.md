# Privacy Policy — Days in Office

**Effective date:** 2026-05-22
**App package:** `com.carvalhorr.daysInOffice`
**Developer:** Rodrigo Carvalho — contact at <carvalhorr@gmail.com>

## Summary

Days in Office is a single-user productivity app. **All data the app processes
stays on your device.** There is no account, no cloud sync, no analytics,
no advertising SDK, and no third-party data sharing.

This policy explains what data the app accesses, why, and what controls you
have.

## Data the app accesses

### Location (foreground and background)

* **Permissions:** `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`,
  `ACCESS_BACKGROUND_LOCATION`
* **Why:** to detect when you arrive at the office using geofencing. The
  app stores the latitude/longitude/radius of office locations you
  configure, and the timestamp of geofence entries that you confirm.
* **Background:** the geofence runs only when geofence detection is enabled.
  No continuous location stream is recorded. The app does not build a
  history of where you've been; it only knows "did you cross the office
  geofence today, and did you confirm?"
* **Opt-out:** turn off geofence detection in Settings → Detection. The
  permission is then unused; you can also revoke it in system settings.

### Wi-Fi state

* **Permissions:** `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`
* **Why:** to detect when your device is connected to (or sees) a Wi-Fi
  network you've marked as the office network. The app stores the
  SSID(s) you configure, and the timestamp of matches you confirm.
* **Opt-out:** turn off Wi-Fi detection in Settings → Detection.

### Calendar (read-only)

* **Permission:** `READ_CALENDAR`
* **Why:** to read PTO and public-holiday events so they can be excluded
  from your in-office mandate calculation. The app reads event titles
  and dates; it does not modify, share, or upload your calendar.
* **Opt-out:** turn off calendar sync in Settings → Calendar. The app
  will then assume Mon–Fri are working days unless you mark specific
  days as PTO manually.

### Notifications

* **Permission:** `POST_NOTIFICATIONS`
* **Why:** to ask you to confirm a detected office day, and (optionally)
  remind you at end of day if you haven't logged anything.
* **Opt-out:** Settings → Notifications, or revoke at system level.

### Boot completion

* **Permission:** `RECEIVE_BOOT_COMPLETED`
* **Why:** to re-register the daily detection worker after device restart.
  No data is read or sent at boot.

### Foreground service

* **Permission:** `FOREGROUND_SERVICE`
* **Why:** WorkManager uses foreground services to run long-running
  detection jobs reliably on Android 12+. No data is transmitted.

## Where data is stored

* **Office days, mandate config, detection config:** stored in the
  app's private Room/SQLite database (`app_database`) and DataStore
  preferences. Both are inside the app sandbox and not readable by
  other apps.
* **Office locations / Wi-Fi SSIDs you configure:** stored in the
  same database.

The database is included in Android's standard app backup so that
re-installs preserve your history. You can disable backup in Settings →
Privacy if you prefer.

## Data the app does NOT collect

* No personal identifiers (no name, email, phone, IMEI, advertising ID).
* No analytics — no Firebase, no Sentry, no Crashlytics, no telemetry of
  any kind.
* No ads. No ad SDK is bundled.
* No data is sent off-device. The app has no outbound network calls in
  the 1.x branch.

## Data you can export or delete

* **Export:** not yet available; planned for a future version.
* **Delete:** Settings → Privacy → "Delete all data" wipes the database
  and preferences. Uninstalling the app also removes everything
  (subject to the operating system's standard backup behaviour — to be
  thorough, disable backup before uninstall).

## Children's privacy

The app is targeted at working adults (18+). It does not knowingly
collect data from children.

## Changes to this policy

If the policy materially changes (e.g., a future version adds a server
component), this document will be updated and the effective date at
the top revised. The app will not start collecting new categories of
data without an in-app notice first.

## Contact

Questions, requests, or complaints:
**Rodrigo Carvalho — <carvalhorr@gmail.com>**

## Hosting note for Play Console

This document is also hosted at:

> `https://carvalhorr.github.io/days-in-office/privacy-policy.html`

Play Console requires a publicly accessible URL. The plan is to enable
GitHub Pages on the `days-in-office` repo's `gh-pages` (or `docs/`)
branch and publish the markdown rendered as HTML. The URL above is the
intended target; it does not yet resolve. **Step before submitting v1
to Play Console:**

1. In GitHub repo settings → Pages → set Source = `main` branch,
   `/docs` folder.
2. Confirm `https://carvalhorr.github.io/days-in-office/privacy-policy` resolves
   (GitHub Pages serves markdown as HTML automatically with the default
   theme).
3. Paste the URL into Play Console → App content → Privacy policy.
