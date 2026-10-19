import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {FragAt, FRAGS, fragPos, openingCam} from '../components/Field';
import {KineticText} from '../components/KineticText';
import {C} from '../styles/tokens';
import {outExpo, p} from '../utils/easing';

export const OPENING_END = 180;

/** Scene 1 (0-6 s): a drifting, accelerating field of clutter. "TOO MUCH NOISE." */
export const OpeningScene: React.FC = () => {
  const frame = useCurrentFrame();
  const c = openingCam(frame);
  const cam = `translate(${c.tx}px, ${c.ty}px) scale(${c.s}) rotate(${c.rot}deg)`;
  return (
    <AbsoluteFill style={{background: C.bg}}>
      <AbsoluteFill style={{transform: cam, transformOrigin: '960px 540px'}}>
        {FRAGS.map((f) => {
          const pos = fragPos(f, frame);
          const appear = p(frame, 6 + f.id * 2.2, 26, outExpo);
          return <FragAt key={f.id} f={f} x={pos.x} y={pos.y} rot={pos.r} opacity={appear} />;
        })}
      </AbsoluteFill>
      {/* Keeps the headline readable over the field */}
      <AbsoluteFill
        style={{
          background: 'radial-gradient(ellipse 1100px 250px at 50% 50%, rgba(5,7,13,0.92) 0%, rgba(5,7,13,0.78) 60%, rgba(5,7,13,0) 100%)',
        }}
      />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 40}}>
        <KineticText text="TOO" frame={frame} start={44} exitAt={150} size={136} />
        <KineticText text="MUCH" frame={frame} start={62} exitAt={150} size={136} />
        <KineticText text="NOISE." frame={frame} start={80} exitAt={150} size={136} />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
