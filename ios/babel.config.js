module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    '@babel/preset-flow',
    '@babel/preset-typescript',
    ['@babel/preset-react', { runtime: 'automatic' }],
  ],
  plugins: [
    '@babel/plugin-proposal-class-properties',
    '@babel/plugin-transform-flow-strip-types',
  ],
  env: {
    test: {
      presets: [
        ['@babel/preset-env', { targets: { node: 'current' }, modules: 'commonjs' }],
        '@babel/preset-flow',
        '@babel/preset-typescript',
        ['@babel/preset-react', { runtime: 'automatic' }],
      ],
      plugins: [
        '@babel/plugin-proposal-class-properties',
        '@babel/plugin-transform-flow-strip-types',
      ],
    },
  },
};
