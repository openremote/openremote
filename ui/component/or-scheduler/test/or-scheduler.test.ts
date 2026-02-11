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
import { Locator } from "@openremote/test";
import { ct, expect } from "./fixtures";

import { OrScheduler, OrSchedulerChangedEvent } from "@openremote/or-scheduler";
import { MONTHS, BY_RRULE_PARTS, NOT_APPLICABLE_BY_RRULE_PARTS, WEEKDAYS } from "../src/util";
import type { VaadinInput } from "../../or-vaadin-components/test/fixtures";
import { Frequency } from "../src";

const slice = (m: string) => m.slice(0, 3);
const months = new RegExp(Object.values(MONTHS).map(slice).join("|"), "i");
const weekdays = new RegExp(Object.values(WEEKDAYS).map(slice).join("|"), "i");
const HOUR_IN_MILLIS = 3600 * 1000;
const DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

const FREQUENCIES = {
    YEARLY: "Year",
    MONTHLY: "Month",
    WEEKLY: "Week",
    DAILY: "Day",
    HOURLY: "Hour",
    MINUTELY: "Minute",
    SECONDLY: "Second",
};

function getDateInLocalMillis(): number {
    const now = Date.now();
    return now - (now % DAY_IN_MILLIS) + new Date().getTimezoneOffset() * 60000;
}

function getPeriodValues(time?: { start: number; end: number; date?: boolean }) {
    const startOfDay = getDateInLocalMillis();
    const end = time?.end ? startOfDay + time.end : startOfDay + DAY_IN_MILLIS - 1; // Defaults to end of day for all-day event
    const start = startOfDay + (time?.start ?? 0);
    const options: Intl.DateTimeFormatOptions = time
        ? time.date
            ? { dateStyle: "short", timeStyle: "short" }
            : { timeStyle: "short" }
        : { dateStyle: "short" };
    const startDate = new Date(start).toLocaleString("en-GB", options).replaceAll("/", "-").replaceAll(",", "");
    const endDate = new Date(end).toLocaleString("en-GB", options).replaceAll("/", "-").replaceAll(",", "");
    return { start, end, timeLabel: `from ${startDate} to ${endDate}` };
}

async function selectEventType(type: string, dialog: Locator, vaadinInput: VaadinInput) {
    await dialog.getByRole("button", { name: "Default", exact: true }).click();
    const option = vaadinInput.getSelectInputOption(type, dialog);
    await expect(option).toBeVisible();
    await option.click();
}

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
});

ct.describe("Default event type should", () => {
    ct("only show event types select", async ({ mount, vaadinDialog }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = vaadinDialog.getDialog();

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "Default", exact: true })).toBeVisible();

        await expect(dialog.locator("#period")).not.toBeVisible();

        await expect(dialog.locator("#recurrence")).not.toBeVisible();

        await expect(dialog.locator("#recurrence-ends")).not.toBeVisible();
    });

    ct("return the default value", async ({ mount, vaadinDialog, shared }) => {
        const expected = { start: 0, end: 0 };
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: {
                header: "Test Calendar Event Component",
                defaultSchedule: expected,
            },
            on: { "or-scheduler-changed": handler },
        });
        await component.click();
        await vaadinDialog.getDialog().getByRole("button", { name: "Save schedule" }).click();

        const actual = (await promise).value;
        expect(actual).toStrictEqual({ ...expected, recurrence: undefined });
        await expect(component.getByRole("button", { name: "Default" })).toHaveCount(1);
    });
});

ct.describe("Period event type should", () => {
    ct("show period inputs", async ({ mount, vaadinDialog, vaadinInput }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan an occurrence", dialog, vaadinInput);

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "Plan an occurrence", exact: true })).toBeVisible();

        await expect(dialog.locator("#period")).toBeVisible();
        await expect(dialog.locator("or-vaadin-time-picker").first()).toBeHidden();
        await expect(dialog.locator("or-vaadin-time-picker").last()).toBeHidden();
        await expect(dialog.locator("label", { hasText: "from" })).toBeVisible();
        await expect(dialog.locator("label", { hasText: "to" })).toBeVisible();
        await expect(dialog.getByRole("checkbox", { checked: true, name: "All day" })).toBeVisible();

        await expect(dialog.locator("#recurrence")).not.toBeVisible();

        await expect(dialog.locator("#recurrence-ends")).not.toBeVisible();
    });

    ct("return the default value", async ({ mount, shared, vaadinDialog, vaadinInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan an occurrence", dialog, vaadinInput);
        await dialog.getByRole("button", { name: "Save schedule" }).click();

        const actual = (await promise).value;
        const { start, end, timeLabel } = getPeriodValues();

        expect(actual).toStrictEqual({ end, start, recurrence: undefined });
        await expect(component.getByRole("button", { name: "Active " + timeLabel })).toBeVisible();
    });

    ct("return period with time component", async ({ mount, shared, vaadinDialog, vaadinInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan an occurrence", dialog, vaadinInput);

        await dialog.getByRole("checkbox", { name: "All day" }).uncheck();
        await expect(dialog.locator("or-vaadin-time-picker").first()).toBeVisible();
        await expect(dialog.locator("or-vaadin-time-picker").last()).toBeVisible();
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().fill("09:00");
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().press("Enter");
        await dialog.locator("or-vaadin-time-picker [slot=input]").last().fill("18:00");
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().press("Enter");
        await dialog.getByRole("button", { name: "Save schedule" }).click();

        const actual = (await promise).value;
        const { start, end, timeLabel } = getPeriodValues({
            start: 9 * HOUR_IN_MILLIS,
            end: 18 * HOUR_IN_MILLIS,
            date: true,
        });

        expect(actual).toStrictEqual({ end, start, recurrence: undefined });
        await expect(component.getByRole("button", { name: "Active " + timeLabel })).toBeVisible();
    });
});

