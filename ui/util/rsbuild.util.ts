import type { RsbuildConfig, RsbuildPlugin } from '@rsbuild/core';
import * as path from 'path';
import * as fs from 'fs';

export interface AppConfigOptions {
  mode: 'development' | 'production';
  isDevServer: boolean;
  dirname: string;
  managerUrl?: string;
  keycloakUrl?: string;
  port?: number;
}

export function getAppConfig(options: AppConfigOptions): RsbuildConfig {
  const { mode, isDevServer, dirname, managerUrl, keycloakUrl, port = 9000 } = options;
  const production = mode === 'production';
  const resolvedManagerUrl = managerUrl || (production && !isDevServer ? undefined : 'http://localhost:8080');
  const outputPath = isDevServer ? 'src' : 'dist';

  if (isDevServer) {
    console.log('');
    console.log('To customise the URL of the manager and/or keycloak use the managerUrl and/or keycloakUrl');
    console.log('environment variables. MANAGER_URL=' + resolvedManagerUrl);
    console.log('KEYCLOAK_URL=' + keycloakUrl);
    console.log('PORT=' + port);
    console.log('');
  }

  const config: RsbuildConfig = {
    mode: mode,
    
    source: {
      entry: {
        bundle: './src/index.ts',
      },
      // TypeScript configuration with legacy decorators support
      decorators: {
        version: 'legacy',
      },
    },

    resolve: {
      alias: {
        // Webpack fallbacks for Node.js modules
        vm: false,
        querystring: require.resolve('querystring-es3'),
        // Workspace aliases for proper resolution
        '@openremote/model': path.resolve(dirname, '../../component/model/src'),
        '@openremote/rest': path.resolve(dirname, '../../component/rest/src'),
        '@openremote/core': path.resolve(dirname, '../../component/core/src'),
        '@openremote/or-app': path.resolve(dirname, '../../component/or-app/src'),
        '@openremote/or-translate': path.resolve(dirname, '../../component/or-translate/src'),
        '@openremote/or-components': path.resolve(dirname, '../../component/or-components/src'),
        '@openremote/or-mwc-components': path.resolve(dirname, '../../component/or-mwc-components/src'),
        '@openremote/or-asset-tree': path.resolve(dirname, '../../component/or-asset-tree/src'),
        '@openremote/or-chart': path.resolve(dirname, '../../component/or-chart/src'),
        '@openremote/or-icon': path.resolve(dirname, '../../component/or-icon/src'),
        '@openremote/or-input': path.resolve(dirname, '../../component/or-input/src'),
        '@openremote/or-map': path.resolve(dirname, '../../component/or-map/src'),
        '@openremote/or-rules': path.resolve(dirname, '../../component/or-rules/src'),
        '@openremote/util': path.resolve(dirname, '../../util'),
      },
      // Add extensions for TypeScript resolution
      extensions: ['.ts', '.tsx', '.js', '.jsx', '.json'],
      // Enable symlinks resolution for workspace packages
      symlinks: true,
    },

    output: {
      distPath: {
        root: path.resolve(dirname, outputPath),
      },
      filename: {
        js: production ? '[name].[contenthash].js' : '[name].js',
      },
      assetPrefix: isDevServer ? `/${dirname.split(path.sep).slice(-1)[0]}/` : './',
      // Source map configuration - replaces source-map-loader
      sourceMap: {
        js: 'source-map',
        css: true,
      },
    },

    server: isDevServer ? {
      historyApiFallback: {
        index: `/${dirname.split(path.sep).slice(-1)[0]}/`,
      },
      port: port,
      open: false,
      hmr: false, // HMR doesn't work with webcomponents at present
      liveReload: true,
    } : undefined,

    html: {
      // HTML template configuration - replaces html-webpack-plugin
      template: './index.html',
    },

    tools: {
      // CSS configuration - replaces css-loader
      cssLoader: {
        modules: {
          // CSS modules configuration
          auto: (resourcePath: string) => {
            return /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/.test(resourcePath);
          },
          localIdentName: '[local]',
          exportLocalsConvention: 'asIs',
        },
      },
    },
  };

  // Production-specific optimizations
  if (production) {
    config.tools = {
      ...config.tools,
      swc: {
        jsc: {
          transform: {
            useDefineForClassFields: false, // For decorators compatibility
          },
        },
      },
    };
  }

  // Development-specific configurations
  if (isDevServer) {
    config.performance = {
      hints: false,
    };
  }

  return config;
}

