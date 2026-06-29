import { expect, Page } from "@openremote/test";
import { EmailNotificationMessage, Notification, NotificationTargetType, User } from "@openremote/model";
import { Manager, adminStatePath, test } from "./fixtures/manager.js";
import { users } from "./fixtures/data/users.js";

test.use({ storageState: adminStatePath });

const CLIENT_ID = "openremote";

/** Admin-authenticated axios config for REST setup calls. */
async function adminConfig(manager: Manager) {
    const token = await manager.getAccessToken("master", "admin", users.admin.password!);
    return { headers: { Authorization: `Bearer ${token}` } };
}

const RECIPIENT_USERNAME = "e2e-notif-recipient";

/** Ensure a user with an email exists in the given realm, so email notifications resolve to a deliverable target. */
async function ensureRecipient(manager: Manager, config: { headers: Record<string, string> }, realm = "master") {
    await manager.api.UserResource.create(realm, {
        username: RECIPIENT_USERNAME,
        email: `${RECIPIENT_USERNAME}@openremote.local`,
        enabled: true,
    } as User, config).catch(() => undefined);
}

/** Persist a notification via REST (email to the given realm) and return its subject for table lookups. */
async function seedNotification(manager: Manager, realm = "master", subject = `E2E seed ${Date.now()}`) {
    const config = await adminConfig(manager);
    await ensureRecipient(manager, config, realm);
    await manager.api.NotificationResource.sendNotification({
        name: subject,
        message: { type: "email", subject, html: "<p>seed</p>" } as EmailNotificationMessage,
        targets: [{ type: NotificationTargetType.REALM, id: realm }],
    } as Notification, config);
    return subject;
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
    const config = await adminConfig(manager);
    const user = await manager.api.UserResource.create(realm, {
        username,
        enabled: true,
    } as User, config).then((r) => r.data).catch(() => undefined);

    if (user?.id) {
        await manager.api.UserResource.updateUserClientRoles(realm, user.id, CLIENT_ID, roles, config);
        // set an initial password (== username) so the throwaway user can log in via the UI
        await manager.api.UserResource.updatePassword(realm, user.id, { value: username }, config);
    }

    await manager.goToRealmStartPage(realm);
    await page.getByRole("textbox", { name: "Username or email" }).fill(username);
    await page.getByRole("textbox", { name: "Password" }).fill(username);
    await page.keyboard.press("Enter");
    await page.waitForURL("**/manager/**");
}

/**
 * @given Logged into the "master" realm as "admin"
 * @and Navigated to the "Notifications" page
 * @then The notifications table is visible
 * @and Both date-range filters are pre-populated with the default range
 */
test("should display the notifications table with populated default date filters", async ({ manager, notificationsPage }) => {
    await manager.goToRealmStartPage("master");
    await notificationsPage.goto();

    await expect(notificationsPage.getTable()).toBeVisible();

    const pickers = notificationsPage.getDatePickers();
    await expect(pickers).toHaveCount(2);
    await expect(pickers.nth(0).locator("input").first()).not.toHaveValue("");
    await expect(pickers.nth(1).locator("input").first()).not.toHaveValue("");
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
    await ensureRecipient(manager, await adminConfig(manager));

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
    await notificationsPage.submitCreate();
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

    await notificationsPage.closeDetailsViaCross();
    await expect(details).not.toBeVisible();
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
