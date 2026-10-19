import React from 'react';
import {Img, staticFile} from 'remotion';
import {C, F} from '../styles/tokens';
import {rng} from '../utils/easing';

type Kind = 'tile' | 'panel' | 'chip' | 'ring' | 'rule';

export type Frag = {
  id: number;
  kind: Kind;
  d: number; // depth 0.45 far .. 1 near
  x: number;
  y: number;
  rot: number;
  vx: number;
  vy: number;
  vr: number;
  w: number;
  h: number;
  img?: string;
  label?: string;
};

const TILES = ['netflix', 'prime', 'youtube', 'hotstar', 'hoichoi', 'binge', 'z5', 'spotify', 'ytmusic', 'playstore', 'playmovies', 'wallpaper', 'apple'];
// Labels that really appear on the stock Google TV home.
const LABELS = ['Sponsored', 'Top Selling Movies', 'Trending on Google', 'Play Next', 'Popular Movies and Shows', 'New release', 'Top Picks For You'];
const KINDS: Kind[] = ['tile', 'tile', 'tile', 'tile', 'tile', 'panel', 'panel', 'chip', 'chip', 'ring', 'rule', 'tile'];

export const N_FRAGS = 36;

const make = (): Frag[] => {
  const r = rng(7);
  return Array.from({length: N_FRAGS}, (_, id) => {
    const kind = KINDS[Math.floor(r() * KINDS.length)];
    const d = [0.45, 0.75, 1][Math.floor(r() * 3)];
    const ang = r() * Math.PI * 2;
    const sp = (0.25 + r() * 0.7) * d;
    return {
      id,
      kind,
      d,
      x: -80 + r() * 2080,
      y: -40 + r() * 1160,
      rot: (r() - 0.5) * 40,
      vx: Math.cos(ang) * sp,
      vy: Math.sin(ang) * sp,
      vr: (r() - 0.5) * 0.12,
      w: kind === 'tile' ? 308 * d * 0.8 : kind === 'panel' ? 300 * d : kind === 'chip' ? 250 * d : kind === 'ring' ? 150 * d : 380 * d,
      h: kind === 'tile' ? 174 * d * 0.8 : kind === 'panel' ? 170 * d : kind === 'chip' ? 56 * d : kind === 'ring' ? 150 * d : 2,
      img: kind === 'tile' ? TILES[Math.floor(r() * TILES.length)] : undefined,
      label: kind === 'chip' ? LABELS[Math.floor(r() * LABELS.length)] : undefined,
    };
  });
};

export const FRAGS: Frag[] = make();

/** Position of a fragment after `t` frames of (accelerating) drift. Parallax offset is included. */
export const fragPos = (f: Frag, t: number) => {
  const te = t + 0.012 * t * t;
  return {x: f.x + f.vx * te - t * 0.35 * f.d, y: f.y + f.vy * te, r: f.rot + f.vr * te};
};

/** The opening scene's camera at frame t, as numbers (so the next scene can start from the same pixels). */
export const openingCam = (t: number) => ({tx: -t * 0.12, ty: t * 0.05, s: 1 + t * 0.0004, rot: t * 0.004});

/** A canvas point after the opening camera (translate, scale, rotate about 960,540). */
export const camPoint = (x: number, y: number, t: number) => {
  const c = openingCam(t);
  const a = (c.rot * Math.PI) / 180;
  const dx = (x - 960) * c.s;
  const dy = (y - 540) * c.s;
  return {x: 960 + c.tx + dx * Math.cos(a) - dy * Math.sin(a), y: 540 + c.ty + dx * Math.sin(a) + dy * Math.cos(a)};
};

export const FragView: React.FC<{f: Frag}> = ({f}) => {
  const blur = f.d < 0.5 ? 5 : f.d < 0.8 ? 1.5 : 0;
  const base: React.CSSProperties = {width: f.w, height: f.h, filter: blur ? `blur(${blur}px)` : undefined};
  if (f.kind === 'tile') {
    return <Img src={staticFile(`tiles/${f.img}.png`)} style={{...base, borderRadius: 14 * f.d, display: 'block'}} />;
  }
  if (f.kind === 'panel') {
    const bar = (w: number, o = 0.18) => <div style={{height: 10 * f.d, width: `${w}%`, borderRadius: 6, background: `rgba(255,255,255,${o})`, marginTop: 12 * f.d}} />;
    return (
      <div style={{...base, borderRadius: 18 * f.d, border: `1px solid ${C.line}`, background: 'rgba(255,255,255,0.04)', padding: 20 * f.d}}>
        <div style={{width: 34 * f.d, height: 34 * f.d, borderRadius: 10, background: 'rgba(255,255,255,0.14)'}} />
        {bar(70)}
        {bar(90, 0.12)}
        {bar(45, 0.12)}
      </div>
    );
  }
  if (f.kind === 'chip') {
    return (
      <div
        style={{
          ...base,
          borderRadius: 999,
          border: `1px solid ${C.line}`,
          background: 'rgba(255,255,255,0.06)',
          color: C.dim,
          fontFamily: F.display,
          fontWeight: 400,
          fontSize: 22 * f.d,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          whiteSpace: 'nowrap',
        }}
      >
        {f.label}
      </div>
    );
  }
  if (f.kind === 'ring') {
    return <div style={{...base, borderRadius: '50%', border: `2px solid rgba(255,255,255,${0.2 * f.d + 0.1})`}} />;
  }
  return <div style={{...base, background: `rgba(255,255,255,${0.16 * f.d})`}} />;
};

/** A fragment positioned by its centre. */
export const FragAt: React.FC<{f: Frag; x: number; y: number; rot: number; scale?: number; opacity?: number}> = ({f, x, y, rot, scale = 1, opacity = 1}) => (
  <div
    style={{
      position: 'absolute',
      left: x - f.w / 2,
      top: y - f.h / 2,
      opacity: opacity * (f.d < 0.5 ? 0.55 : f.d < 0.8 ? 0.82 : 1),
      transform: `rotate(${rot}deg) scale(${scale})`,
    }}
  >
    <FragView f={f} />
  </div>
);
