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

## Scenes (14.1 s, 422 frames)
| # | Scene | Time | Frames | File |
|---|---|---|---|---|
| 1 | Stock Android home, "TOO MUCH NOISE.", shift to Lucerra, "LESS FRICTION. MORE FOCUS." | 0-3.2 s | 0-96 | `StockScene` |
| 2 | UI tour: focus, hold-OK menu, All Apps, remote button, Now Playing | 3.2-10.1 s | 96-304 | `ExperienceScene` |
| 3 | Final frame: wordmark, tagline, URL | 10.1-14.1 s | 304-422 | `EndCardScene` |

Timings live in `src/utils/timing.ts`. Pace of the tour: `SLOW` in `ExperienceScene.tsx`.
`OpeningScene`, `TransformationScene` and `ProductRevealScene` are an unused longer opening, kept for reference.

## Replacing assets
See `public/ASSETS.md`. Screenshots are 1920x1080 and swap in place under `public/shots/`.
