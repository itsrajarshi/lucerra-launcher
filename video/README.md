# Lucerra film: "A Better Way to Launch"

35 s, 1920x1080, 30 fps Remotion film. Real TV captures only.

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

## Scenes
| # | Scene | Time | Frames | File |
|---|---|---|---|---|
| 1 | Problem: "TOO MUCH NOISE." | 0-6 s | 0-180 | `OpeningScene` |
| 2 | Turning point: "LESS FRICTION. MORE FOCUS." | 6-11 s | 180-330 | `TransformationScene` |
| 3 | Reveal: "MEET LUCERRA." | 11-18 s | 330-540 | `ProductRevealScene` |
| 4 | Experience: home, arrange, All Apps, remote | 18-28 s | 540-840 | `ExperienceScene` |
| 5 | Final brand frame and URL | 28-35 s | 840-1050 | `EndCardScene` |

Timings live in `src/utils/timing.ts`; captions in `BEATS` in `ExperienceScene.tsx`.

## Replacing assets
See `public/ASSETS.md`. Screenshots are 1920x1080 and swap in place under `public/shots/`.
