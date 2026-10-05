/*
 * Copyright 2026, OpenRemote Inc.
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
import { ct, expect } from "@openremote/test";

import { OrTranslate } from "@openremote/or-translate";

import { localisedDates } from "./fixtures/data/localised-dates";

// Built in local time so the rendered clock is the same in every time zone
const DATE = new Date(2026, 11, 15, 13, 45);

ct.beforeEach(async ({ shared }) => {
  // Only the date is interpolated, so the language decides the formatting and nothing else
  await shared.locales(
    Object.fromEntries(localisedDates.map(({ language }) => [language, { test: { date: "{{-date, lll}}" } }]))
  );
});

for (const { language, expected } of localisedDates) {
  ct(`should format an interpolated date in ${language}`, async ({ mount }) => {
    const date = DATE;
    const component = await mount(OrTranslate, {
      props: { value: "date", options: { ns: "test", lng: language, date } },
    });

    // Moment reaches its locale data through an aliased require. Where the bundler does not follow
    // that, the data is absent, Moment falls back to `en` and every language renders the English
    // date while the surrounding text stays translated.
    await expect(component).toHaveText(expected);
  });
}
