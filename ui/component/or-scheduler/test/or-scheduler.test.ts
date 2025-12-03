import { Locator } from "@openremote/test";
import { ct, expect } from "./fixtures";

import { OrScheduler, OrSchedulerChangedEvent } from "@openremote/or-scheduler";
import { MONTHS, BY_RRULE_PARTS, NOT_APPLICABLE_BY_RRULE_PARTS } from "../src/data";
import type { MwcInput } from "../../or-mwc-components/test/fixtures";

const months = new RegExp(Object.values(MONTHS).join("|"));

async function selectEventType(type: string, dialog: Locator, mwcInput: MwcInput) {
    await dialog.getByRole("button", { name: "default", exact: true }).click();
    const option = mwcInput.getSelectInputOption(type, dialog);
    await expect(option).toBeVisible();
    await option.click();
}

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
});

ct.describe("Default event type should", () => {
    ct("only show event types select", async ({ mount, mwcDialog }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = mwcDialog.getDialog();

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "default", exact: true })).toBeVisible();

        await expect(dialog.locator("#recurrence")).not.toBeVisible();

        await expect(dialog.locator("#period")).not.toBeVisible();

        await expect(dialog.locator("#recurrence-ends")).not.toBeVisible();
    });

    ct("return the default value", async ({ mount, mwcDialog, shared }) => {
        const expected = { start: 0, end: 0 };
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: {
                header: "Test Calendar Event Component",
                default: expected,
            },
            on: { "or-scheduler-changed": handler },
        });
        await component.click();
        await mwcDialog.getDialog().getByRole("button", { name: "apply" }).click();

        const actual = await promise;
        expect(actual).toStrictEqual(expected);
        await expect(component.getByRole("button")).toContainText("Default");
    });
});

ct.describe("Period event type should", () => {
    ct("show period inputs", async ({ mount, mwcDialog, mwcInput }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = mwcDialog.getDialog();
        await selectEventType("period", dialog, mwcInput);

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "period", exact: true })).toBeVisible();

        await expect(dialog.locator("#recurrence")).not.toBeVisible();

        await expect(dialog.locator("#period")).toBeVisible();
        await expect(dialog.locator("label", { hasText: "from" })).toHaveCount(2);
        await expect(dialog.locator("label", { hasText: "from" }).last()).toBeDisabled();
        await expect(dialog.locator("label", { hasText: "to" })).toHaveCount(2);
        await expect(dialog.locator("label", { hasText: "to" }).last()).toBeDisabled();
        await expect(dialog.getByRole("checkbox", { checked: true, name: "All day" })).toBeVisible();

        await expect(dialog.locator("#recurrence-ends")).not.toBeVisible();
    });

    ct("return the default value", async ({ mount, shared, mwcDialog, mwcInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = mwcDialog.getDialog();
        await selectEventType("period", dialog, mwcInput);
        await dialog.getByRole("button", { name: "apply" }).click();

        const actual = await promise;
        expect(actual).toStrictEqual({ end: 1764889199999, start: 1764802800000 });
        await expect(component.getByRole("button")).toContainText("Active from 04-12-2025 to 04-12-2025");
    });

    ct("return period with time component", async ({ mount, shared, mwcDialog, mwcInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = mwcDialog.getDialog();
        await selectEventType("period", dialog, mwcInput);

        await dialog.getByRole("checkbox", { name: "All day" }).uncheck();
        await expect(dialog.locator("label", { hasText: "from" }).last()).toBeEnabled();
        await dialog.locator("label", { hasText: "from" }).last().fill("09:00");
        await expect(dialog.locator("label", { hasText: "to" }).last()).toBeEnabled();
        await dialog.locator("label", { hasText: "to" }).last().fill("18:00");

        await dialog.getByRole("button", { name: "apply" }).click();

        const actual = await promise;
        expect(actual).toStrictEqual({ end: 1764867600000, start: 1764835200000, recurrence: undefined });
        await expect(component.getByRole("button")).toContainText("Active from 04-12-2025 09:00 to 04-12-2025 18:00");
    });
});

ct.describe("Recurrence event type should", () => {
    ct("show recurrence inputs", async ({ page, mount, mwcDialog, mwcInput }) => {
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
        });
        await component.click();
        const dialog = mwcDialog.getDialog();
        await selectEventType("recurrence", dialog, mwcInput);

        await expect(dialog.locator("#event-type")).toBeVisible();
        await expect(dialog.getByRole("button", { name: "recurrence", exact: true })).toBeVisible();

        await expect(dialog.locator("#recurrence")).toBeVisible();
        await dialog.getByRole("button", { name: "DAILY" }).click();
        for (const [freq, parts] of Object.entries(NOT_APPLICABLE_BY_RRULE_PARTS)) {
            await mwcInput.getSelectInputOption(freq, dialog).click();
            for (const part of BY_RRULE_PARTS.filter((p) => !parts.includes(p.toUpperCase()))) {
                if (part === "byweekday") {
                    await expect(
                        dialog.locator("or-mwc-input > .mdc-checkbox-list label", { hasText: /MO|TU|WE|TH|FR|SA|SU/ })
                    ).toHaveCount(7);
                } else if (part === "bymonth") {
                    await expect(
                        dialog.locator("or-mwc-input > .mdc-checkbox-list label", { hasText: months })
                    ).toHaveCount(12);
                } else {
                    await expect(dialog.getByRole("button", { name: part })).toBeVisible();
                }
            }
            for (const part of parts) {
                if (part === "byweekday") {
                    await expect(
                        dialog.locator("or-mwc-input > .mdc-checkbox-list label", { hasText: /MO|TU|WE|TH|FR|SA|SU/ })
                    ).not.toBeVisible();
                } else if (part === "bymonth") {
                    await expect(
                        dialog.locator("or-mwc-input > .mdc-checkbox-list label", { hasText: months })
                    ).not.toBeVisible();
                } else {
                    await expect(dialog.getByRole("button", { name: part })).not.toBeVisible();
                }
            }
            await dialog.getByRole("button", { name: freq }).click();
            await page.waitForTimeout(100);
        }

        await expect(dialog.locator("#period")).toBeVisible();
        await expect(dialog.locator("label", { hasText: "from" })).toHaveCount(2);
        await expect(dialog.locator("label", { hasText: "from" }).last()).toBeDisabled();
        await expect(dialog.locator("label", { hasText: "to" })).toHaveCount(2);
        await expect(dialog.locator("label", { hasText: "to" }).last()).toBeDisabled();
        await expect(dialog.getByRole("checkbox", { checked: true, name: "All day" })).toBeVisible();

        await expect(dialog.locator("#recurrence-ends")).toBeVisible();
    });

    ct("return the default value", async ({ mount, shared, mwcDialog, mwcInput }) => {
        const [promise, handler] = shared.promiseEventDispatch<OrSchedulerChangedEvent>();
        const component = await mount(OrScheduler, {
            props: { header: "Test Calendar Event Component" },
            on: { "or-scheduler-changed": handler },
        });

        await component.click();
        const dialog = mwcDialog.getDialog();
        await selectEventType("recurrence", dialog, mwcInput);

        await dialog.getByRole("button", { name: "apply" }).click();

        const actual = await promise;
        expect(actual).toStrictEqual({ end: 1764889199999, start: 1764802800000, recurrence: "FREQ=DAILY" });
        await expect(component.getByRole("button")).toContainText("every day");
    });
});
