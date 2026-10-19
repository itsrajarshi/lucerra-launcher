import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame} from 'remotion';
import {GeometricAssembly} from '../components/GeometricAssembly';
import {KineticText} from '../components/KineticText';
import {FRAME_CY, ProductFrame} from '../components/ProductFrame';
import {REMOTE_AR, Remote} from '../components/Remote';
import {C, F} from '../styles/tokens';
import {inOut, mix, outExpo, p} from '../utils/easing';
import {CAM_NP, REMOTE_START} from './ExperienceScene';
import {camTransform, lerpCam} from '../components/CameraMotion';

const FRAME_END = {cx: 1260, cy: 480, s: 0.6785};
const REMOTE_END = {x: 1740, y: 380, w: 150};
const BOX = {x: 880, y: 770, w: 230};

/**
 * Scene 5 (28-35 s): pull back from the All Apps shot to a composed final frame:
 * the TV on the real home screen, the box and remote, the wordmark and the destination.
 */
export const EndCardScene: React.FC = () => {
  const frame = useCurrentFrame();
  const m = p(frame, 0, 32, inOut);
  const cam = lerpCam(CAM_NP, {s: FRAME_END.s, fx: 960, fy: FRAME_CY, cx: FRAME_END.cx, cy: FRAME_END.cy}, m);

  const rem = {
    x: mix(REMOTE_START.x, REMOTE_END.x, m),
    y: mix(REMOTE_START.y, REMOTE_END.y, m),
    w: mix(REMOTE_START.w, REMOTE_END.w, m),
  };
  const box = p(frame, 28, 24, outExpo);
  const bg = p(frame, 0, 40);

  const ctaIn = p(frame, 52, 18, outExpo);

  return (
    <AbsoluteFill style={{background: C.bg}}>
      <AbsoluteFill style={{opacity: 0.22 * bg}}>
        <Img src={staticFile('wp/w2.jpg')} style={{width: 1920, height: 1080, filter: 'blur(50px)', transform: 'scale(1.2)'}} />
      </AbsoluteFill>
      <AbsoluteFill style={{background: 'linear-gradient(90deg, rgba(5,7,13,0.92) 0%, rgba(5,7,13,0.55) 55%, rgba(5,7,13,0.2) 100%)'}} />

      <AbsoluteFill style={{transform: camTransform(cam), transformOrigin: '0 0'}}>
        <ProductFrame
          screens={[{src: 'shots/nowplaying.png', opacity: 1}]}
        />
      </AbsoluteFill>

      <div style={{position: 'absolute', left: BOX.x, top: BOX.y + (1 - box) * 34, width: BOX.w, opacity: box, filter: 'drop-shadow(0 30px 34px rgba(0,0,0,0.6))'}}>
        <Img src={staticFile('device/box.png')} style={{width: BOX.w, display: 'block'}} />
      </div>
      <Remote x={rem.x} y={rem.y} w={rem.w} />

      {/* Wordmark column */}
      <div style={{position: 'absolute', left: 120, top: 236, opacity: p(frame, 18, 16), transform: `translateY(${(1 - p(frame, 18, 16, outExpo)) * 20}px)`}}>
        <GeometricAssembly u={1} rot={0} core={1} size={96} />
      </div>
      <div style={{position: 'absolute', left: 120, top: 372}}>
        <KineticText text="LUCERRA" frame={frame} start={20} size={112} tracking={0.14} />
      </div>
      <div style={{position: 'absolute', left: 124, top: 520}}>
        <KineticText text="A calm home for Android TV." frame={frame} start={34} size={40} weight={300} tracking={0.01} stagger={0.8} color={C.dim} rise={16} />
      </div>
      <div style={{position: 'absolute', left: 124, top: 620, opacity: ctaIn, transform: `translateY(${(1 - ctaIn) * 16}px)`, fontFamily: F.display}}>
        <div
          style={{
            display: 'inline-block',
            padding: '16px 30px',
            borderRadius: 999,
            background: C.accent,
            color: '#0a1020',
            fontSize: 28,
            fontWeight: 500,
            letterSpacing: '0.02em',
          }}
        >
          Free and open source
        </div>
        <div style={{marginTop: 28, fontSize: 30, fontWeight: 400, color: C.ink, letterSpacing: '0.01em'}}>github.com/itsrajarshi/lucerra-launcher</div>
        <div style={{marginTop: 14, fontSize: 24, fontWeight: 300, color: C.dim}}>GPL-3.0 &nbsp;·&nbsp; No ads &nbsp;·&nbsp; No network permission</div>
      </div>
    </AbsoluteFill>
  );
};

export {REMOTE_AR};
