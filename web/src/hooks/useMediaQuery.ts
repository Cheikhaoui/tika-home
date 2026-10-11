"use client";

import { useSyncExternalStore } from "react";

/**
 * True when the CSS media query matches, updated live (e.g. phone rotated).
 * useSyncExternalStore is React's way to read a browser value that can change outside React;
 * the server snapshot (false) is used while prerendering, where there is no window.
 */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(query);
      mql.addEventListener("change", onChange);
      return () => mql.removeEventListener("change", onChange);
    },
    () => window.matchMedia(query).matches,
    () => false,
  );
}
