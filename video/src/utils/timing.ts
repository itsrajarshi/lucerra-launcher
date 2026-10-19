export const FPS = 30;
export const W = 1920;
export const H = 1080;

/** Absolute scene timing in frames. Scene 3 runs 20 frames past scene 4's start so the lockup can fade over it. */
export const SCENES = {
  opening: {from: 0, dur: 180},
  transform: {from: 180, dur: 150},
  reveal: {from: 330, dur: 230},
  experience: {from: 540, dur: 340},
  end: {from: 880, dur: 170},
} as const;

export const TOTAL = 1050;
export const sec = (s: number) => Math.round(s * FPS);
