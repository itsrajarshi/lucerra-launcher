import React from 'react';
import {p} from '../utils/easing';

type Props = {frame: number; start: number; dur: number; size: number; round?: boolean};

/** One soft diagonal light band that crosses its (clipped) parent once. */
export const LightSweep: React.FC<Props> = ({frame, start, dur, size, round = true}) => {
  const t = p(frame, start, dur);
  if (t <= 0 || t >= 1) return null;
  return (
    <div
      style={{
        position: 'absolute',
        width: size,
        height: size,
        borderRadius: round ? '50%' : 0,
        overflow: 'hidden',
        pointerEvents: 'none',
        mixBlendMode: 'soft-light',
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: -size * 0.25,
          bottom: -size * 0.25,
          width: size * 0.45,
          left: -size * 0.45 + t * size * 1.45,
          transform: 'rotate(20deg)',
          background:
            'linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.95) 50%, rgba(255,255,255,0) 100%)',
        }}
      />
    </div>
  );
};
