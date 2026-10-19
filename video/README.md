# Lucerra film: "A Better Way to Launch"

14 s, 1920x1080, 30 fps Remotion film. Real TV captures only.

## Install
```bash
npm install
```

## Preview
```bash
npm run studio
```

## Render
```bash
npx remotion render src/index.ts LucerraFilm out/lucerra-film.mp4 --crf=18
```
If Remotion cannot download its own Chromium, add
`--browser-executable="C:/Program Files/Google/Chrome/Application/chrome.exe"`.
Single frame: `npx remotion still src/index.ts LucerraFilm out.png --frame=600`.
Regenerate audio: `python tools/make_audio.py`.

## Scenes (14.3 s, 429 frames)
| # | Scene | Time | Frames | File |
|---|---|---|---|---|
| 1 | Stock Android home, "TOO MUCH NOISE.", shift to Lucerra, "LESS FRICTION. MORE FOCUS." | 0-3 s | 0-90 | `StockScene` |
| 2 | Real-time navigation: dock focus, Up to All Apps, Home, Now Playing | 3-10.7 s | 90-321 | `ExperienceScene` |
| 3 | Final frame: wordmark, tagline, URL | 10.7-14.3 s | 321-429 | `EndCardScene` |

Timings live in `src/utils/timing.ts`. Scene 2 plays `public/shots/nav.mp4`, a real screen recording of the TV
(`adb shell screenrecord`) while keys were sent with `adb shell input keyevent`. Key moments are listed in
`PRESSES` in `ExperienceScene.tsx` so the remote overlay matches. `nav_start.jpg` / `nav_end.jpg` are its first and last used frames.
`OpeningScene`, `TransformationScene` and `ProductRevealScene` are an unused longer opening, kept for reference.

## Replacing assets
See `public/ASSETS.md`. Screenshots are 1920x1080 and swap in place under `public/shots/`.
