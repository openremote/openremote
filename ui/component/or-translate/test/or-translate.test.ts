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

ct.beforeEach(async ({ shared }) => {
  await shared.locales({
    en: {
      test: {
        thing: "Thing",
        "internet.of": { things: "Internet of things" },
      },
    },
    nl: {
      test: {
        thing: "Ding",
        "internet.of": { things: "Internet van dingen" },
      },
    },
  });
});

ct("Should translate text", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "thing",
      options: { ns: "test", lng: "en" },
    },
  });
  await expect(component).toContainText("Thing");
  await component.update({
    props: {
      value: "thing",
      options: { ns: "test", lng: "nl" },
    },
  });
  await expect(component).toContainText("Ding");
});

ct("Should allow mixed key paths", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "internet.of.things",
      options: { ns: "test", lng: "en" },
    },
  });
  await expect(component).toContainText("Internet of things");
  await component.update({
    props: {
      value: "internet.of.things",
      options: { ns: "test", lng: "nl" },
    },
  });
  await expect(component).toContainText("Internet van dingen");
});