// Plugin for environment variables
export function createDefinePlugin(options: AppConfigOptions): RsbuildPlugin {
  const { mode, managerUrl, keycloakUrl } = options;
  const production = mode === 'production';
  const resolvedManagerUrl = managerUrl || (production && !options.isDevServer ? undefined : 'http://localhost:8080');

  return {
    name: 'define-env-vars',
    setup(api) {
      api.modifyBundlerChain((chain) => {
        const { rspack } = require('@rsbuild/core');
        chain.plugin('define').use(rspack.DefinePlugin, [{
          PRODUCTION: JSON.stringify(production),
          MANAGER_URL: JSON.stringify(resolvedManagerUrl),
          KEYCLOAK_URL: JSON.stringify(keycloakUrl),
          'process.env': {
            BABEL_ENV: JSON.stringify(mode),
          },
        }]);
      });
    },
  };
}

// Copy patterns for static assets
export function getCopyPatterns(dirname: string) {
  const patterns = [
    {
      from: path.dirname(require.resolve('@webcomponents/webcomponentsjs')),
      to: 'modules/@webcomponents/webcomponentsjs',
      globOptions: {
        ignore: ['!*.js'],
      },
    },
  ];

  // Check if images dir exists
  if (fs.existsSync(path.join(dirname, 'images'))) {
    patterns.push({
      from: './images',
      to: 'images',
    });
  }

  // Check if locales dir exists
  if (fs.existsSync(path.join(dirname, 'locales'))) {
    patterns.push({
      from: './locales',
      to: 'locales',
    });
  }

  return patterns;
}

// Plugin for copying assets
export function createCopyPlugin(dirname: string): RsbuildPlugin {
  const patterns = getCopyPatterns(dirname);
  
  return {
    name: 'copy-assets',
    setup(api) {
      api.modifyBundlerChain((chain) => {
        const { rspack } = require('@rsbuild/core');
        chain.plugin('copy').use(rspack.CopyRspackPlugin, [patterns]);
      });
    },
  };
}

// Plugin for app version definition
export function createAppVersionPlugin(appName: string, version: string): RsbuildPlugin {
  return {
    name: `${appName}-app-version`,
    setup(api) {
      api.modifyBundlerChain((chain) => {
        const { rspack } = require('@rsbuild/core');
        chain.plugin('define-app-version').use(rspack.DefinePlugin, [{
          APP_VERSION: JSON.stringify(version)
        }]);
      });
    },
  };
}

// Simplified app configuration with all standard plugins
export function createStandardAppConfig(options: AppConfigOptions & { 
  appName: string; 
  version: string; 
}): RsbuildConfig {
  const config = getAppConfig(options);
  
  const plugins = [
    createDefinePlugin(options),
    createCopyPlugin(options.dirname),
    createAppVersionPlugin(options.appName, options.version),
  ];

  return {
    ...config,
    plugins: [...(config.plugins || []), ...plugins],
  };
}

export function getLibName(componentName: string): string {
  return componentName.split('-').map(part => 
    part.charAt(0).toUpperCase() + part.slice(1)
  ).join('');
}

// Export utility functions for backward compatibility
export function getStandardModuleRules() {
  return {
    rules: [
      {
        test: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/, //output css as strings
        type: "asset/source"
      },
      {
        test: /\.css$/, //
        exclude: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/,
        use: [
          { loader: "css-loader" }
        ]
      },
      {
        test: /\.(png|jpg|ico|gif|svg|eot|ttf|woff|woff2|mp4)$/,
        type: "asset",
        generator: {
          filename: 'images/[hash][ext][query]'
        }
      },
      {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        use: {
          // TODO: Switch to builtin:swc-loader, and remove ts-loader / webpack dependency
          loader: "ts-loader",
          options: {
            projectReferences: true
          }
        }
      }
    ]
  };
}