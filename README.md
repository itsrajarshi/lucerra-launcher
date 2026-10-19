# Glass Home

A fast, ad-free Android TV launcher with an Apple TV-style layout and liquid-glass touches. Built for
the Mi Box 4K (Android 9), where real-time blur is too slow, so the glass is pre-blurred once per
wallpaper and sampled by the dock.

- No network permission, no background services, no ads.
- Procedurally generated wallpapers (nothing to license), crossfading every 10 minutes.
- Spring-driven focus motion that can be interrupted mid-flight.
- "Effects: Reduced" toggle for opaque panels and no motion (also auto-on if system animations are off).

## Build

Needs JDK 17 and the Android SDK (platform 34). Create `local.properties` with
`sdk.dir=C:/path/to/Android/Sdk`, then:

```
./gradlew assembleDebug
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Edit `app/src/main/res/values/config.xml` to change the Favorites row or hide packages.

## License

Proprietary, all rights reserved. See [LICENSE](LICENSE).

## Wallpapers
Six scenery photos in `app/src/main/assets/wallpapers/` (credits in `WALLPAPER-CREDITS.md`). Drop more 1920x1080 JPEGs there; they rotate every 10 minutes.

## Make it the default Home (Mi Box 4K)
Stock Google TV home wins the Home key, so disable it (reversible):

```
powershell -File tools/debloat.ps1     # disables stock home, recommendations, Xiaomi analytics, MiTV channel/plus, Play Games
powershell -File tools/restore.ps1     # puts everything back
```
Pass `-Device <ip>:5555` if your box differs from 192.168.0.103. Measured on the box: about 67 MB PSS, ~1% janky frames while scrolling.

## All Apps, pinning and reordering

- Home shows exactly five favourite apps. Press **Up** from the shelf to open All Apps: your favourites in a strip on top, every other app below.
- **Hold OK** on an app in the grid to pin it to Home (five at most).
- **Hold OK** on a favourite (on Home or in the strip) for a menu: Move left, Move right, Remove from Home.
- Back closes All Apps. The order is stored in app preferences, so it survives reboots.
- The round Assistant bubble beside the clock opens Google Assistant.
- Put a square `app/src/main/assets/profile.jpg` in the project to show your photo in the clock pill (otherwise the initial from `profile_initial` in `config.xml`).
- Press **Up** again from the favourites strip to reach the Assistant bubble and the clock pill. The pill opens a dropdown: Wi-Fi, Settings, Power.
- Hold OK on any app in the grid for Add to Home or Uninstall.
- The remote's apps button (key 284) is claimed by the system and sent to TV Settings. To make it open All Apps, enable the Glass Home accessibility service (Settings > Accessibility, or `adb shell settings put secure enabled_accessibility_services dev.rajarshi.glasshome/dev.rajarshi.glasshome.KeyService` plus `accessibility_enabled 1`). The same service opens the Power dialog.

## Now playing

While Spotify (or any app with a media session) is playing, a glass widget appears in the top bar with the title, artist, album art and previous / play-pause / next. After a pause it stays 20 seconds, then fades away.

Android only shows other apps' media sessions to an app with notification access. Glass Home's listener class contains no code, so it reads no notifications. Grant it once:

```bash
adb shell cmd notification allow_listener dev.rajarshi.glasshome/dev.rajarshi.glasshome.NowPlayingListener
```

Use `cmd notification allow_listener` rather than writing the secure setting directly: the direct write overwrites the other allowed listeners and was not picked up by the system.
