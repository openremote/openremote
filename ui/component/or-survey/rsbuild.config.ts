import { defineConfig } from '@rsbuild/core';
import { getAppConfig, createDefinePlugin, createCopyPlugin } from '@openremote/util/rsbuild.util';
import packageJson from './package.json';

export default defineConfig((env) => {
  const mode = env.mode as 'development' | 'production';
  const isDevServer = process.argv.some(arg => arg.includes('dev'));
  const managerUrl = process.env.MANAGER_URL;
  const keycloakUrl = process.env.KEYCLOAK_URL;
  const port = process.env.PORT ? parseInt(process.env.PORT) : undefined;

  const appConfigOptions = {
    mode,
    isDevServer,
    dirname: __dirname,
    managerUrl,
    keycloakUrl,
    port
  };

  const config = getAppConfig(appConfigOptions);

  // Add or-survey-specific plugins
  const plugins = [
    createDefinePlugin(appConfigOptions),
    createCopyPlugin(__dirname),
    {
      name: 'or-survey-app-version',
      setup(api) {
        api.modifyBundlerChain((chain) => {
          const { rspack } = require('@rsbuild/core');
          chain.plugin('define-app-version').use(rspack.DefinePlugin, [{
            APP_VERSION: JSON.stringify(packageJson.version)
          }]);
        });
      },
    },
  ];

  return {
    ...config,
    plugins: [...(config.plugins || []), ...plugins],
  };
});
