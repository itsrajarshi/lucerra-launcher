import React from 'react';
import {C, F} from '../styles/tokens';
import {outExpo, p} from '../utils/easing';

type Props = {
  text: string;
  frame: number;
  start: number;
  /** Frame at which the text starts leaving (omit to stay). */
  exitAt?: number;
  size: number;
  weight?: number;
  tracking?: number; // em
  stagger?: number; // frames between letters
  color?: string;
  rise?: number; // px the letters travel while entering
};

/** Letter-by-letter entrance (rise + unblur) and a quick fade-out. All driven by `frame`. */
export const KineticText: React.FC<Props> = ({
  text,
  frame,
  start,
  exitAt,
  size,
  weight = 500,
  tracking = 0.12,
  stagger = 1.6,
  color = C.ink,
  rise = 34,
}) => {
  const exit = exitAt === undefined ? 0 : p(frame, exitAt, 12);
  return (
    <div
      style={{
        display: 'inline-flex',
        whiteSpace: 'pre',
        fontFamily: F.display,
        fontSize: size,
        fontWeight: weight,
        letterSpacing: `${tracking}em`,
        lineHeight: 1,
        color,
        opacity: 1 - exit,
      }}
    >
      {Array.from(text).map((ch, i) => {
        const t = p(frame, start + i * stagger, 22, outExpo);
        return (
          <span
            key={i}
            style={{
              display: 'inline-block',
              opacity: t,
              transform: `translateY(${(1 - t) * rise}px)`,
              filter: `blur(${(1 - t) * 10}px)`,
            }}
          >
            {ch}
          </span>
        );
      })}
    </div>
  );
};
