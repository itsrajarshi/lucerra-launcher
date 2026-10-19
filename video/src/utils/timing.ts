export const FPS = 30;
export const W = 1920;
export const H = 1080;

/** Absolute scene timing in frames. Scene 3 runs 20 frames past scene 4's start so the lockup can fade over it. */
export const SCENES = {
  stock: {from: 0, dur: 96},
  experience: {from: 96, dur: 208},
  end: {from: 304, dur: 118},
} as const;

export const TOTAL = 422;
export const sec = (s: number) => Math.round(s * FPS);
