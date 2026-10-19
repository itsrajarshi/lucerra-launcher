import React, {useEffect, useState} from 'react';
import {AbsoluteFill, continueRender, delayRender, Img, staticFile} from 'remotion';
import '../styles/global.css';
import {C, F} from '../styles/tokens';

/** Reel cover, 9:16. Everything sits inside the centre 3:4 band so the profile-grid crop keeps it. */
export const ReelCover: React.FC = () => {
  const [handle] = useState(() => delayRender('fonts'));
  useEffect(() => {
    Promise.all([300, 500, 600].map((w) => document.fonts.load(`${w} 40px Outfit`)))
      .catch(() => undefined)
      .then(() => continueRender(handle));
  }, [handle]);
  return (
    <AbsoluteFill style={{background: `radial-gradient(ellipse at 50% 55%, #17214a 0%, ${C.bg} 70%)`, fontFamily: F.display, alignItems: 'center'}}>
      <div style={{position: 'absolute', top: 400, left: 0, right: 0, textAlign: 'center', color: C.ink, fontSize: 104, fontWeight: 600, lineHeight: 1.05, letterSpacing: '-0.02em'}}>
        Your TV,
        <br />
        <span style={{color: C.accent}}>without the noise.</span>
      </div>
      <div style={{position: 'absolute', top: 800, left: 80, width: 920, borderRadius: 30, overflow: 'hidden', border: '12px solid #0b0e18', boxShadow: '0 60px 120px -30px rgba(108,140,255,0.45), 0 0 0 1px rgba(255,255,255,0.1)'}}>
        <Img src={staticFile('shots/nav_end.jpg')} style={{display: 'block', width: '100%'}} />
      </div>
      <div style={{position: 'absolute', top: 1400, left: 0, right: 0, textAlign: 'center', color: C.ink, fontSize: 44, fontWeight: 500, letterSpacing: '0.32em'}}>LUCERRA</div>
      <div style={{position: 'absolute', top: 1480, left: 0, right: 0, textAlign: 'center', color: C.dim, fontSize: 34, fontWeight: 300, letterSpacing: '0.02em'}}>Free and open source. No ads.</div>
    </AbsoluteFill>
  );
};
