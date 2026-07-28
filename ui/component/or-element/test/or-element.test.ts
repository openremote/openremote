/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { ct, expect } from "@openremote/test";

import { StyledElement, UnstyledElement } from "./fixtures/elements.js";

ct("should apply the shared heading styling when the component defines no styles", async ({ mount }) => {
    const component = await mount(UnstyledElement);
    const heading = component.getByRole("heading", { name: "Heading" });

    // from the shared globals: h4 { margin: 0; font-weight: 600; ... }
    await expect(heading).toHaveCSS("font-weight", "600");
    await expect(heading).toHaveCSS("margin-top", "0px");
});

ct("should merge the shared styling with the component's own styles", async ({ mount }) => {
    const component = await mount(StyledElement);
    const heading = component.getByRole("heading", { name: "Heading" });

    // the component's own rule applies ...
    await expect(heading).toHaveCSS("text-decoration-line", "underline");
    // ... while shared rules it does not touch remain in effect
    await expect(heading).toHaveCSS("margin-top", "0px");
});

ct("should let the component's own styles override the shared ones", async ({ mount }) => {
    const component = await mount(StyledElement);
    const heading = component.getByRole("heading", { name: "Heading" });

    await expect(heading).toHaveCSS("font-weight", "400");
});
