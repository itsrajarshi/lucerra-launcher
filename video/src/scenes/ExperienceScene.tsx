import React from 'react';
import {AbsoluteFill, interpolate, OffthreadVideo, staticFile, useCurrentFrame} from 'remotion';
import {Cam, CameraMotion, lerpCam} from '../components/CameraMotion';
import {FRAME_CY, ProductFrame} from '../components/ProductFrame';
import {Press, Remote} from '../components/Remote';
import {C} from '../styles/tokens';
import {inOut, outExpo, p} from '../utils/easing';

export const REMOTE_START = {x: 1640, y: 120, w: 218};

const CAM_ID: Cam = {s: 1, fx: 960, fy: FRAME_CY, cx: 960, cy: FRAME_CY};
const CAM_DOCK: Cam = {s: 1.35, fx: 960, fy: FRAME_CY + 240, cx: 800, cy: 820};
// Push to the Now Playing widget (screenshot point 1150,95), kept clear of the remote on the right.
export const CAM_NP: Cam = {s: 1.9, fx: 1087, fy: FRAME_CY - 297, cx: 640, cy: 400};

/** Real screen recording of the TV (public/shots/nav.mp4), played in real time from START_AT. */
const START_AT = 36; // frames into the recording
const CAM_KEYS: [number, Cam][] = [
  [0, CAM_ID],
  [24, CAM_DOCK],
  [72, CAM_DOCK],
  [96, CAM_ID],
  [190, CAM_ID],
  [222, CAM_NP],
  [231, CAM_NP],
];

const camAt = (f: number): Cam => {
  for (let i = 0; i < CAM_KEYS.length - 1; i++) {
    const [a, ca] = CAM_KEYS[i];
    const [b, cb] = CAM_KEYS[i + 1];
    if (f <= b) return lerpCam(ca, cb, inOut(Math.min(1, Math.max(0, (f - a) / (b - a)))));
  }
  return CAM_NP;
};

/** Key presses as sent over adb while recording (scene frame at which each lands in the video). */
const PRESSES: {at: number; button: Press['button']}[] = [
  {at: 16, button: 'right'},
  {at: 54, button: 'right'},
  {at: 92, button: 'up'},
  {at: 129, button: 'right'},
  {at: 144, button: 'down'},
  {at: 189, button: 'home'},
];

/** Scene 2: a real recording of Lucerra being navigated with the remote, in real time. */
export const ExperienceScene: React.FC = () => {
  const frame = useCurrentFrame();
  const cam = camAt(frame);
  const rx = interpolate(p(frame, 2, 22, outExpo), [0, 1], [2100, REMOTE_START.x]);
  const hit = PRESSES.find((q) => frame >= q.at && frame < q.at + 14);
  const press: Press | null = hit ? {button: hit.button, t: p(frame, hit.at, 14)} : null;
  return (
    <AbsoluteFill style={{background: C.bg}}>
      <CameraMotion cam={cam}>
        <ProductFrame
          screens={[]}
          overlay={<OffthreadVideo src={staticFile('shots/nav.mp4')} startFrom={START_AT} muted style={{width: 1920, height: 1080}} />}
        />
      </CameraMotion>
      <Remote x={rx} y={REMOTE_START.y} w={REMOTE_START.w} press={press} />
    </AbsoluteFill>
  );
};
