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
import { expect } from "@openremote/test";
import { type Alarm, AlarmSeverity, AlarmStatus } from "@openremote/model";
import { type Manager, adminStatePath, test } from "./fixtures/manager.js";

test.use({ storageState: adminStatePath });

/** Alarms seeded through the REST API, removed again after each test. */
const seededAlarmIds: number[] = [];

/** Distinguishes alarms seeded within the same millisecond, so their titles stay unique enough to look up by. */
let seedCount = 0;

/** Persist an alarm via REST and return its id and title, for table lookups and assertions. */
async function seedAlarm(
  manager: Manager,
  { realm = "master", status = AlarmStatus.OPEN, severity = AlarmSeverity.MEDIUM } = {}
) {
  const title = `E2E alarm ${Date.now()}-${++seedCount}`;
  const config = await manager.adminConfig();
  const alarm = (
    await manager.api.AlarmResource.createAlarm(
      {
        realm,
        title,
        content: "seeded by the alarms E2E test",
        severity,
        status,
      } as Alarm,
      {},
      config
    )
  ).data;

  expect(alarm.id).toBeTruthy();
  seededAlarmIds.push(alarm.id!);
  return { id: alarm.id!, title };
}

test.afterEach(async ({ manager }) => {
  if (seededAlarmIds.length > 0) {
    // tests that delete through the UI leave ids behind that are already gone, which is not a failure
    await manager.api.AlarmResource.removeAlarms(seededAlarmIds.splice(0), await manager.adminConfig()).catch((e) =>
      console.warn("Could not delete seeded alarm(s): ", e.response?.status ?? e.message)
    );
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

/**
 * The assertions below count table rows, so they run against "smartcity", which holds only the alarms seeded here
 * rather than the master realm's demo data.
 */
test.describe("Filter alarms", () => {
  // Keycloak sessions are per realm, so the stored admin state (master) cannot view "smartcity". Start clean and
  // log in as a user of that realm instead.
  test.use({ storageState: { cookies: [], origins: [] } });

  /**
   * One alarm per status, each with a distinct severity so the same set also exercises the severity filter.
   * OPEN, ACKNOWLEDGED and IN_PROGRESS make up the "All active" set; RESOLVED and CLOSED fall outside it.
   */
  async function seedOnePerStatus(manager: Manager) {
    return {
      open: await seedAlarm(manager, { realm: "smartcity", status: AlarmStatus.OPEN, severity: AlarmSeverity.LOW }),
      acknowledged: await seedAlarm(manager, {
        realm: "smartcity",
        status: AlarmStatus.ACKNOWLEDGED,
        severity: AlarmSeverity.MEDIUM,
      }),
      inProgress: await seedAlarm(manager, {
        realm: "smartcity",
        status: AlarmStatus.IN_PROGRESS,
        severity: AlarmSeverity.HIGH,
      }),
      resolved: await seedAlarm(manager, {
        realm: "smartcity",
        status: AlarmStatus.RESOLVED,
        severity: AlarmSeverity.MEDIUM,
      }),
      closed: await seedAlarm(manager, {
        realm: "smartcity",
        status: AlarmStatus.CLOSED,
        severity: AlarmSeverity.HIGH,
      }),
    };
  }

  async function loginAsAlarmUser(manager: Manager, username: string) {
    await manager.provisionUserAndLogin("smartcity", {
      username,
      roles: ["read:alarms", "write:alarms"],
    });
  }

  /**
   * @given One alarm of every status in "smartcity"
   * @when Each of the status filter's options is picked in turn
   * @then Only the alarms carrying that status are listed
   */
  test("should list only the alarms with the selected status", async ({ manager, alarmsPage }) => {
    const alarms = await seedOnePerStatus(manager);
    await loginAsAlarmUser(manager, "e2e-alarm-status-filterer");
    await alarmsPage.goto();

    // the overview opens on "All active", which leaves out the resolved and closed alarms
    await expect(alarmsPage.getRows()).toHaveCount(3);

    // picking a concrete status used to leave the filter unset, listing every alarm instead
    for (const [label, alarm] of [
      ["Open", alarms.open],
      ["Acknowledged", alarms.acknowledged],
      ["In Progress", alarms.inProgress],
      ["Resolved", alarms.resolved],
      ["Closed", alarms.closed],
    ] as const) {
      await alarmsPage.setStatusFilter(label);
      await expect(alarmsPage.getRows()).toHaveCount(1);
      await expect(alarmsPage.getRowByText(alarm.title)).toBeVisible();
    }

    await alarmsPage.setStatusFilter("All");
    await expect(alarmsPage.getRows()).toHaveCount(5);

    // and the default is reachable again from a concrete status
    await alarmsPage.setStatusFilter("All active");
    await expect(alarmsPage.getRows()).toHaveCount(3);
  });

  /**
   * @given One alarm of every status in "smartcity", spread across the three severities
   * @when Each of the severity filter's options is picked in turn, with the status filter widened to "All"
   * @then Only the alarms carrying that severity are listed
   */
  test("should list only the alarms with the selected severity", async ({ manager, alarmsPage }) => {
    const alarms = await seedOnePerStatus(manager);
    await loginAsAlarmUser(manager, "e2e-alarm-severity-filterer");
    await alarmsPage.goto();

    // widen the status first, so the counts below reflect the severity filter alone
    await alarmsPage.setStatusFilter("All");
    await expect(alarmsPage.getRows()).toHaveCount(5);

    await alarmsPage.setSeverityFilter("Low");
    await expect(alarmsPage.getRows()).toHaveCount(1);
    await expect(alarmsPage.getRowByText(alarms.open.title)).toBeVisible();

    await alarmsPage.setSeverityFilter("Medium");
    await expect(alarmsPage.getRows()).toHaveCount(2);
    await expect(alarmsPage.getRowByText(alarms.acknowledged.title)).toBeVisible();
    await expect(alarmsPage.getRowByText(alarms.resolved.title)).toBeVisible();

    await alarmsPage.setSeverityFilter("High");
    await expect(alarmsPage.getRows()).toHaveCount(2);
    await expect(alarmsPage.getRowByText(alarms.inProgress.title)).toBeVisible();
    await expect(alarmsPage.getRowByText(alarms.closed.title)).toBeVisible();

    await alarmsPage.setSeverityFilter("All");
    await expect(alarmsPage.getRows()).toHaveCount(5);
  });

  /**
   * @given One alarm of every status in "smartcity", spread across the three severities
   * @when Both the severity and the status filter are narrowed
   * @then Each filter is applied on top of the other rather than replacing it
   */
  test("should apply the severity and status filters together", async ({ manager, alarmsPage }) => {
    const alarms = await seedOnePerStatus(manager);
    await loginAsAlarmUser(manager, "e2e-alarm-combined-filterer");
    await alarmsPage.goto();

    // "All active" hides the closed alarm, leaving only the in-progress one at high severity
    await alarmsPage.setSeverityFilter("High");
    await expect(alarmsPage.getRows()).toHaveCount(1);
    await expect(alarmsPage.getRowByText(alarms.inProgress.title)).toBeVisible();

    // narrowing the status on top of it must keep the severity filter rather than replace it
    await alarmsPage.setStatusFilter("Closed");
    await expect(alarmsPage.getRows()).toHaveCount(1);
    await expect(alarmsPage.getRowByText(alarms.closed.title)).toBeVisible();

    // and widening the severity back out leaves the status filter in place
    await alarmsPage.setSeverityFilter("All");
    await expect(alarmsPage.getRows()).toHaveCount(1);
    await expect(alarmsPage.getRowByText(alarms.closed.title)).toBeVisible();
  });

  /**
   * @given Alarms in "smartcity", none of them assigned to the logged-in user
   * @when The "assigned to me" checkbox is ticked and unticked again
   * @then The list empties and is restored, on top of whichever status filter is set
   */
  test("should list only the alarms assigned to the current user", async ({ manager, alarmsPage }) => {
    await seedOnePerStatus(manager);
    await loginAsAlarmUser(manager, "e2e-alarm-assignee-filterer");
    await alarmsPage.goto();
    await expect(alarmsPage.getRows()).toHaveCount(3);

    // the seeded alarms have no assignee, so none of them belong to this user
    await alarmsPage.getAssignedToMeCheckbox().check();
    await expect(alarmsPage.getRows()).toHaveCount(0);

    // unticking reloads rather than leaving the list emptied
    await alarmsPage.getAssignedToMeCheckbox().uncheck();
    await expect(alarmsPage.getRows()).toHaveCount(3);

    // and it composes with the status filter instead of resetting it
    await alarmsPage.setStatusFilter("All");
    await alarmsPage.getAssignedToMeCheckbox().check();
    await expect(alarmsPage.getRows()).toHaveCount(0);
    await alarmsPage.getAssignedToMeCheckbox().uncheck();
    await expect(alarmsPage.getRows()).toHaveCount(5);
  });
});

/**
 * The assertions below count table rows, so they run against "smartcity", which holds only the alarms seeded here
 * rather than the master realm's demo data.
 */
test.describe("Delete alarms", () => {
  // Keycloak sessions are per realm, so the stored admin state (master) cannot view "smartcity". Start clean and
  // log in as a user of that realm instead.
  test.use({ storageState: { cookies: [], origins: [] } });

  /**
   * @given Three alarms in the "smartcity" realm, and a user of that realm who may write alarms
   * @when A single row is selected, and "select all" is then ticked on top of it
   * @then The confirmation counts each alarm once rather than counting the pre-selected row twice
   * @and Confirming removes all of them
   */
  test("should delete the alarms selected in the table", async ({ manager, alarmsPage }) => {
    // seeded over REST as the master admin, which is independent of the realm session used below
    const alarms = [
      await seedAlarm(manager, { realm: "smartcity" }),
      await seedAlarm(manager, { realm: "smartcity" }),
      await seedAlarm(manager, { realm: "smartcity" }),
    ];

    await manager.provisionUserAndLogin("smartcity", {
      username: "e2e-alarm-editor",
      roles: ["read:alarms", "write:alarms"],
    });
    await alarmsPage.goto();
    await expect(alarmsPage.getRows()).toHaveCount(alarms.length);

    // Selecting all emits a select event per row, including the row already selected here, which used to be
    // added to the selection a second time. More than one row is needed: selecting the only row would already
    // put the header checkbox in its checked state, making ticking it a no-op that emits nothing.
    await alarmsPage.getRowCheckbox(alarms[0].title).check();
    await alarmsPage.getSelectAllCheckbox().check();

    // the trash button only appears once something is selected
    await expect(alarmsPage.getDeleteSelectedButton()).toBeVisible();
    await alarmsPage.getDeleteSelectedButton().click();
    await expect(alarmsPage.getDeleteConfirmation(alarms.length)).toHaveCount(1);

    // confirming clears the table, and the alarms are gone from the backend rather than just the view
    await alarmsPage.getConfirmDeleteButton().click();
    await expect(alarmsPage.getRows()).toHaveCount(0);

    const config = await manager.adminConfig();
    const remaining = (await manager.api.AlarmResource.getAlarms({ realm: "smartcity" }, config)).data;
    expect(remaining).toEqual([]);
  });
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
