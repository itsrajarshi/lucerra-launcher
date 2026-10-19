import React from 'react';
import {Composition} from 'remotion';
import {LucerraFilm} from './compositions/LucerraFilm';
import {FPS, H, TOTAL, W} from './utils/timing';

export const Root: React.FC = () => (
  <Composition id="LucerraFilm" component={LucerraFilm} durationInFrames={TOTAL} fps={FPS} width={W} height={H} />
);
