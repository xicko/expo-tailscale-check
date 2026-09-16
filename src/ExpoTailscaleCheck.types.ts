import { TailscaleStateType } from './types/state';

export type ExpoTailscaleCheckModuleEvents = {
  onStateChange: (params: ChangeEventPayload) => void;
};

export type ChangeEventPayload = {
  state: TailscaleStateType;
};
