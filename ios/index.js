// Polyfill for DOMRectReadOnly (required by some dependencies like Firebase)
if (typeof global.DOMRectReadOnly === 'undefined') {
  global.DOMRectReadOnly = class DOMRectReadOnly {
    constructor(x = 0, y = 0, width = 0, height = 0) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.top = y;
      this.right = x + width;
      this.bottom = y + height;
      this.left = x;
    }

    static fromRect(other) {
      return new DOMRectReadOnly(other?.x, other?.y, other?.width, other?.height);
    }

    toJSON() {
      return JSON.stringify(this);
    }
  };
}

import { registerRootComponent } from 'expo';
import App from './App';

// registerRootComponent calls AppRegistry.registerComponent('main', () => App);
// It also ensures that whether you load the app in Expo Go or in a native build,
// the environment is set up appropriately
registerRootComponent(App);
