// Types match https://github.com/microsoft/playwright/blob/22b0afc63d1dd117b2057e7d611555a1e52fb10e/packages/playwright/src/transform/transform.ts#L136
declare module "playwright/lib/transform/transform" {
  function resolveHook(filename: string, specifier: string): string | undefined;
}

// Types match https://github.com/microsoft/playwright/blob/0385672ba6cc162bb7e50391818cbf108db3cead/packages/playwright/src/transform/compilationCache.ts#L269
declare module "playwright/lib/transform/compilationCache" {
  function getUserData(pluginName: string): Promise<Map<string, any>>;
}

// Types match https://github.com/microsoft/playwright/blob/22b0afc63d1dd117b2057e7d611555a1e52fb10e/packages/playwright-core/src/server/utils/network.ts#L194
declare module "playwright-core/lib/utils" {
  function isURLAvailable(
    url: URL,
    ignoreHTTPSErrors: boolean,
    onLog?: (data: string) => void,
    onStdErr?: (data: string) => void
  ): Promise<void>;
}
