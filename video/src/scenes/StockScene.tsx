import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {KineticText} from '../components/KineticText';
import {FRAME_CY, ProductFrame} from '../components/ProductFrame';
import {C} from '../styles/tokens';
import {inOut, outExpo, p} from '../utils/easing';

/** Scene 1 (0-3.2 s): the stock Android TV home, "TOO MUCH NOISE.", then it settles into Lucerra's home. */
export const StockScene: React.FC = () => {
  const frame = useCurrentFrame();
  const inn = p(frame, 0, 16, outExpo);
  const shift = p(frame, 50, 34, inOut);
  const dip = 0.7 * Math.sin(Math.PI * p(frame, 46, 42, inOut));
  const settle = 1.035 - 0.035 * p(frame, 0, 96, inOut);
  const textOut = 1 - p(frame, 84, 10);
  return (
    <AbsoluteFill style={{background: C.bg}}>
      <AbsoluteFill style={{opacity: inn, transform: `scale(${settle})`, transformOrigin: `960px ${FRAME_CY}px`}}>
        <ProductFrame
          screens={[
            {src: 'shots/stock.jpg', opacity: 1 - shift},
            {src: 'shots/home_f0.jpg', opacity: shift},
          ]}
          overlay={<div style={{position: 'absolute', inset: 0, background: C.bg, opacity: dip}} />}
        />
      </AbsoluteFill>
      <div style={{position: 'absolute', left: 0, right: 0, top: 96, display: 'flex', justifyContent: 'center', gap: 28, opacity: textOut}}>
        <KineticText text="TOO MUCH NOISE." frame={frame} start={8} exitAt={46} size={84} stagger={1.1} />
        <div style={{position: 'absolute', left: 0, right: 0, top: 0, display: 'flex', justifyContent: 'center'}}>
          <KineticText text="LESS FRICTION. MORE FOCUS." frame={frame} start={54} size={84} stagger={1.0} />
        </div>
      </div>
    </AbsoluteFill>
  );
};
