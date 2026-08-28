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
import { ct } from "./fixtures";
import { expect } from "@openremote/test";

import { OrCollapsiblePanel } from "@openremote/or-components/or-collapsible-panel";

ct.beforeEach(async ({ shared }) => {
  await shared.fonts();
});

ct("Should append header and content to collapsible panel", async ({ mount, collapsiblePanel }) => {
  const component = await mount(OrCollapsiblePanel, {
    props: {},
    slots: {
      header: "<div>Header</div>", // slot="" is optional
      content: "<div>Content</div>",
    },
  });

  const header = collapsiblePanel.getHeader(component);
  await header.click();
  await expect(header).toContainText("Header");

  const content = collapsiblePanel.getContent(component);
  await expect(content).toContainText("Content");
});
