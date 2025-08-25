import fs from "node:fs";
import path from "node:path";

import { getUserData } from "playwright/lib/transform/compilationCache";
import { resolveHook } from "playwright/lib/transform/transform";

import type { PlaywrightTestConfig as BasePlaywrightTestConfig } from "playwright/types/test";
import type { FullConfig } from "playwright/types/testReporter";
import type { Configuration as RspackConfig } from "@rspack/core";

export type CtConfig = BasePlaywrightTestConfig["use"] & {
  ct?: boolean;
  ctPort?: number;
  ctTemplateDir?: string;
  ctCacheDir?: string;
  ctRspackConfig?: RspackConfig | (() => Promise<RspackConfig>);
};

/**
 * Resolves the first project that contains a `project.use.ct`.
 * @param config The full Playwright configuration
 * @returns The projects' `UseOptions`
 */
export function resolveCtConfig(config: FullConfig): CtConfig | undefined {
  return config.projects.find((project) => (project.use as CtConfig).ct)?.use;
}

export async function resolveDirs(configDir: string, use: CtConfig) {
  const templateDir = await fs.promises
    .realpath(use.ctTemplateDir || path.join(configDir, "playwright"))
    .catch(() => undefined);
  if (!templateDir) return null;

  const outDir = use.ctCacheDir ? path.resolve(configDir, use.ctCacheDir) : path.resolve(templateDir, ".cache");

  return {
    configDir,
    outDir,
    templateDir,
  };
}

export function resolveEndpoint(use: CtConfig) {
  const baseURL = new URL(use.baseURL || "http://localhost");
  return {
    https: baseURL.protocol.startsWith("https:"),
    host: baseURL.hostname,
    port: use.ctPort || Number(baseURL.port) || 3100,
  };
}

export type ImportInfo = {
  id: string;
  filename: string;
  importSource: string;
  remoteName: string | undefined;
};
export type ComponentRegistry = Map<string, ImportInfo>;

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
