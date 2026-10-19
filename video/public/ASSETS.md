# Asset sources

| Asset | Where | Source / licence |
|---|---|---|
| `shots/nav.mp4`, `nav_*.jpg` | Real-time navigation recording | Screen-recorded from the author's Mi Box 4K over adb. |
| `shots/*.jpg` | TV screenshots (home, menu, All Apps) | Captured by the author over adb from Lucerra running on their Mi Box 4K. |
| `wp/w2.jpg`, `wp/w5.jpg` | Wallpapers | NASA imagery shipped in the launcher (public domain). |
| `tiles/*.png` | App tile art | As displayed on the author's TV. Logos belong to their owners (Apple, YouTube, Netflix, Amazon, JioHotstar, etc.) and are shown only to depict the real home screen. |
| `device/*.png` | Mi Box S/4K and remote | Product photo supplied by the author, background removed. Xiaomi marks belong to Xiaomi. |
| Outfit font | `@fontsource/outfit` | SIL Open Font Licence 1.1. |
| `audio/bed.wav`, `audio/fx.wav` | Music and sound effects | Original. Synthesised by `tools/make_audio.py` (numpy/scipy), no samples. |

To replace audio: drop 35 s, 44.1 kHz stereo WAVs with the same names into `public/audio/`.
- Now Playing capture shows a real track title as played on the author's TV.
