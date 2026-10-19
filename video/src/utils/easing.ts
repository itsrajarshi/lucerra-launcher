import {Easing, interpolate} from 'remotion';

export const outExpo = Easing.bezier(0.16, 1, 0.3, 1);
export const inOut = Easing.bezier(0.65, 0, 0.35, 1);
export const inCubic = Easing.bezier(0.55, 0, 1, 0.45);
export const linear = (t: number) => t;

/** 0..1 progress of `frame` across [start, start + dur], clamped. */
export const p = (frame: number, start: number, dur: number, easing: (t: number) => number = inOut) =>
  interpolate(frame, [start, start + dur], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing,
  });

export const mix = (a: number, b: number, t: number) => a + (b - a) * t;

/** Deterministic PRNG (mulberry32) so every render is identical. */
export function rng(seed: number) {
  let s = seed >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