ct.describe("Recurrence event type should", () => {
    ct("show recurrence inputs", async ({ mount, vaadinDialog, vaadinInput }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan a repeating occurrence", dialog, vaadinInput);

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "Plan a repeating occurrence", exact: true })).toBeVisible();

        await expect(dialog.locator("#period")).toBeVisible();
        await expect(dialog.locator("label", { hasText: "from" })).toBeVisible();
        await expect(dialog.locator("label", { hasText: "to" })).toBeVisible();
        await expect(dialog.getByRole("checkbox", { checked: true, name: "All day" })).toBeVisible();
        await expect(dialog.locator("or-vaadin-time-picker").first()).toBeHidden();
        await expect(dialog.locator("or-vaadin-time-picker").last()).toBeHidden();

        await expect(dialog.locator("#recurrence")).toBeVisible();
        await dialog.getByRole("button", { name: "Day" }).click();

        const weekDaysSelector = dialog
            .locator("or-vaadin-checkbox-group")
            .last()
            .getByRole("checkbox", { name: weekdays });
        const monthsSelector = dialog
            .locator("or-vaadin-checkbox-group")
            .first()
            .getByRole("checkbox", { name: months });

        for (const [freq, parts] of Object.entries(NOT_APPLICABLE_BY_RRULE_PARTS)) {
            if (freq === "SECONDLY") continue; // Intentionally skipped as its partially broken and unused
            await vaadinInput.getSelectInputOption(FREQUENCIES[freq as Frequency], dialog).click();
            for (const part of BY_RRULE_PARTS.filter((p) => !parts.includes(p.toUpperCase()))) {
                if (part === "byweekday") {
                    await expect(weekDaysSelector).toHaveCount(7);
                } else if (part === "bymonth") {
                    await expect(monthsSelector).toHaveCount(12);
                } else {
                    await expect(dialog.getByRole("combobox", { name: part })).toBeVisible();
                }
            }
            for (const part of parts) {
                if (part === "byweekday") {
                    await expect(weekDaysSelector).not.toBeVisible();
                } else if (part === "bymonth") {
                    await expect(monthsSelector).not.toBeVisible();
                } else {
                    await expect(dialog.getByRole("combobox", { name: part })).not.toBeVisible();
                }
            }
            await dialog
                .getByRole("button", { name: FREQUENCIES[freq as Frequency], exact: true })
                .click({ delay: 100 });
        }

        const ends = dialog.locator("#recurrence-ends");
        await expect(ends).toBeVisible();
        await expect(ends.getByRole("radio", { name: "Never" })).toBeChecked();
        await expect(ends.getByRole("radio", { name: "On" })).not.toBeChecked();
        await expect(ends.getByRole("radio", { name: "After" })).not.toBeChecked();
        await expect(ends.locator("or-vaadin-date-time-picker")).toBeDisabled();
        await expect(ends.getByRole("spinbutton")).toBeDisabled();
    });

    ct("return the default value", async ({ mount, shared, vaadinDialog, vaadinInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan a repeating occurrence", dialog, vaadinInput);

        await dialog.getByRole("button", { name: "Save schedule" }).click();

        const actual = (await promise).value;
        const { start, end } = getPeriodValues();

        expect(actual).toStrictEqual({ end, start, recurrence: "FREQ=DAILY" });
        await expect(component.getByRole("button", { name: "every day" })).toBeVisible();
    });

    ct("return recurrence with time components", async ({ mount, shared, vaadinDialog, vaadinInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = vaadinDialog.getDialog();
        await selectEventType("Plan a repeating occurrence", dialog, vaadinInput);

        await dialog.getByRole("checkbox", { name: "All day" }).uncheck();
        await expect(dialog.locator("or-vaadin-time-picker").first()).toBeVisible();
        await expect(dialog.locator("or-vaadin-time-picker").last()).toBeVisible();
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().fill("09:00");
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().press("Enter");
        await dialog.locator("or-vaadin-time-picker [slot=input]").last().fill("18:00");
        await dialog.locator("or-vaadin-time-picker [slot=input]").first().press("Enter");
        await dialog.getByRole("button", { name: "Save schedule" }).click();

        const actual = (await promise).value;
        const { start, end, timeLabel } = getPeriodValues({ start: 9 * HOUR_IN_MILLIS, end: 18 * HOUR_IN_MILLIS });

        expect(actual).toStrictEqual({ end, start, recurrence: "FREQ=DAILY" });
        await expect(component.getByRole("button", { name: "every day " + timeLabel })).toBeVisible();
    });
});
