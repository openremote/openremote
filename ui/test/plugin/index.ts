import fs from "node:fs";
import path from "path";
import webpack from "webpack";
import WebpackDevServer from "webpack-dev-server";
import HtmlWebpackPlugin from "html-webpack-plugin";

// @ts-expect-error No declaration file available
import { getStandardModuleRules } from "@openremote/util";
// @ts-expect-error No declaration file available (internal Playwright module)
import { isURLAvailable } from "playwright-core/lib/utils";

import type { FullConfig } from "playwright/types/testReporter";

import {
  frameworkConfig,
  resolveDirs,
  resolveEndpoint,
  populateComponentsFromTests,
  ImportInfo,
  transformIndexFile,
} from "./webpackUtils";

let devServer: WebpackDevServer;

export function createPlugin() {
  let config: FullConfig;
  let configDir: string;

  return {
    name: "playwright-webpack-plugin",

    setup: async (cfg: FullConfig, cfgDir: string) => {
      config = cfg;
      configDir = cfgDir;
    },

    begin: async () => {
      const result = await buildBundle(config, configDir);
      if (!result) return;

      const { webpackConfig } = result;
      devServer = new WebpackDevServer(webpackConfig.devServer, webpack(webpackConfig));
      await devServer.start();

      if (!devServer.server) return;

      const address = devServer.server.address();
      if (address && typeof address === "object") {
        const protocol = webpackConfig.devServer.https ? "https:" : "http:";
        process.env.PLAYWRIGHT_TEST_BASE_URL = `${protocol}//${address.address}:${address.port}`;
      }
    },

    end: async () => {
      if (devServer) await devServer.stop();
    },

    populateDependencies: async () => {
      await buildBundle(config, configDir);
    },

    clearCache: async () => {
      const dirs = await resolveDirs(configDir, config);
      if (dirs) {
        console.log(`Removing ${await fs.promises.realpath(dirs.outDir)}`);
        await fs.promises.rm(dirs.outDir, { recursive: true, force: true });
      }
    },

    startDevServer: async () => {
      // For debugging via playwright directly
      return devServer;
    },
  };
}

async function buildBundle(config: FullConfig, configDir: string): Promise<{ webpackConfig: any } | null> {
  const { registerSourceFile } = frameworkConfig(config);
  const endpoint = resolveEndpoint(config);
  if (!endpoint) return null;

  const protocol = endpoint.https ? "https:" : "http:";
  const url = new URL(`${protocol}//${endpoint.host}:${endpoint.port}`);
  if (await isURLAvailable(url, true)) {
    console.log(`Dev Server already running at ${url.toString()}`);
    process.env.PLAYWRIGHT_TEST_BASE_URL = url.toString();
    return null;
  }

  const dirs = await resolveDirs(configDir, config);
  if (!dirs) {
    console.log(`Template file playwright/index.html is missing.`);
    return null;
  }

  const componentRegistry: Map<string, ImportInfo> = new Map();
  const componentsByImportingFile = new Map<string, string[]>();
  // Populate component registry based on the tests' component imports.
  await populateComponentsFromTests(componentRegistry, componentsByImportingFile);

  // Consider including buildInfo in the cache dir to be able to know how to invalidate the cache dir
  const registerSource = fs.readFileSync(registerSourceFile, "utf-8");
  const indexSourcePath = path.join(dirs.templateDir, "index.js");
  const transformedIndex = transformIndexFile(
    fs.readFileSync(indexSourcePath, "utf-8"),
    registerSource,
    componentRegistry
  );
  const outputIndexPath = path.join(dirs.outDir, "index.js");
  fs.mkdirSync(dirs.outDir, { recursive: true });
  fs.writeFileSync(outputIndexPath, transformedIndex);

  const webpackConfig = {
    mode: "development",
    entry: outputIndexPath,
    output: {
      path: dirs.outDir,
      filename: "bundle.js",
    },
    devtool: "source-map",
    devServer: {
      static: {
        directory: dirs.templateDir,
      },
      host: endpoint.host,
      port: endpoint.port,
      https: !!endpoint.https,
    },
    module: getStandardModuleRules(),
    plugins: [
      new webpack.DefinePlugin({
        __REGISTER_SOURCE__: JSON.stringify(registerSource),
        __COMPONENTS__: JSON.stringify([...componentRegistry.values()]),
      }),
      new HtmlWebpackPlugin({
        inject: "body",
        scriptLoading: "module",
      }),
    ],
    resolve: {
      extensions: [".js", ".jsx", ".ts", ".tsx"],
    },
  };

  return { webpackConfig };
}
