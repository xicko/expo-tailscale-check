import { useEffect, useState } from 'react';

import ExpoTailscaleCheck from '../ExpoTailscaleCheckModule';
import type { TailscaleStateType } from '../types/state';

export default function useTailscaleState(options?: { debounceMs?: number }): TailscaleStateType {
  const debounceMs = options?.debounceMs ?? 100;
  const [state, setState] = useState<TailscaleStateType>(() => ExpoTailscaleCheck.getState());

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    const sub = ExpoTailscaleCheck.addListener('onStateChange', ({ state }) => {
      if (debounceMs > 0) {
        clearTimeout(timer);
        timer = setTimeout(() => setState(state), debounceMs);
      } else {
        setState(state);
      }
    });
    return () => {
      clearTimeout(timer);
      sub.remove();
    };
  }, [debounceMs]);

  return state;
}
