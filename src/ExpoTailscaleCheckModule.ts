import { NativeModule, requireNativeModule } from 'expo';

import { ExpoTailscaleCheckModuleEvents } from './ExpoTailscaleCheck.types';
import { TailscaleStateType } from './types/state';

declare class ExpoTailscaleCheckModule extends NativeModule<ExpoTailscaleCheckModuleEvents> {
  getState(): TailscaleStateType;
  tailscaleInterface(): { name: string; ip: string } | null;

  /**
   * Launches the Tailscale client app.
   *
   * @platform android
   *
   * @param packageName - Custom package name if not using the official Tailscale android client.
   * Custom package name must be added to queries in android manifest, otherwise nothing launches.
   *
   * @returns void
   */
  openTailscaleApp(packageName?: string): void;

  /**
   * Sends a broadcast intent to connect to the Tailscale VPN.
   *
   * @platform android
   *
   * @param packageName - Custom package name if not using the official Tailscale android client.
   * Custom package name must be added to queries in android manifest, otherwise nothing launches.
   *
   * @returns void
   */
  connectVPN(packageName?: string): void;

  /**
   * Sends a broadcast intent to disconnect from the Tailscale VPN.
   *
   * @platform android
   *
   * @param packageName - Custom package name if not using the official Tailscale android client.
   * Custom package name must be added to queries in android manifest, otherwise nothing launches.
   *
   * @returns void
   */
  disconnectVPN(packageName?: string): void;
}

export default requireNativeModule<ExpoTailscaleCheckModule>('ExpoTailscaleCheck');
