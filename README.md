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

MIT. See [LICENSE](LICENSE).

## Wallpapers
Six scenery photos in `app/src/main/assets/wallpapers/` (credits in `WALLPAPER-CREDITS.md`). Drop more 1920x1080 JPEGs there; they rotate every 10 minutes.

## Make it the default Home (Mi Box 4K)
Stock Google TV home wins the Home key, so disable it (reversible):

```
powershell -File tools/debloat.ps1     # disables stock home, recommendations, Xiaomi analytics, MiTV channel/plus, Play Games
powershell -File tools/restore.ps1     # puts everything back
```
Pass `-Device <ip>:5555` if your box differs from 192.168.0.103. Measured on the box: about 67 MB PSS, ~1% janky frames while scrolling.
