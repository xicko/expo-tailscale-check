import { NativeModule, requireNativeModule } from 'expo';

import { ExpoTailscaleCheckModuleEvents } from './ExpoTailscaleCheck.types';
import { TailscaleStateType } from './types/state';

declare class ExpoTailscaleCheckModule extends NativeModule<ExpoTailscaleCheckModuleEvents> {
  getState(): TailscaleStateType;
  tailscaleInterface(): { name: string; ip: string } | null;
}

export default requireNativeModule<ExpoTailscaleCheckModule>('ExpoTailscaleCheck');
