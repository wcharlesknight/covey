const { getDefaultConfig } = require('expo/metro-config');

/** @type {import('expo/metro-config').MetroConfig} */
const config = getDefaultConfig(__dirname);

// Firebase JS SDK uses package.json `react-native` fields for RN-specific bundles,
// but the `exports` field (enabled by default in Metro 0.81) has no `react-native`
// condition for @firebase/app, causing split module instances and auth registration failure.
config.resolver.unstable_enablePackageExports = false;

module.exports = config;
