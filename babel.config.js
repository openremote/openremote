module.exports = {
  presets: [
    ['@babel/preset-env', {
      targets: {node: 'current'},
      modules: 'commonjs' // Explicitly set modules to commonjs
    }],
    '@babel/preset-typescript'
  ],
  plugins: [
    '@babel/plugin-transform-runtime'
  ]
};