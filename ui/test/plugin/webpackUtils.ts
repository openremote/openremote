import fs from "node:fs";
import path from "node:path";

// @ts-expect-error No declaration file available (internal Playwright module)
import { getUserData } from "playwright/lib/transform/compilationCache";
// @ts-expect-error No declaration file available (internal Playwright module)
import { resolveHook } from "playwright/lib/transform/transform";

import type { PlaywrightTestConfig as BasePlaywrightTestConfig } from "playwright/types/test";
import type { FullConfig } from "playwright/types/testReporter";
import type webpack from "webpack";
import type { Configuration as WebpackConfig } from "webpack";

export type CtConfig = BasePlaywrightTestConfig["use"] & {
  ct?: boolean;
  ctPort?: number;
  ctTemplateDir?: string;
  ctCacheDir?: string;
  ctWebpackConfig?: WebpackConfig | (() => Promise<WebpackConfig>);
};

export type ImportInfo = {
  id: string;
  filename: string;
  importSource: string;
  remoteName: string | undefined;
};
export type ComponentRegistry = Map<string, ImportInfo>;
export type ComponentDirs = {
  configDir: string;
  outDir: string;
  templateDir: string;
};

function resolveCtConfig(config: FullConfig): CtConfig | undefined {
  return config.projects.find((project) => (project.use as CtConfig).ct)?.use;
}

export async function resolveDirs(configDir: string, config: FullConfig): Promise<ComponentDirs | null> {
  const use = resolveCtConfig(config);
  if (!use) return null;
  const relativeTemplateDir = use.ctTemplateDir || "playwright";
  const templateDir = await fs.promises.realpath(path.join(configDir, relativeTemplateDir)).catch(() => undefined);
  if (!templateDir) return null;

  const outDir = use.ctCacheDir ? path.resolve(configDir, use.ctCacheDir) : path.resolve(templateDir, ".cache");

  return {
    configDir,
    outDir,
    templateDir,
  };
}

export function resolveEndpoint(config: FullConfig) {
  const use = resolveCtConfig(config);
  if (!use) return null;
  const baseURL = new URL(use.baseURL || "http://localhost");
  return {
    https: baseURL.protocol.startsWith("https:"),
    host: baseURL.hostname,
    port: use.ctPort || Number(baseURL.port) || 3100,
  };
}

export async function populateComponentsFromTests(
  componentRegistry: ComponentRegistry,
  componentsByImportingFile?: Map<string, string[]>
) {
  const importInfos: Map<string, ImportInfo[]> = await getUserData("playwright-ct-core");
  for (const [file, importList] of importInfos) {
    for (const importInfo of importList) {
      componentRegistry.set(importInfo.id, importInfo);
    }
    if (componentsByImportingFile) {
      componentsByImportingFile.set(
        file,
        importList.map((i) => resolveHook(i.filename, i.importSource)).filter(Boolean) as string[]
      );
    }
  }
}

export function transformIndexFile(
  content: string,
  registerSource: string,
  importInfos: Map<string, ImportInfo>
): string {
  const lines = [content, "", registerSource];

  for (const value of importInfos.values()) {
    const importPath = resolveHook(value.filename, value.importSource) || value.importSource;
    lines.push(
      `const ${value.id} = () => import('${importPath?.replaceAll(path.sep, "/")}').then(mod => mod.${
        value.remoteName || "default"
      });`
    );
  }

  lines.push(`__pwRegistry.initialize({ ${[...importInfos.keys()].join(",\n  ")} });`);

  return lines.join("\n");
}

export function frameworkConfig(config: FullConfig): {
  registerSourceFile: string;
  frameworkPluginFactory?: () => Promise<webpack.WebpackPluginInstance>;
} {
  return (config as any)["@playwright/experimental-ct-core"];
}
