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
import { expect, Page } from "@openremote/test";
import { Asset, EmailNotificationMessage, Notification, NotificationTargetType } from "@openremote/model";
import { type AxiosRequestConfig } from "axios";
import { Manager, adminStatePath, test } from "./fixtures/manager.js";

test.use({ storageState: adminStatePath });

test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});

const RECIPIENT_USERNAME = "e2e-notif-recipient";

/** Ensure a user with an email exists in the given realm, so email notifications resolve to a deliverable target. */
async function ensureRecipient(manager: Manager, realm = "master", config?: AxiosRequestConfig) {
    await manager.provisionUser(realm, {
        username: RECIPIENT_USERNAME,
        email: `${RECIPIENT_USERNAME}@openremote.local`,
    }, config);
}

/** Persist a notification via REST (email to the given realm) and return its subject for table lookups. */
async function seedNotification(manager: Manager, realm = "master", subject = `E2E seed ${Date.now()}`) {
    const config = await manager.adminConfig();
    await ensureRecipient(manager, realm, config);
    await manager.api.NotificationResource.sendNotification({
        name: subject,
        message: { type: "email", subject, html: "<p>seed</p>" } as EmailNotificationMessage,
        targets: [{ type: NotificationTargetType.REALM, id: realm }],
    } as Notification, config);
    return subject;
}

/** Create an asset in `realm` (REST, tracked for teardown) and return its id. */
async function createAssetAndGetId(manager: Manager, asset: Asset, config: AxiosRequestConfig): Promise<string> {
    await manager.createAsset(asset, config);
    const created = manager.assets.find((a) => a.name === asset.name);
    expect(created?.id).toBeTruthy();
    return created!.id!;
}

/** Link each of `userIds` to `assetId` in `realm` (REST). One call per user, as a batch must be for a single user. */
async function linkUsersToAsset(manager: Manager, realm: string, assetId: string, userIds: string[], config: AxiosRequestConfig) {
    for (const userId of userIds) {
        await manager.api.AssetResource.createUserAssetLinks([{ id: { realm, userId, assetId } }], config);
    }
}

/**
 * Create a ConsoleAsset with a (fake) FCM push token in `realm` and link it to `userId` (REST, tracked for teardown),
 * so push targets resolve to a console in dev mode. Mirrors ManagerNotificationSetup#createConsole.
 */
async function createConsoleForUser(manager: Manager, realm: string, userId: string, username: string, config: AxiosRequestConfig) {
    const consoleId = await createAssetAndGetId(manager, {
        name: `${username} console`,
        type: "ConsoleAsset",
        realm,
        // ConsoleAsset requires these well-known attributes to be present to pass validation
        attributes: {
            notes: { name: "notes", type: "text" },
            location: { name: "location", type: "GEO_JSONPoint" },
            consoleName: { name: "consoleName", type: "text", value: `${username} console` },
            consoleVersion: { name: "consoleVersion", type: "text", value: "1.0.0" },
            consolePlatform: { name: "consolePlatform", type: "text", value: "Android 14" },
            consoleProviders: {
                name: "consoleProviders",
                type: "consoleProviders",
                value: {
                    push: {
                        version: "fcm",
                        requiresPermission: true,
                        hasPermission: true,
                        success: true,
                        enabled: true,
                        disabled: false,
                        data: { token: `dev-fcm-token-${username}` },
                    },
                },
            },
        },
    } as Asset, config);
    await linkUsersToAsset(manager, realm, consoleId, [userId], config);
}

/**
 * Create a throwaway user with the given client roles in the given realm (REST), then log in as them through the UI.
 * Used to exercise the page under a specific permission set without disturbing the stored admin session.
 */
async function createUserAndLogin(
    manager: Manager,
    page: Page,
    { realm, username, roles }: { realm: string; username: string; roles: string[] },
) {
    // set an initial password (== username) so the throwaway user can log in via the UI
    await manager.provisionUser(realm, { username, roles, password: username });

    await manager.goToRealmStartPage(realm);
    await page.getByRole("textbox", { name: "Username or email" }).fill(username);
    await page.getByRole("textbox", { name: "Password" }).fill(username);
    await page.keyboard.press("Enter");
    await page.waitForURL("**/manager/**");
}

