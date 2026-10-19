import React from 'react';
import {Img, staticFile} from 'remotion';
import {C} from '../styles/tokens';

export const REMOTE_AR = 1373 / 352; // height / width of public/device/remote.png

/** Button centres as fractions of remote.png (352x1373). */
const BTN = {
  up: {x: 176 / 352, y: 286 / 1373},
  ok: {x: 176 / 352, y: 380 / 1373},
  apps: {x: 75.5 / 352, y: 579 / 1373},
};

export type Press = {button: keyof typeof BTN; /** 0..1 ripple progress */ t: number; hold?: boolean};

type Props = {x: number; y: number; w: number; opacity?: number; press?: Press | null};

/** Real Mi remote cut-out with an accent ring marking the pressed button. */
export const Remote: React.FC<Props> = ({x, y, w, opacity = 1, press}) => {
  const h = w * REMOTE_AR;
  const ring = w * 0.23;
  return (
    <div style={{position: 'absolute', left: x, top: y, width: w, height: h, opacity, filter: 'drop-shadow(0 40px 50px rgba(0,0,0,0.6))'}}>
      <Img src={staticFile('device/remote.png')} style={{width: w, height: h, display: 'block'}} />
      {press && press.t > 0 && press.t < 1.001 ? (
        press.hold ? (
          <svg
            width={ring}
            height={ring}
            viewBox="0 0 100 100"
            style={{position: 'absolute', left: BTN[press.button].x * w - ring / 2, top: BTN[press.button].y * h - ring / 2}}
          >
            <circle cx="50" cy="50" r="44" fill="none" stroke="rgba(255,255,255,0.2)" strokeWidth="6" />
            <circle
              cx="50"
              cy="50"
              r="44"
              fill="none"
              stroke={C.accent}
              strokeWidth="6"
              strokeLinecap="round"
              pathLength={100}
              strokeDasharray="100"
              strokeDashoffset={100 * (1 - Math.min(1, press.t))}
              transform="rotate(-90 50 50)"
            />
          </svg>
        ) : (
          <div
            style={{
              position: 'absolute',
              left: BTN[press.button].x * w - ring / 2,
              top: BTN[press.button].y * h - ring / 2,
              width: ring,
              height: ring,
              borderRadius: '50%',
              border: `3px solid ${C.accent}`,
              opacity: 1 - press.t,
              transform: `scale(${0.75 + press.t * 0.9})`,
            }}
          />
        )
      ) : null}
    </div>
  );
};
