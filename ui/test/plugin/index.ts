import fs from "node:fs";
import path from "path";
import rspack, { Configuration } from "@rspack/core";
import { RspackDevServer } from "@rspack/dev-server";
import HtmlWebpackPlugin from "html-webpack-plugin";

// @ts-expect-error No declaration file available
import { getStandardModuleRules } from "@openremote/util";
import { isURLAvailable } from "playwright-core/lib/utils";

import type { FullConfig } from "playwright/types/testReporter";

import {
  resolveDirs,
  resolveEndpoint,
  populateComponentsFromTests,
  ImportInfo,
  transformIndexFile,
  resolveCtConfig,
} from "./rspackUtils";

// Copied Playwright module (not exported in the playwright-core/package.json)
const injectedSource = require.resolve("./injectedScriptSource");

let devServer: RspackDevServer;

/**
 * Create a plugin for Playwright component testing with Rspack.
 *
 * The plugin expects an index.html file to be present. The directory of the index.html file
 * can be set through `ctTemplateDir` (defaults to "playwright") in the playwright configuration file.
 * The index.html is used to serve the component to the browser to run the component tests against.
 *
 * Each component is mounted using the `mount` fixture available through `playwright-ct-core`. The plugin
 * starts a Rspack development server that bundles the code to register and mount components to the
 * document. The bundle will reference the component source code through the provided import alias.
 *
 * Note: You must use an import alias (like `@openremote/or-icon`) to import a component in a test file,
 * because the component code is expected to be transpiled when referenced in the bundle.
 */
export function createPlugin() {
  let config: FullConfig;
  let configDir: string;

  return {
    name: "playwright-rspack-plugin",

    setup: async (cfg: FullConfig, cfgDir: string) => {
      config = cfg;
      configDir = cfgDir;
    },

    begin: async () => {
      const rspackConfig = await buildBundle(config, configDir);
      if (!rspackConfig) return;

      devServer = new RspackDevServer(rspackConfig.devServer!, rspack(rspackConfig));
      await devServer.start();

      if (!devServer.server) return;

      const address = devServer.server.address();
      if (address && typeof address === "object") {
        process.env.PLAYWRIGHT_TEST_BASE_URL = `http://${address.address}:${address.port}`;
      }
    },

    end: async () => {
      if (devServer) await devServer.stop();
    },

    populateDependencies: async () => {
      await buildBundle(config, configDir);
    },

    clearCache: async () => {
      const use = resolveCtConfig(config);
      if (!use) return;
      const dirs = await resolveDirs(configDir, use);
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

/**
 * Writes required component registration source to the bundle cache directory and returns
 * a corresponding Rspack configuration.
 *
 * Note: The Playwright configuration must specify the name of the project under `projects.use.ct`.
 * Otherwise the configuration will be ignored.
 *
 * @param ctConfig The full Playwright component testing config
 * @param configDir The directory of the component project
 *
 * @returns The Rspack configuration used to bundle and serve a component on the specified address.
 */
async function buildBundle(ctConfig: FullConfig, configDir: string): Promise<Configuration | null> {
  const use = resolveCtConfig(ctConfig);
  if (!use) {
    console.log(`Could not resolve component test configuration.`);
    return null;
  }

  const name = use.ct;
  const endpoint = resolveEndpoint(use);

  const protocol = endpoint.https ? "https:" : "http:";
  const url = new URL(`${protocol}//${endpoint.host}:${endpoint.port}`);
  if (await isURLAvailable(url, true)) {
    console.log(`Dev Server already running at ${url.toString()}`);
    process.env.PLAYWRIGHT_TEST_BASE_URL = url.toString();
    return null;
  }

  const dirs = await resolveDirs(configDir, use);
  if (!dirs) {
    console.log(`Template file playwright/index.html is missing.`);
    return null;
  }

  const componentRegistry: Map<string, ImportInfo> = new Map();
  const componentsByImportingFile = new Map<string, string[]>();
  // Populate component registry based on the tests' component imports.
  await populateComponentsFromTests(componentRegistry, componentsByImportingFile);

  const { registerSourceFile } = (ctConfig as any)["@playwright/experimental-ct-core"];

  // Consider including buildInfo in the cache dir to be able to know how to invalidate the cache dir
  const registerSource = fs.readFileSync(injectedSource, "utf-8") + "\n" + fs.readFileSync(registerSourceFile, "utf-8");
  const indexSourcePath = path.join(dirs.templateDir, "index.js");
  const transformedIndex = transformIndexFile(
    fs.readFileSync(indexSourcePath, "utf-8"),
    registerSource,
    componentRegistry
  );
  const outputIndexPath = path.join(dirs.outDir, name + "-index.js");
  fs.mkdirSync(dirs.outDir, { recursive: true });
  fs.writeFileSync(outputIndexPath, transformedIndex);

  const config: Configuration = {
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
      // Force ipv4
      host: "127.0.0.1",
      port: 0,
    },
    module: getStandardModuleRules(),
    performance: {
      hints: false,
    },
    plugins: [
      new HtmlWebpackPlugin({
        inject: "body",
        scriptLoading: "module",
      }),
    ],
    resolve: {
      extensions: [".js", ".jsx", ".ts", ".tsx"],
    },
  };

  return config;
}
