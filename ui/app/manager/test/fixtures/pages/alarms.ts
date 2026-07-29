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
import { BasePage, Locator, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager.js";

/**
 * Page object for the alarms page (page-alarms).
 *
 * The overview is an `or-alarms-table`; clicking a row opens the single-alarm view, whose properties column
 * (`#prop-panel`) holds the severity, status and assignee `or-vaadin-select`s. Those carry no id, so they are
 * located by the translation key of their slotted label. Opening one shows its items in a
 * `vaadin-select-list-box`.
 */
export class AlarmsPage implements BasePage {
    constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

    async goto() {
        // alarms are reached through the bell button in the header, not the drawer menu
        await this.page.getByTitle("Alarms", { exact: true }).click();
        await expect(this.getTable()).toBeVisible();
    }

    // --- Overview table ----------------------------------------------------

    getTable(): Locator {
        return this.page.locator("or-alarms-table");
    }

    getRows(): Locator {
        return this.getTable().locator("tbody tr");
    }

    /** A table row containing the given text (e.g. an alarm's title). */
    getRowByText(text: string): Locator {
        return this.getRows().filter({ hasText: text });
    }

    getAddButton(): Locator {
        return this.page.getByRole("button", { name: "Add Alarm" });
    }

    // --- Single alarm view -------------------------------------------------

    /** The properties column of the single-alarm view, holding the severity/status/assignee selects. */
    getPropertiesPanel(): Locator {
        return this.page.locator("#prop-panel");
    }

    /** A properties-column select, located by the translation key of its slotted label. */
    private getPropertySelect(labelKey: string): Locator {
        return this.getPropertiesPanel()
            .locator("or-vaadin-select")
            .filter({ has: this.page.locator(`or-translate[value="${labelKey}"]`) });
    }

    getStatusSelect(): Locator {
        return this.getPropertySelect("alarm.status");
    }

    getSeveritySelect(): Locator {
        return this.getPropertySelect("alarm.severity");
    }

    getSaveButton(): Locator {
        return this.page.locator("#savebtn");
    }

    /** Open the single-alarm view of the (first) row containing the given text. */
    async openAlarmByText(text: string) {
        await this.getRowByText(text).first().click();
        await expect(this.getPropertiesPanel()).toBeVisible();
    }

    /** Pick a status (e.g. "In Progress") in the single-alarm view. */
    async setStatus(label: string) {
        await this.getStatusSelect().click();
        await this.pickOverlayOption(label);
    }

    /**
     * Click the option with the given exact label in the currently-open Vaadin select overlay.
     *
     * Vaadin slots the items into a `vaadin-select-list-box` that it projects into an overlay, so the options live
     * under the list box rather than the select. Only the open one is visible; scoping the lookup to it (and
     * waiting for it to open and close) avoids clicking an item that is still repositioning under the trigger.
     */
    private async pickOverlayOption(label: string) {
        const listBox = this.page.locator("vaadin-select-list-box:visible");
        await expect(listBox).toBeVisible();
        await listBox.getByRole("option", { name: label, exact: true }).click();
        await expect(listBox).not.toBeVisible();
    }
}
