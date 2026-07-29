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
import { expect } from "@openremote/test";
import { Alarm, AlarmSeverity, AlarmStatus } from "@openremote/model";
import { Manager, adminStatePath, test } from "./fixtures/manager.js";

test.use({ storageState: adminStatePath });

/** Alarms seeded through the REST API, removed again after each test. */
const seededAlarmIds: number[] = [];

/** Persist an alarm via REST and return its id and title, for table lookups and assertions. */
async function seedAlarm(
    manager: Manager,
    { realm = "master", status = AlarmStatus.OPEN, severity = AlarmSeverity.MEDIUM } = {}
) {
    const title = `E2E alarm ${Date.now()}`;
    const config = await manager.adminConfig();
    const alarm = (await manager.api.AlarmResource.createAlarm({
        realm,
        title,
        content: "seeded by the alarms E2E test",
        severity,
        status,
    } as Alarm, {}, config)).data;

    expect(alarm.id).toBeTruthy();
    seededAlarmIds.push(alarm.id!);
    return { id: alarm.id!, title };
}

test.afterEach(async ({ manager }) => {
    if (seededAlarmIds.length > 0) {
        await manager.api.AlarmResource.removeAlarms(seededAlarmIds.splice(0), await manager.adminConfig());
    }
    await manager.cleanUp();
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and An open alarm seeded via REST
 * @when The alarm is opened and its status is changed to "In Progress" and saved
 * @then The overview row shows the new status, and the change is persisted on the alarm itself
 */
test("should change the status of an existing alarm", async ({ manager, alarmsPage }) => {
    const { id, title } = await seedAlarm(manager);

    await manager.goToRealmStartPage("master");
    await alarmsPage.goto();
    await alarmsPage.openAlarmByText(title);

    // the seeded status is shown, and saving is only offered once something changes
    await expect(alarmsPage.getStatusSelect()).toContainText("Open");
    await expect(alarmsPage.getSaveButton()).toBeDisabled();

    await alarmsPage.setStatus("In Progress");
    await expect(alarmsPage.getStatusSelect()).toContainText("In Progress");
    await expect(alarmsPage.getSaveButton()).toBeEnabled();

    // saving returns to the overview, where the row reflects the new status ...
    await alarmsPage.getSaveButton().click();
    await expect(alarmsPage.getTable()).toBeVisible();
    await expect(alarmsPage.getRowByText(title)).toContainText("In Progress");

    // ... and the alarm itself was updated, not just the table
    const stored = (await manager.api.AlarmResource.getAlarm(id, await manager.adminConfig())).data;
    expect(stored.status).toBe(AlarmStatus.IN_PROGRESS);
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and An open alarm seeded via REST
 * @when The alarm is resolved through the single-alarm view
 * @then It drops out of the overview, which defaults to showing active alarms only
 */
test("should drop a resolved alarm from the default active filter", async ({ manager, alarmsPage }) => {
    const { title } = await seedAlarm(manager);

    await manager.goToRealmStartPage("master");
    await alarmsPage.goto();
    await expect(alarmsPage.getRowByText(title)).toBeVisible();

    await alarmsPage.openAlarmByText(title);
    await alarmsPage.setStatus("Resolved");
    await alarmsPage.getSaveButton().click();

    await expect(alarmsPage.getTable()).toBeVisible();
    await expect(alarmsPage.getRowByText(title)).toHaveCount(0);
});

test.describe("Role-Based Access Control", () => {
    // Start from a clean session so we can log in as the low-privilege user rather than a stored admin state.
    test.use({ storageState: { cookies: [], origins: [] } });

    /**
     * @given An alarm seeded into "smartcity" and a "smartcity" user with read:alarms only
     * @when That user opens the alarm
     * @then The status is shown but cannot be changed, and no alarm can be added
     */
    test("should show the status read-only for a user without write permission", async ({ manager, alarmsPage }) => {
        const { title } = await seedAlarm(manager, { realm: "smartcity" });
        await manager.provisionUserAndLogin("smartcity", {
            username: "e2e-alarm-viewer",
            roles: ["read:alarms"],
        });

        await alarmsPage.goto();
        await expect(alarmsPage.getAddButton()).not.toBeVisible();

        await alarmsPage.openAlarmByText(title);
        await expect(alarmsPage.getStatusSelect()).toContainText("Open");
        await expect(alarmsPage.getStatusSelect()).toHaveAttribute("readonly", "");
    });
});
