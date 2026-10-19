import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {camPoint, FragAt, FRAGS, fragPos, N_FRAGS, openingCam} from '../components/Field';
import {GeometricAssembly} from '../components/GeometricAssembly';
import {KineticText} from '../components/KineticText';
import {C} from '../styles/tokens';
import {inCubic, inOut, mix, p} from '../utils/easing';
import {OPENING_END} from './OpeningScene';

export const RING = {x: 960, y: 470};
const R_OUTER = 330;
const R_RING = 165;

/**
 * Scene 2 (6-11 s): motion freezes, then the clutter is pulled onto a circle, collapses
 * to a single ring. "LESS FRICTION." / "MORE FOCUS."
 */
export const TransformationScene: React.FC = () => {
  const frame = useCurrentFrame();
  // Scene 1's final camera, held still: the freeze.
  const t0 = OPENING_END;
  const c0 = openingCam(t0);

  return (
    <AbsoluteFill style={{background: C.bg}}>
      <AbsoluteFill>
        {FRAGS.map((f) => {
          const raw = fragPos(f, t0);
          const pt = camPoint(raw.x, raw.y, t0);
          const frozen = {x: pt.x, y: pt.y, r: raw.r + c0.rot};
          const start = 16 + (f.id / N_FRAGS) * 22;
          const pull = p(frame, start, 44, inOut);
          const collapse = p(frame, 66, 34, inCubic);
          const theta = (f.id / N_FRAGS) * Math.PI * 2 - Math.PI / 2;
          const rad = mix(R_OUTER, R_RING, collapse);
          const tx = RING.x + Math.cos(theta) * rad;
          const ty = RING.y + Math.sin(theta) * rad;
          const x = mix(frozen.x, tx, pull);
          const y = mix(frozen.y, ty, pull);
          const rot = mix(frozen.r, (theta * 180) / Math.PI + 90, pull);
          const scale = mix(c0.s, 0.3, pull) * mix(1, 0.15, collapse);
          const fade = 1 - p(frame, 84, 16);
          return <FragAt key={f.id} f={f} x={x} y={y} rot={rot} scale={scale} opacity={fade} />;
        })}
      </AbsoluteFill>

      {/* The ring the clutter collapses into */}
      <div style={{position: 'absolute', left: RING.x - 200, top: RING.y - 200}}>
        <GeometricAssembly u={0} rot={-90 + p(frame, 86, 40) * 90} core={0} draw={p(frame, 84, 36)} opacity={p(frame, 84, 8)} />
      </div>

      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'flex-start', paddingTop: 868}}>
        <div style={{position: 'absolute'}}>
          <KineticText text="LESS FRICTION." frame={frame} start={26} exitAt={72} size={112} />
        </div>
        <div style={{position: 'absolute'}}>
          <KineticText text="MORE FOCUS." frame={frame} start={82} size={112} />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