/** Format a Date as the local `YYYY-MM-DDTHH:mm` string the date-range pickers use. */
function pickerValue(d: Date): string {
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

/** A Date at `days` ago (0 = today) with the given time-of-day. */
function daysAgo(days: number, hours = 0, minutes = 0): Date {
    const d = new Date();
    d.setDate(d.getDate() - days);
    d.setHours(hours, minutes, 0, 0);
    return d;
}

/**
 * @given Logged into the "master" realm as "admin"
 * @and A notification has just been sent (REST setup), so it falls within today's default range
 * @and Navigated to the "Notifications" page
 * @then Both date filters are pre-populated and the notification is listed under the default (last day) range
 * @when The range is moved to a window in the past that excludes today
 * @then The notification is filtered out, and reappears once the range is widened to include today again
 */
test("should filter the notifications table by the date range", async ({ manager, notificationsPage }) => {
    // a REST-sent notification is stamped "now", so it sits inside the default (today) range
    const subject = await seedNotification(manager);

    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    // the default range is pre-populated ...
    const pickers = notificationsPage.getDatePickers();
    await expect(pickers).toHaveCount(2);
    await expect(pickers.nth(0).locator("input").first()).not.toHaveValue("");
    await expect(pickers.nth(1).locator("input").first()).not.toHaveValue("");

    // ... and the just-sent notification falls within it
    await expect(notificationsPage.getRowByText(subject)).toBeVisible();

    // moving the window to a past range that excludes today filters the notification out
    await notificationsPage.setDateRange(pickerValue(daysAgo(10)), pickerValue(daysAgo(9, 23, 59)));
    await expect(notificationsPage.getRowByText(subject)).toHaveCount(0);

    // widening the range back to include today brings it back
    await notificationsPage.setDateRange(pickerValue(daysAgo(0)), pickerValue(daysAgo(0, 23, 59)));
    await expect(notificationsPage.getRowByText(subject)).toBeVisible();
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and A recipient user with an email exists (REST setup)
 * @and Navigated to the "Notifications" page
 * @when The user fills in the send form step by step and submits it
 * @then The submit button stays disabled until a message and a recipient are provided
 * @and Submitting closes the dialog and the new notification appears in the table
 */
test("should disable the submit button until valid, then send and show it in the table", async ({ manager, notificationsPage }) => {
    // setup via REST: a recipient with an email so the email notification is deliverable/persisted
    await ensureRecipient(manager);

    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    const subject = `E2E created ${Date.now()}`;
    await notificationsPage.openCreateDialog();

    // nothing filled in -> cannot submit
    await expect(notificationsPage.getSubmitButton()).toBeDisabled();

    // a message but still no recipient -> cannot submit
    await notificationsPage.selectMessageType("Email");
    await notificationsPage.fillEmailMessage(subject, "Hello from the E2E test");
    await expect(notificationsPage.getSubmitButton()).toBeDisabled();

    // add a recipient -> the form is now valid
    await notificationsPage.selectTargetType("Users");
    await notificationsPage.checkTarget(RECIPIENT_USERNAME);
    await expect(notificationsPage.getSubmitButton()).toBeEnabled();

    // submitting closes the dialog and the notification appears in the table
    await notificationsPage.getSubmitButton().click();
    await expect(notificationsPage.getCreateForm()).not.toBeVisible();
    await expect(notificationsPage.getRowByText(subject)).toBeVisible();
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and A notification with source CLIENT has been seeded (REST setup)
 * @and Navigated to the "Notifications" page
 * @when The user changes the source filter
 * @then Only notifications matching the selected source remain in the table
 */
test("should filter notifications by source", async ({ manager, notificationsPage }) => {
    // a REST-sent notification has source CLIENT
    const subject = await seedNotification(manager);

    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    // visible under the default ("All sources") filter
    await expect(notificationsPage.getRowByText(subject)).toBeVisible();

    // a source it does not have hides it
    await notificationsPage.setSourceFilter("Realm ruleset");
    await expect(notificationsPage.getRowByText(subject)).toHaveCount(0);

    // its own source shows it again
    await notificationsPage.setSourceFilter("Client");
    await expect(notificationsPage.getRowByText(subject)).toBeVisible();
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and A notification has been seeded via the REST API
 * @and Navigated to the "Notifications" page
 * @when The user clicks the notification row
 * @then A read-only details dialog opens
 * @and It can be closed via the top-right cross
 */
test("should show the correct data in the notification details dialog", async ({ manager, notificationsPage }) => {
    // setup via REST: persist a known notification so there is a row to open
    const subject = await seedNotification(manager);

    // --- action (E2E): open the seeded row's details dialog ---
    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    await expect(notificationsPage.getRowByText(subject)).toBeVisible();
    await notificationsPage.openDetailsByText(subject);

    const details = notificationsPage.getDetailsForm();

    // the dialog reflects the seeded notification's content and resolved metadata
    await expect(details.locator("#notificationSubject input")).toHaveValue(subject);
    await expect(details.locator("#notificationEmailBody textarea")).toHaveValue("<p>seed</p>");
    // the realm-targeted email resolves to a per-user record, sent from the REST client
    await expect(notificationsPage.getDetailsFieldByLabel("Recipient type")).toHaveValue("User");
    await expect(notificationsPage.getDetailsFieldByLabel("Source")).toHaveValue(/Client/i);

    await notificationsPage.getDetailsCloseButton().click();
    await expect(details).not.toBeVisible();
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and Three notifications seeded via REST: a plain send, one marked delivered, and one whose send failed
 * @when Navigated to the "Notifications" page
 * @then Their rows show the "Sent", "Delivered" and "Error" status badges respectively
 * @and The error badge carries the failure reason as its tooltip
 */
test("should show the sent, delivered and error statuses in the table", async ({ manager, notificationsPage }) => {
    const config = await manager.adminConfig();
    const now = Date.now();

    // Sent: a plain seed persists without delivery confirmation (dev mode skips the actual SMTP send)
    const sentSubject = await seedNotification(manager, "master", `E2E sent ${now}`);

    // Delivered: seed another and mark its per-user rows delivered via the REST API
    const deliveredSubject = await seedNotification(manager, "master", `E2E delivered ${now}`);
    const seeded = (await manager.api.NotificationResource.getNotifications({
        from: now - 60_000,
        to: now + 60_000,
        realmId: "master",
    }, config)).data.filter((n) => n.name === deliveredSubject);
    expect(seeded.length).toBeGreaterThan(0);
    for (const n of seeded) {
        await manager.api.NotificationResource.notificationDelivered(n.id!, { targetId: n.targetId! }, config);
    }

    // Error: a custom target without any address persists the notification, then fails with "no recipients"
    // (the send request itself returns a 400 after the row is stored, so swallow it)
    const errorSubject = `E2E error ${now}`;
    await manager.api.NotificationResource.sendNotification({
        name: errorSubject,
        message: { type: "email", subject: errorSubject, html: "<p>seed</p>" } as EmailNotificationMessage,
        targets: [{ type: NotificationTargetType.CUSTOM, id: "to:" }],
    } as Notification, config).catch(() => {});

    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    await expect(notificationsPage.getStatusBadge(sentSubject)).toHaveText("Sent");
    await expect(notificationsPage.getStatusBadge(deliveredSubject)).toHaveText("Delivered");
    await expect(notificationsPage.getStatusBadge(errorSubject)).toHaveText("Error");
    await expect(notificationsPage.getStatusBadge(errorSubject)).toHaveAttribute("title", /no recipients/i);
});

/**
 * @given Logged into the "master" realm as "admin"
 * @and An asset with two users linked to it, each with a push-registered console (REST setup)
 * @and Navigated to the "Notifications" page
 * @when A push notification is composed and sent to that asset target
 * @then Submitting closes the dialog and the push fans out to a row per linked user's console
 */
test("should send a push notification to an asset target linked to multiple users", async ({ manager, notificationsPage }) => {
    const config = await manager.adminConfig();
    const stamp = Date.now();
    const assetName = `E2E push asset ${stamp}`;
    const assetId = await createAssetAndGetId(manager, {
        name: assetName,
        type: "ThingAsset",
        realm: "master",
        // ThingAsset requires these well-known attributes to be present to pass validation
        attributes: {
            notes: { name: "notes", type: "text" },
            location: { name: "location", type: "GEO_JSONPoint" },
        },
    } as Asset, config);

    // two users linked to the asset, each with a push-registered console so an asset push resolves to their consoles
    const usernames = [`e2e-push-1-${stamp}`, `e2e-push-2-${stamp}`];
    for (const username of usernames) {
        const user = await manager.provisionUser("master", { username }, config);
        expect(user?.id).toBeTruthy();
        await linkUsersToAsset(manager, "master", assetId, [user!.id!], config);
        await createConsoleForUser(manager, "master", user!.id!, username, config);
    }

    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    const title = `E2E asset push ${stamp}`;
    await notificationsPage.openCreateDialog();
    await notificationsPage.selectMessageType("Push");
    await notificationsPage.fillPushMessage(title, "Hello from the E2E push test");

    // pick the asset as the target -> the form becomes valid
    await notificationsPage.selectTargetType("Users linked to assets");
    await notificationsPage.checkAssetTarget(assetName);
    await expect(notificationsPage.getSubmitButton()).toBeEnabled();

    // submitting closes the dialog and the push fans out to a record per linked user's console
    await notificationsPage.getSubmitButton().click();
    await expect(notificationsPage.getCreateForm()).not.toBeVisible();
    await expect(notificationsPage.getRowByText(title)).toHaveCount(usernames.length);
});

/**
 * Each test logs in as a freshly-created "smartcity" user with a specific permission set (REST setup) and asserts
 * the distinct UI condition that permission set should produce on the notifications page.
 */
test.describe("Role-Based Access Control", () => {
    // Start from a clean session so we can log in as the low-privilege user rather than a stored admin state.
    test.use({ storageState: { cookies: [], origins: [] } });

    /**
     * @given A "smartcity" user with read:notifications only (no write:admin/write:notifications)
     * @then The table is visible but the write-gated "Send new" button is hidden
     */
    test("should hide the send button for a user without write permission", async ({ page, manager, notificationsPage }) => {
        await createUserAndLogin(manager, page, {
            realm: "smartcity",
            username: "e2e-readonly",
            roles: ["read:notifications"],
        });

        await notificationsPage.goto();
        await expect(notificationsPage.getCreateButton()).not.toBeVisible();
    });

    /**
     * @given A "smartcity" user with write:notifications but no read:admin/read:users/read:assets
     * @then The "Send new" button is shown (write permission) but disabled (no recipient type can be chosen)
     */
    test("should disable the send button for a user who cannot choose any recipient type", async ({ page, manager, notificationsPage }) => {
        await createUserAndLogin(manager, page, {
            realm: "smartcity",
            username: "e2e-sender",
            roles: ["read:notifications", "write:notifications"],
        });

        await notificationsPage.goto();
        await expect(notificationsPage.getCreateButton()).toBeVisible();
        await expect(notificationsPage.getCreateButton()).toBeDisabled();
    });

    /**
     * @given A "smartcity" user with write:notifications and read:users but no asset read permission
     * @when The create dialog is opened for the first time
     * @then The target type is forced to Users and the recipient checkbox list is populated
     */
    test("should list user recipients on first open for a user who cannot read assets", async ({ page, manager, notificationsPage }) => {
        await ensureRecipient(manager, "smartcity", await manager.adminConfig());
        await createUserAndLogin(manager, page, {
            realm: "smartcity",
            username: "e2e-user-sender",
            roles: ["read:notifications", "write:notifications", "read:users"],
        });

        await notificationsPage.goto();
        await notificationsPage.openCreateDialog();
        await expect(notificationsPage.getCreateForm().locator("#target")
            .getByRole("checkbox", { name: RECIPIENT_USERNAME })).toBeVisible();
    });

    /**
     * @given A notification seeded into "smartcity" (REST) and a "smartcity" user with read:notifications only
     * @when That user opens the notification's details dialog
     * @then The recipient type is shown, but the recipient's identity is sanitised away (shown as "-")
     */
    test("should hide the recipient identity for a viewer without user/asset read permission", async ({ page, manager, notificationsPage }) => {
        const subject = await seedNotification(manager, "smartcity");
        await createUserAndLogin(manager, page, {
            realm: "smartcity",
            username: "e2e-viewer",
            roles: ["read:notifications"],
        });

        await notificationsPage.goto();
        await notificationsPage.openDetailsByText(subject);

        // the realm email resolves to per-user USER targets; the type is visible, the identity is not
        await expect(notificationsPage.getDetailsFieldByLabel("Recipient type")).toHaveValue("User");
        await expect(notificationsPage.getDetailsFieldByLabel("Recipient", true)).toHaveValue("-");
    });
});
