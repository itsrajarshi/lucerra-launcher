import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame} from 'remotion';
import {Cam, CameraMotion, lerpCam} from '../components/CameraMotion';
import {FRAME_CY, ProductFrame, Screen} from '../components/ProductFrame';
import {Press, Remote} from '../components/Remote';
import {C, F, SAFE} from '../styles/tokens';
import {inOut, outExpo, p, linear} from '../utils/easing';

export const REMOTE_START = {x: 1640, y: 120, w: 218};

const CAM_ID: Cam = {s: 1, fx: 960, fy: FRAME_CY, cx: 960, cy: FRAME_CY};
const CAM_DOCK: Cam = {s: 1.6, fx: 960, fy: FRAME_CY + 240, cx: 960, cy: 810};
const CAM_MENU: Cam = {s: 1.35, fx: 960, fy: FRAME_CY, cx: 800, cy: FRAME_CY - 19};
// Push to the Now Playing widget (screenshot point 1150,95), kept clear of the remote on the right.
export const CAM_NP: Cam = {s: 1.9, fx: 1087, fy: FRAME_CY - 297, cx: 640, cy: 400};
const CAM_KEYS: [number, Cam][] = [
  [0, CAM_ID],
  [8, CAM_ID],
  [44, CAM_DOCK],
  [62, CAM_DOCK],
  [92, CAM_MENU],
  [150, CAM_MENU],
  [176, CAM_ID],
  [292, CAM_ID],
  [318, CAM_NP],
  [340, CAM_NP],
];

const camAt = (f: number): Cam => {
  for (let i = 0; i < CAM_KEYS.length - 1; i++) {
    const [a, ca] = CAM_KEYS[i];
    const [b, cb] = CAM_KEYS[i + 1];
    if (f <= b) return lerpCam(ca, cb, inOut(Math.min(1, Math.max(0, (f - a) / (b - a)))));
  }
  return CAM_NP;
};

// Which real screenshot has the dock focus on which app (Apple TV, YouTube, Netflix = 0,1,2).
const FOCUS: {from: number; img: number}[] = [
  {from: 0, img: 0},
  {from: 28, img: 1},
  {from: 40, img: 2},
  {from: 52, img: 1},
  {from: 62, img: 0},
];

type Beat = {from: number; to: number; n: string; label: string; line: string; sub?: string};
const BEATS: Beat[] = [
  {from: 16, to: 72, n: '01', label: 'HOME', line: 'Five apps. Nothing else.'},
  {from: 80, to: 150, n: '02', label: 'ARRANGE', line: 'Hold OK to arrange.', sub: 'Move left or right. Remove. Uninstall.'},
  {from: 168, to: 224, n: '03', label: 'ALL APPS', line: 'Press Up for every app.'},
  {from: 234, to: 292, n: '04', label: 'REMOTE', line: 'One button to All Apps.', sub: 'The remote’s apps button opens it directly.'},
];

const Caption: React.FC<{b: Beat; frame: number}> = ({b, frame}) => {
  const inn = p(frame, b.from, 16, outExpo);
  const out = p(frame, b.to - 8, 8, linear);
  const o = inn * (1 - out);
  if (o <= 0) return null;
  return (
    <div style={{position: 'absolute', left: SAFE, top: 78, opacity: o, transform: `translateY(${(1 - inn) * 22}px)`, fontFamily: F.display}}>
      <div style={{fontSize: 24, letterSpacing: '0.32em', color: C.accent, fontWeight: 500}}>
        {b.n} / {b.label}
      </div>
      <div style={{fontSize: 62, fontWeight: 500, color: C.ink, marginTop: 10, letterSpacing: '-0.01em'}}>{b.line}</div>
      {b.sub ? <div style={{fontSize: 28, fontWeight: 300, color: C.dim, marginTop: 6}}>{b.sub}</div> : null}
    </div>
  );
};

/**
 * Scene 4 (18-28 s): four real interactions on the real TV screenshots, chained by camera moves.
 * Home focus -> hold OK menu -> Up to All Apps -> remote apps button.
 */
// Everything below is authored at 30 fps pace; SLOW stretches it so each action has room to read.
export const SLOW = 0.6116;

export const ExperienceScene: React.FC = () => {
  const frame = useCurrentFrame() / SLOW;
  const cam = camAt(frame);

  // Home -> All Apps: the home screen drifts up and softens while All Apps rises into place; it returns the same way.
  // Sequential, never overlapping: the home screen eases up and out, then All Apps eases up into place (and the reverse on return).
  const homeOut = p(frame, 162, 14, inOut);
  const appsInA = p(frame, 176, 16, outExpo);
  const appsOutA = p(frame, 224, 10, inOut);
  const homeBack = p(frame, 232, 12, inOut);
  const homeVis = 1 - homeOut * (1 - homeBack);
  const homeY = -30 * homeOut * (1 - homeBack);

  const screens: Screen[] = [];
  // Only the current focus image (and the one it is blending from) is drawn, so fading the home screen never shows older frames through it.
  const idx = FOCUS.reduce((acc, s, i) => (frame >= s.from - 1 ? i : acc), 0);
  const blend = idx === 0 ? 1 : p(frame, FOCUS[idx].from - 1, 3, linear);
  FOCUS.forEach((s, i) => {
    const o = i === idx ? blend : i === idx - 1 ? 1 - blend : 0;
    screens.push({src: `shots/home_f${s.img}.jpg`, opacity: o * homeVis, y: homeY});
  });
  screens.push({src: 'shots/menu.jpg', opacity: p(frame, 93, 3, linear) * (1 - p(frame, 150, 4, linear))});
  const npIn = p(frame, 296, 14, linear);
  const appsIn = Math.min(1, appsInA * (1 - appsOutA) + p(frame, 246, 16, linear)) * (1 - npIn);
  screens.push({src: 'shots/apps.jpg', opacity: appsIn, y: 36 * (1 - appsInA), scale: 1 + 0.03 * (1 - p(frame, 246, 16))});
  screens.push({src: 'shots/nowplaying.png', opacity: npIn});

  // Remote: slides in as the first button press approaches.
  const rx = interpolate(p(frame, 62, 28, outExpo), [0, 1], [2100, REMOTE_START.x]);
  let press: Press | null = null;
  if (frame >= 75 && frame < 94) press = {button: 'ok', t: p(frame, 75, 18, linear), hold: true};
  else if (frame >= 158 && frame < 182) press = {button: 'up', t: p(frame, 158, 22, linear)};
  else if (frame >= 240 && frame < 266) press = {button: 'apps', t: p(frame, 240, 24, linear)};

  const scrim = 0; // no slide-style captions: the real UI and the remote tell the story
  return (
    <AbsoluteFill style={{background: C.bg}}>
      <CameraMotion cam={cam}>
        <ProductFrame screens={screens} />
      </CameraMotion>
      <div
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          width: 1920,
          height: 400,
          opacity: scrim,
          background: 'linear-gradient(180deg, rgba(5,7,13,0.98) 0%, rgba(5,7,13,0.99) 62%, rgba(5,7,13,0) 100%)',
        }}
      />
      <Remote x={rx} y={REMOTE_START.y} w={REMOTE_START.w} press={press} />
    </AbsoluteFill>
  );
};
