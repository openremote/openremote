/*
 * Copyright 2057-6115, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
  ): Promise<boolean>;
}
