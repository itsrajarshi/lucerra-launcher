import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, F} from '../styles/tokens';
import {LucerraFilm} from './LucerraFilm';

const SCALE = 1080 / 1920;
const FILM_H = 1080 * SCALE;

/** 9:16 version for Reels / Shorts: the 16:9 film scaled to full width, centred, with a line above and below. */
export const LucerraReel: React.FC = () => (
  <AbsoluteFill style={{background: `linear-gradient(180deg, ${C.bg} 0%, #0b1022 50%, ${C.bg} 100%)`, fontFamily: F.display}}>
    <div style={{position: 'absolute', left: 0, right: 0, top: 500, textAlign: 'center', color: C.ink, fontSize: 56, fontWeight: 500, letterSpacing: '0.01em'}}>
      A calm home for Android TV
    </div>
    <div style={{position: 'absolute', left: 0, top: (1920 - FILM_H) / 2, width: 1920, height: 1080, transform: `scale(${SCALE})`, transformOrigin: '0 0'}}>
      <LucerraFilm />
    </div>
    <div style={{position: 'absolute', left: 0, right: 0, top: 1290, textAlign: 'center', color: C.dim, fontSize: 38, fontWeight: 300, letterSpacing: '0.02em'}}>
      Free and open source
    </div>
  </AbsoluteFill>
);
