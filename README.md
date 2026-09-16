<div align="center">

# expo-tailscale-check ✧*:･ﾟ

**a native module that tells your expo app whether it's on tailscale — and whether traffic is split-tunneled or riding an exit node.**

[![Expo](https://img.shields.io/badge/Expo-54.0+-000020?logo=expo&logoColor=white&style=flat-square)](https://expo.dev/)
[![React Native](https://img.shields.io/badge/React_Native-0.81-61DAFB?logo=react&logoColor=black&style=flat-square)](https://reactnative.dev/)
[![iOS](https://img.shields.io/badge/iOS-15.1+-000000?logo=apple&logoColor=white&style=flat-square)](https://developer.apple.com/ios/)
[![Android](https://img.shields.io/badge/Android-WIP-3DDC84?logo=android&logoColor=white&style=flat-square)](#platform-support)
[![Tailscale](https://img.shields.io/badge/Tailscale-Networking-4B23D1?logo=tailscale&logoColor=white&style=flat-square)](https://tailscale.com/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white&style=flat-square)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-6E7681?style=flat-square)](./LICENSE)

</div>

---

> [!NOTE]
> this is a **native module** — no UI, just swift/kotlin methods and a hook. it cannot run in expo go; you'll need a development build or a `prebuild`.

## features

- **reactive state**: a `useTailscaleState()` hook that re-renders when the tailnet state changes.
- **exit node vs split tunnel**: not just "vpn on/off" - it reads the routing table to tell whether entire traffic is tunneled or only tailnet traffic.
- **interface details**: grab the tailscale `utun` interface and its `100.64.0.0/10` CGNAT address.
- **event stream**: listen to `onStateChange` for updates.
- **zero third-party deps**: pure `Network` / `getifaddrs` / `sysctl` on apple. nothing to audit but the platform itself.

---

## platform support

| platform | status |
| :------- | :----- |
| ios      | ✅ done |
| android  | 🚧 in progress |
| web      | ❌ not supported (it's native) |

---

## install

```bash
npx expo install expo-tailscale-check
```

then rebuild the native project — autolinking handles the rest:

```bash
npx expo prebuild (--clean)
npx expo run:ios
```

---

## usage

**hook**

```tsx
import { useTailscaleState } from 'expo-tailscale-check';

function Status() {
  const state = useTailscaleState({ debounceMs: 400 });
  // 'NONE' | 'SPLIT_TUNNEL' | 'EXIT_NODE' | 'UNKNOWN'
  return <Text>{state}</Text>;
}
```

**direct**:

```ts
import ExpoTailscaleCheck from 'expo-tailscale-check';

ExpoTailscaleCheck.getState();            // synchronous, cached
ExpoTailscaleCheck.tailscaleInterface();  // { name, ip } | null

const sub = ExpoTailscaleCheck.addListener('onStateChange', ({ state }) => {
  console.log('tailscale ->', state);
});
// sub.remove() when you're done
```

---

## api

| member | returns | notes |
| :----- | :------ | :---- |
| `useTailscaleState(options?)` | `TailscaleStateType` | react hook. `options.debounceMs` (default `100`). |
| `getState()` | `TailscaleStateType` | cached current state (`'UNKNOWN'` until first detection). |
| `tailscaleInterface()` | `{ name, ip } \| null` | the tailscale `utun` interface + its CGNAT ipv4. |
| `onStateChange` | `{ state }` | emitted whenever the state changes. |

**`TailscaleStateType`**

- `NONE` — no tailscale interface present, tailscale likely not connected.
- `SPLIT_TUNNEL` — tailscale's up, but the general traffic still leaves the physical interface.
- `EXIT_NODE` — the default route goes through tailscale (everything's tunneled through a node).
- `UNKNOWN` — not determined yet.

---

## how it works

on ios it enumerates interfaces (`getifaddrs`) looking for an up `utun*` carrying a tailscale CGNAT address (`100.64.0.0/10`), then reads the routing table (`sysctl` / `PF_ROUTE`) to decide whether its split-tunnel vs exit-node, ignoring interface-scoped routes so tailscale's always-present `utun` default does not trip a false positive. detection is ipv4-based.

---

> [!WARNING]
> early days — the api may shift and android isn't wired up yet. pin a version if you depend on it.

---

<div align="center">

MIT © [xicko](https://github.com/xicko)

</div>
