import React from 'react';
import {AbsoluteFill} from 'remotion';

type Cam = {s: number; fx: number; fy: number; cx: number; cy: number};

/** Zoom-in strength. 1 = as designed; the 9:16 reel lowers it so zoomed shots stay inside the frame. */
export const camLimit = {k: 1};

/** Camera as "put canvas point (fx,fy) at (cx,cy) and scale by s". Push-ins (s > 1) are eased back toward the neutral view by camLimit.k. */
export const camTransform = (cam: Cam) => {
  const w = 1 - (1 - camLimit.k) * Math.min(1, Math.max(0, (cam.s - 1) / 0.2));
  const {s, fx, fy, cx, cy} = {
    s: 1 + (cam.s - 1) * w,
    fx: 960 + (cam.fx - 960) * w,
    fy: 540 + (cam.fy - 540) * w,
    cx: 960 + (cam.cx - 960) * w,
    cy: 540 + (cam.cy - 540) * w,
  };
  return `translate(${cx}px, ${cy}px) scale(${s}) translate(${-fx}px, ${-fy}px)`;
};

export const lerpCam = (a: Cam, b: Cam, t: number): Cam => ({
  s: a.s + (b.s - a.s) * t,
  fx: a.fx + (b.fx - a.fx) * t,
  fy: a.fy + (b.fy - a.fy) * t,
  cx: a.cx + (b.cx - a.cx) * t,
  cy: a.cy + (b.cy - a.cy) * t,
});

export const CameraMotion: React.FC<{cam: Cam; children: React.ReactNode}> = ({cam, children}) => (
  <AbsoluteFill style={{transform: camTransform(cam), transformOrigin: '0 0'}}>{children}</AbsoluteFill>
);

export type {Cam};
