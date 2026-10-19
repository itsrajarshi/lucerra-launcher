import React from 'react';
import {AbsoluteFill} from 'remotion';

type Cam = {s: number; fx: number; fy: number; cx: number; cy: number};

/** Camera as "put canvas point (fx,fy) at (cx,cy) and scale by s". */
export const camTransform = ({s, fx, fy, cx, cy}: Cam) =>
  `translate(${cx}px, ${cy}px) scale(${s}) translate(${-fx}px, ${-fy}px)`;

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
