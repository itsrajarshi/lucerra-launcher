import React, {useEffect, useState} from 'react';
import {AbsoluteFill, Audio, continueRender, delayRender, interpolate, Sequence, staticFile} from 'remotion';
import {EndCardScene} from '../scenes/EndCardScene';
import {ExperienceScene} from '../scenes/ExperienceScene';
import {StockScene} from '../scenes/StockScene';
import '../styles/global.css';
import {C} from '../styles/tokens';
import {SCENES, TOTAL} from '../utils/timing';

/** Waits for the bundled Outfit weights so no frame renders with a fallback font. */
const useFonts = () => {
  const [handle] = useState(() => delayRender('fonts'));
  useEffect(() => {
    Promise.all([300, 400, 500, 600].map((w) => document.fonts.load(`${w} 40px Outfit`)))
      .catch(() => undefined)
      .then(() => continueRender(handle));
  }, [handle]);
};

export const LucerraFilm: React.FC = () => {
  useFonts();
  const S = SCENES;
  // Master fade-out over the last second of the mix.
  const master = (f: number) => interpolate(f, [TOTAL - 30, TOTAL], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill style={{background: C.bg}}>
      <Sequence from={S.stock.from} durationInFrames={S.stock.dur} name="1 Stock to Lucerra">
        <StockScene />
      </Sequence>
      <Sequence from={S.experience.from} durationInFrames={S.experience.dur} name="2 Experience">
        <ExperienceScene />
      </Sequence>
      <Sequence from={S.end.from} durationInFrames={S.end.dur} name="3 Final frame">
        <EndCardScene />
      </Sequence>

      <Audio src={staticFile('audio/bed.wav')} volume={(f) => 0.9 * master(f)} />
      <Audio src={staticFile('audio/fx.wav')} volume={(f) => 0.85 * master(f)} />
    </AbsoluteFill>
  );
};
