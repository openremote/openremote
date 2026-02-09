/*
 * Copyright 2025, OpenRemote Inc.
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
import { MwcDialog, MwcInput } from "../../../or-mwc-components/test/fixtures";
import { VaadinDialog, VaadinInput } from "../../../or-vaadin-components/test/fixtures";
import { ct as base, type SharedComponentTestFixtures, withPage } from "@openremote/test";
export { expect } from "@openremote/test";

interface ComponentFixtures extends SharedComponentTestFixtures {
    mwcDialog: MwcDialog;
    mwcInput: MwcInput;
    vaadinDialog: VaadinDialog;
    vaadinInput: VaadinInput;
}

export const ct = base.extend<ComponentFixtures>({
    // Components
    mwcDialog: withPage(MwcDialog),
    mwcInput: withPage(MwcInput),
    vaadinDialog: withPage(VaadinDialog),
    vaadinInput: withPage(VaadinInput),
});
