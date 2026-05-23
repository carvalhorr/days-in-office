# Play Store listing — Days in Office

Source of truth for the title, descriptions, and asset checklist. Copy/paste
straight into Play Console → Main store listing.

## Title (max 30 chars)

```
Days in Office
```

(14 chars — well under limit.)

## Short description (max 80 chars)

```
Track your in-office days and stay on top of your hybrid-work mandate.
```

(69 chars.)

## Full description (max 4000 chars)

```
Days in Office helps you keep up with your company's in-office mandate
without having to remember whether you went in last Tuesday.

The app tracks which days you spent at the office, calculates your
compliance against a target you set (default: 50% of working days), and
shows you exactly how many more office days you need to hit your target
this week, month, quarter, or rolling four-week window.

EVERYTHING STAYS ON YOUR DEVICE
There is no account, no server, and no cloud sync. Your office days are
stored locally in an encrypted database. Nothing leaves your phone.

HOW IT TRACKS YOU
Pick the detection methods that suit how you work. You can combine any of:

  • Manual check-in — the simplest option, a single tap from the home
    screen widget or the dashboard.
  • Geofencing — silently logs an office day when your phone enters a
    location you've marked as "the office". Requires location permission.
  • Wi-Fi connected — when your phone joins a Wi-Fi network you've
    marked as office Wi-Fi, the app proposes today as an office day.
  • Wi-Fi scan — for users who don't connect to corporate Wi-Fi: the
    app spots known office SSIDs in the background.

For every automatic detection the app sends you a notification asking
"Were you at the office today?" — your tap is what records the day.
The app never records an office day without your explicit yes.

PTO AND HOLIDAYS
Mark a day as PTO or a public holiday from the calendar view, and
the day stops counting toward your mandate target.

MANDATE TARGET, YOUR WAY
  • Choose your target percentage (50% is the default).
  • Choose the period that matches your company's policy:
    weekly, monthly, quarterly, or rolling 4 weeks.
  • Mark which days are working days (Mon–Fri by default).

The dashboard shows your compliance for the current period, how many
office days you've logged, how many days remain, and how many of
those you still need to be in to hit your target.

PERMISSIONS
  • Location (foreground and background) — only used to detect when
    you arrive at an office geofence, and only if you enable
    geofencing. Disable it and the permission is never requested.
  • Wi-Fi state — only used to read the SSID of your current network
    or scan for nearby networks, and only if you enable Wi-Fi
    detection.
  • Notifications — used to ask you to confirm a detected office day.

You can change or revoke any of these from system settings; the rest
of the app keeps working without them.

WHAT YOU GET
  • Home-screen widget for one-tap "I'm in the office today".
  • Daily reminder if you haven't logged a working day.
  • Calendar view of the current month with one tile per day.
  • Configurable working week, mandate target, and detection methods.

Days in Office is free, ad-free, and contains no analytics or tracking.

Known issue at 1.0: Wi-Fi SSID detection on some Pixel devices reads
"<unknown ssid>" until you toggle the network manually. Geofence
detection and manual check-in are unaffected. A fix is tracked for
the next patch.
```

(2 998 chars — within limit.)

## Launcher icon audit

Current state of `app/src/main/res/`:

| Resource | What's there |
|---|---|
| `drawable/ic_launcher_background.xml` | Solid `#1565C0` (Material blue 800) shape |
| `drawable/ic_launcher_foreground.xml` | A single white circle (54 dp radius, centered) |
| `mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon = background + foreground |
| `mipmap-anydpi-v26/ic_launcher_round.xml` | Same |

**Verdict: placeholder.** The shipped icon is two-tone and visually empty.
Before going to production:

1. Replace `ic_launcher_foreground.xml` with a real glyph. A calendar tile
   with a checkmark or a stylised office-building silhouette both fit the
   theme.
2. Generate density-bucket fallbacks (`mipmap-mdpi/ic_launcher.png` etc.) via
   Android Studio → Image Asset Studio. With `minSdk = 28` the
   adaptive-icon is sufficient for the runtime, but Play Console wants the
   high-res 512×512 PNG.

I am leaving the placeholder in place rather than ship a half-baked icon.
Treat this as the one **must-do** item before publishing.

## Store-listing assets checklist

All assets go through Play Console → Main store listing. None of these
ship with the APK; they live in Play Console only.

| Asset | Spec | Source / TODO |
|---|---|---|
| App icon (hi-res) | 512 × 512 PNG, 32-bit, max 1 MB | Generate from `ic_launcher.xml` once the foreground glyph is replaced. Use Android Studio → Image Asset Studio → export 512×512. |
| Feature graphic | 1024 × 500 PNG/JPG | Need to design. Suggest: brand-blue background, app icon at left, "Days in Office" wordmark, sub-line "Track your hybrid-work mandate." Tools: Figma / Canva. |
| Phone screenshots (min 2, max 8) | 16:9 or 9:16, between 320 px and 3840 px on each edge | Take from the actual app after a few days of usage. Cover: dashboard with compliance ring, calendar view, settings, onboarding step 1. **Requires a connected device or emulator** — automated this pass can't produce them. |
| 7-inch tablet screenshots | optional, ≥1024 px | Optional — skip for v1. |
| 10-inch tablet screenshots | optional, ≥1024 px | Optional — skip for v1. |
| Video (promo) | 30 s – 2 min, YouTube URL | Optional. Skip for v1. |

### Screenshot script (when a device is available)

Use these flows to take the four required screenshots:

1. **Dashboard with compliance ring.** Open the app on a date where the user
   has 3–4 office days logged and a 50% target → captures the ring,
   "X days in office", "Y more needed".
2. **Calendar view.** Tap the calendar tab. Picks a month with a mix of
   office / remote / PTO days.
3. **Settings.** Settings tab → captures the detection rows (Wi-Fi
   connected, Wi-Fi scan, geofence, manual) and the mandate target row.
4. **Onboarding step 1.** Long-press app → clear data → relaunch →
   captures the "Set your mandate" first step.

Frame each on a Pixel 6 (or emulator @ 1080 × 2340). No status-bar
clean-up needed for the Play Store — the screenshot bar itself is fine.

## Workflow for filling Play Console

1. Replace the launcher-icon foreground glyph, regenerate the 512×512.
2. Take the four phone screenshots from a running build.
3. Have the feature graphic designed.
4. Open Play Console → Main store listing.
5. Paste title, short description, full description from the blocks above.
6. Upload icon, feature graphic, screenshots.
7. Save. The listing is not visible until the first release is published.
