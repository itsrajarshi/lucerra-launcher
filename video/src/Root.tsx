import React from 'react';
import {Composition} from 'remotion';
import {LucerraFilm} from './compositions/LucerraFilm';
import {LucerraReel} from './compositions/LucerraReel';
import {FPS, H, TOTAL, W} from './utils/timing';

export const Root: React.FC = () => (
  <>
    <Composition id="LucerraFilm" component={LucerraFilm} durationInFrames={TOTAL} fps={FPS} width={W} height={H} />
    <Composition id="LucerraReel" component={LucerraReel} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
  </>
);
