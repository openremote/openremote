import { defineConfig } from '@rsbuild/core';
import { createStandardAppConfig } from '@openremote/util/rsbuild.util';
import packageJson from './package.json';

export default defineConfig((env) => {
  const mode = env.mode as 'development' | 'production';
  const isDevServer = process.argv.some(arg => arg.includes('dev'));
  const managerUrl = process.env.MANAGER_URL;
  const keycloakUrl = process.env.KEYCLOAK_URL;
  const port = process.env.PORT ? parseInt(process.env.PORT) : undefined;

  return createStandardAppConfig({
    mode,
    isDevServer,
    dirname: __dirname,
    managerUrl,
    keycloakUrl,
    port,
    appName: 'demo-rest',
    version: packageJson.version
  });
});
