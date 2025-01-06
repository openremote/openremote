module.exports = {
  preset: 'ts-jest/presets/default-esm',
  testEnvironment: 'jsdom',
  moduleNameMapper: {
      // handle module aliases
      '^@openremote/core(.*)$': '<rootDir>/component/core/src$1',
      '^@openremote/(.*)$': '<rootDir>/component/$1',
      // handle CSS imports
      '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
      // ESM module mapping
    '^(\\.{1,2}/.*)\\.js$': '$1',
  },
  modulePathIgnorePatterns: [
    "<rootDir>/app/console_loader/dist/",
    "<rootDir>/app/insights/dist/"
  ],
  transformIgnorePatterns: [
      // tell Jest to transform these node_modules packages
      '/node_modules/(?!(lit|@lit|lit-html|@lit-labs|@open-wc|@openremote)/)',
      '/node_modules/(?!(@lit|lit|@open-wc|@openremote)/).+\\.js$'
  ],
  transform: {
      '^.+\\.(ts|tsx)$': ['ts-jest', {
        useESM: true,  
        tsconfig: '<rootDir>/tsconfig.json'
      }],
      '^.+\\.(js|jsx)$': ['babel-jest', {
      presets: [
        ['@babel/preset-env', { targets: { node: 'current' }, modules: 'commonjs' }]
      ]
    }]
  },
  extensionsToTreatAsEsm: ['.ts', '.tsx'],
  setupFilesAfterEnv: ['<rootDir>/__tests__/jest.setup.js'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  setupFilesAfterEnv: ['<rootDir>/__tests__/jest.setup.js'],
  testEnvironmentOptions: {
    customExportConditions: ['node', 'node-addons'],
  },
};