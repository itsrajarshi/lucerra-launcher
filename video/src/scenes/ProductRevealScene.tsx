import React from 'react';
import {AbsoluteFill, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {GeometricAssembly} from '../components/GeometricAssembly';
import {KineticText} from '../components/KineticText';
import {LightSweep} from '../components/LightSweep';
import {ProductFrame} from '../components/ProductFrame';
import {C} from '../styles/tokens';
import {inOut, mix, outExpo, p} from '../utils/easing';
import {RING} from './TransformationScene';

/**
 * Scene 3 (11-18 s, plus 20 frames under scene 4): the ring thickens into the frosted disc,
 * the blue core lands, light crosses it once. "MEET LUCERRA." The disc then travels up beside
 * the title while a TV showing the real home screen rises.
 */
export const ProductRevealScene: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();

  const u = p(frame, 10, 50);
  const rot = p(frame, 10, 60) * 90;
  const core = spring({frame: frame - 52, fps, config: {damping: 14, stiffness: 120, mass: 0.8}});
  const dolly = mix(1.12, 1, p(frame, 0, 130, outExpo));
  const tilt = mix(-12, 4, p(frame, 0, 130, outExpo));

  // Disc -> lockup beside the title
  const m = p(frame, 134, 40);
  const mt = p(frame, 122, 40);
  const cx = mix(RING.x, 663, m);
  const cy = mix(RING.y, 110, m);
  const ds = mix(1, 0.2, m);

  const tx = mix(960, 1014, mt);
  const ty = mix(800, 110, mt);
  const ts = mix(1, 0.46, mt);

  const rise = p(frame, 150, 55, outExpo);
  // The shift: stock Android home dips to dark, Lucerra home comes up.
  const shift = p(frame, 262, 36, inOut);
  const dip = 0.7 * Math.sin(Math.PI * p(frame, 258, 44, inOut));
  const settle = 1.035 - 0.035 * p(frame, 205, 95, inOut);
  const lockupFade = 1 - p(frame, 200, 16);

  return (
    <AbsoluteFill style={{background: C.bg}}>
      {/* soft contact shadow under the disc */}
      <div
        style={{
          position: 'absolute',
          left: RING.x - 150,
          top: RING.y + 205,
          width: 300,
          height: 40,
          borderRadius: '50%',
          background: 'rgba(0,0,0,0.7)',
          filter: 'blur(24px)',
          opacity: u * 0.8 * (1 - m),
        }}
      />

      <AbsoluteFill style={{opacity: lockupFade}}>
        <div style={{position: 'absolute', left: cx - 200, top: cy - 200, transform: `scale(${ds})`, perspective: 1600}}>
          <div style={{transform: `scale(${dolly}) rotateY(${tilt * (1 - m)}deg)`, position: 'relative'}}>
            <GeometricAssembly u={u} rot={rot} core={core} />
            <div style={{position: 'absolute', left: 35, top: 35}}>
              <LightSweep frame={frame} start={66} dur={44} size={330} />
            </div>
          </div>
        </div>

        <div style={{position: 'absolute', left: tx, top: ty, transform: `translate(-50%, -50%) scale(${ts})`}}>
          <KineticText text="MEET LUCERRA." frame={frame} start={78} size={120} />
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{perspective: 2200, opacity: rise, transform: `scale(${settle})`, transformOrigin: '960px 616px'}}>
        <div style={{position: 'absolute', inset: 0, transform: `translateY(${(1 - rise) * 220}px) rotateX(${(1 - rise) * 18}deg) scale(${0.92 + 0.08 * rise})`, transformOrigin: '50% 100%'}}>
          <ProductFrame
            screens={[
              {src: 'shots/stock.jpg', opacity: 1 - shift, scale: 1 + 0.025 * p(frame, 205, 90)},
              {src: 'shots/home_f0.jpg', opacity: shift},
            ]}
            overlay={<div style={{position: 'absolute', inset: 0, background: C.bg, opacity: dip}} />}
          />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
