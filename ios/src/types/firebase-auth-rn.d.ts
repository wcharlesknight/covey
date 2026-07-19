import type { Persistence } from '@firebase/auth';

// getReactNativePersistence is exported by @firebase/auth's RN bundle (dist/rn/index.js)
// but missing from the web-facing type declarations (auth-public.d.ts). This augments
// the module so tsc resolves it correctly while Metro loads the right bundle at runtime.
declare module '@firebase/auth' {
  export function getReactNativePersistence(storage: object): Persistence;
}
