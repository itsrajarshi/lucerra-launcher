#!/usr/bin/env bash
# Renders review stills: tools/stills.sh 60 120 ...  (uses the installed Chrome, no download)
cd "$(dirname "$0")/.."
CH="C:/Program Files/Google/Chrome/Application/chrome.exe"
mkdir -p frames
printf '%s\n' "$@" | xargs -P 4 -I{} npx remotion still src/index.ts LucerraFilm frames/f{}.png --frame={} --browser-executable="$CH" --log=error
