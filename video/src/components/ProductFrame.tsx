import React from 'react';
import {AbsoluteFill, Img, staticFile} from 'remotion';

export const FRAME_CX = 960;
export const FRAME_CY = 616;
const SCALE = 2 / 3; // 1920x1080 screenshot -> 1280x720 on canvas
const PAD = 7;

/** Maps a point in screenshot pixels (1920x1080) to canvas pixels, for scale 1. */
export const toCanvas = (x: number, y: number) => ({
  x: FRAME_CX - 640 + x * SCALE,
  y: FRAME_CY - 360 + y * SCALE,
});

export type Screen = {src: string; opacity: number; scale?: number};

type Props = {
  screens: Screen[];
  /** Extra children rendered in screenshot space (1920x1080). */
  overlay?: React.ReactNode;
  style?: React.CSSProperties;
};

/** A TV bezel around real TV screenshots. Children live in 1920x1080 screenshot space. */
export const ProductFrame: React.FC<Props> = ({screens, overlay, style}) => (
  <div
    style={{
      position: 'absolute',
      left: FRAME_CX - 640 - PAD,
      top: FRAME_CY - 360 - PAD,
      width: 1280 + PAD * 2,
      height: 720 + PAD * 2,
      padding: PAD,
      borderRadius: 14,
      background: '#0a0c11',
      border: '2px solid #2a2e38',
      boxShadow: '0 70px 140px -40px rgba(0,0,0,0.85), 0 0 0 1px rgba(255,255,255,0.03)',
      ...style,
    }}
  >
    <div style={{position: 'relative', width: 1280, height: 720, borderRadius: 12, overflow: 'hidden', background: '#000'}}>
      <div style={{position: 'absolute', left: 0, top: 0, width: 1920, height: 1080, transform: `scale(${SCALE})`, transformOrigin: '0 0'}}>
        {screens.map((s, i) =>
          s.opacity > 0.001 ? (
            <AbsoluteFill key={`${i}-${s.src}`} style={{opacity: s.opacity, transform: s.scale ? `scale(${s.scale})` : undefined}}>
              <Img src={staticFile(s.src)} style={{width: 1920, height: 1080}} />
            </AbsoluteFill>
          ) : null,
        )}
        {overlay}
      </div>
      {/* glass reflection */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(115deg, rgba(255,255,255,0.07) 0%, rgba(255,255,255,0.0) 38%, rgba(255,255,255,0.0) 70%, rgba(255,255,255,0.03) 100%)',
          pointerEvents: 'none',
        }}
      />
    </div>
    {/* stand feet */}
    {[0.2, 0.8].map((x) => (
      <div
        key={x}
        style={{
          position: 'absolute',
          left: `${x * 100}%`,
          bottom: -34,
          width: 150,
          height: 34,
          marginLeft: -75,
          background: 'linear-gradient(180deg, #1a1d25 0%, #0b0d12 100%)',
          clipPath: 'polygon(14% 0, 86% 0, 100% 100%, 0 100%)',
        }}
      />
    ))}
  </div>
);
