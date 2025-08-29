/**
 * Legacy compatibility layer for the old webpack.util.js
 * This provides the same exports as webpack.util.js but uses the new Rsbuild utilities
 */
import {
  getAppConfig,
  createDefinePlugin,
  createCopyPlugin,
  getLibName,
  getStandardModuleRules,
} from './rsbuild.util.js';

// Export the functions with the old interface for backward compatibility
export {
  getLibName,
  getStandardModuleRules,
  getAppConfig,
  createDefinePlugin as definePlugin,
  createCopyPlugin as copyPlugin,
};

// Legacy function that maintains the old webpack-style API
export function generateExports(dirname) {
  const libName = getLibName(dirname.split(require('path').sep).pop());
  
  // For legacy compatibility, return a simplified config array
  return [{
    entry: { index: "./src/index.ts" },
    mode: "production",
    output: {
      filename: "[name].js",
      path: require('path').resolve(dirname, "dist/umd"),
      library: libName,
      libraryTarget: "umd"
    },
    resolve: {
      extensions: [".ts", ".tsx", "..."],
      fallback: { "vm": false }
    },
    module: getStandardModuleRules(),
    // Note: externals would need to be handled separately in Rsbuild
  }];
}

// Legacy function for external dependencies (simplified)
export function generateExternals(bundle) {
  // This would need to be handled differently in Rsbuild
  return {};
}

// Legacy function for external OR dependencies (simplified) 
export function ORExternals(context, callback) {
  // This would need to be handled differently in Rsbuild
  callback();
}