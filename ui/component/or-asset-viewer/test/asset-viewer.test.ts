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
import { OrAssetViewer } from "@openremote/or-asset-viewer";
import { validAsset, invalidAsset } from "./fixtures/data/asset";

ct.beforeEach(async ({ shared }) => {
  await shared.locales();
  await shared.fonts();
  await shared.registerAssets([validAsset, invalidAsset]);
});

// Due to how the component tests resolve imports, imported data with an object reference gets
// confused for a component that is meant to be registered in the playwright component test app.
// Which causes the data to be transformed to an intermediate object referencing the data.
//
// So we can use a "cloned" variable outside the test (but in the same test file) to avoid this.
const validId = validAsset.id;
ct("Should not show asset invalid error", async ({ mount }) => {
  const component = await mount(OrAssetViewer, {
    props: { assetId: validId, editMode: true },
  });
  await expect(component).not.toContainText("Asset is not valid");
});

// Due to how the component tests resolve imports, imported data with an object reference gets
// confused for a component that is meant to be registered in the playwright component test app.
// Which causes the data to be transformed to an intermediate object referencing the data.
//
// So we can use a "cloned" variable outside the test (but in the same test file) to avoid this.
const invalidId = invalidAsset.id;
ct("Should show asset invalid error", async ({ mount, assetViewer }) => {
  const component = await mount(OrAssetViewer, {
    props: { assetId: invalidId, editMode: true },
  });
  await assetViewer.getAttributeValueLocator("invalid").fill("0.1");
  await assetViewer.getAttributeValueLocator("invalid").press("Enter");
  await expect(assetViewer.getAttributeValueLocator("invalid")).toHaveAttribute("invalid");
});

ct("Should not show asset invalid error after switching assets", async ({ mount, assetViewer }) => {
  const component = await mount(OrAssetViewer, {
    props: { assetId: invalidId, editMode: true },
  });
  await assetViewer.getAttributeValueLocator("invalid").fill("0.1");
  await assetViewer.getAttributeValueLocator("invalid").press("Enter");
  await expect(assetViewer.getAttributeValueLocator("invalid")).toHaveAttribute("invalid");

  await component.update({ props: { assetId: validId, editMode: true } });
  await expect(component).not.toContainText("Asset is not valid");
});
