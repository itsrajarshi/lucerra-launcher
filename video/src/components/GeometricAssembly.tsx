import React from 'react';

type Props = {
  /** 0 = thin four-arc ring, 1 = solid frosted disc. */
  u: number;
  /** Rotation of the arc group in degrees. */
  rot: number;
  /** 0..1.x scale of the blue core. */
  core: number;
  /** 0..1 how much of the ring has been drawn (stroke reveal). */
  draw?: number;
  size?: number;
  opacity?: number;
};

const R = 165; // outer radius in the 400-unit viewBox
const CORE = R * (14 / 26); // core/disc ratio taken from ic_launcher.xml

/**
 * The launcher icon (frosted white disc + blue core) built from a ring.
 * A circle of radius R - sw/2 stroked with width sw fills the disc as sw grows to R.
 */
export const GeometricAssembly: React.FC<Props> = ({u, rot, core, draw = 1, size = 400, opacity = 1}) => {
  const sw = 6 + (R - 6) * u;
  const r = R - sw / 2;
  const gap = 3 * (1 - u);
  const arc = 25 - gap;
  return (
    <svg width={size} height={size} viewBox="-200 -200 400 400" style={{overflow: 'visible', opacity}}>
      <defs>
        <linearGradient id="disc" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#ffffff" stopOpacity="0.92" />
          <stop offset="1" stopColor="#c9d3f4" stopOpacity="0.62" />
        </linearGradient>
        <radialGradient id="core" cx="0.35" cy="0.3" r="0.9">
          <stop offset="0" stopColor="#9fb4ff" />
          <stop offset="1" stopColor="#6C8CFF" />
        </radialGradient>
      </defs>
      <g transform={`rotate(${rot})`}>
        <circle
          r={r}
          fill="none"
          stroke="url(#disc)"
          strokeWidth={sw}
          pathLength={100}
          strokeDasharray={`${arc} ${gap}`}
          strokeDashoffset={(1 - draw) * 100}
        />
      </g>
      <circle r={R - 1} fill="none" stroke="rgba(255,255,255,0.5)" strokeWidth={1.5} opacity={u} />
      <circle r={CORE * core} fill="url(#core)" />
    </svg>
  );
};

export const DISC_R = R;
