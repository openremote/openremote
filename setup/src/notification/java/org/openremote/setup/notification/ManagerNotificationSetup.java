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
package org.openremote.setup.notification;

import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.ConsoleAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.console.ConsoleProvider;
import org.openremote.model.notification.AbstractNotificationMessage;
import org.openremote.model.notification.EmailNotificationMessage;
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.PushNotificationMessage;
import org.openremote.model.notification.SentNotification;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.security.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.value.MetaItemType.RULE_STATE;

public class ManagerNotificationSetup extends ManagerSetup {

    /**
     * Value of the notes attribute (with RULE_STATE meta) on the seeded assets; can be matched in the when
     * clause of manually created rules to test rule triggered notifications.
     */
    public static final String RULE_TRIGGER_VALUE = "notification-test";

    private final KeycloakNotificationSetup keycloakSetup;

    public ManagerNotificationSetup(Container container, KeycloakNotificationSetup keycloakSetup) {
        super(container);
        this.keycloakSetup = keycloakSetup;
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Create a shared test asset in the master realm; the notes attribute is matched by the notification rulesets
        // and needs the RULE_STATE meta item so it is available as a fact in the rule engines
        ThingAsset testAsset = new ThingAsset("Notification Test Asset");
        testAsset.setRealm(MASTER_REALM);
        testAsset.setNotes(RULE_TRIGGER_VALUE);
        testAsset.getAttribute(Asset.NOTES).ifPresent(attr -> attr.addMeta(new MetaItem<>(RULE_STATE, true)));
        testAsset = assetStorageService.merge(testAsset);
        final String assetId = testAsset.getId();

        // Link all users to the asset
        List<User> users = List.of(
            keycloakSetup.notifRead,
            keycloakSetup.notifAssets,
            keycloakSetup.notifUsers,
            keycloakSetup.notifViewUsers,
            keycloakSetup.notifWrite,
            keycloakSetup.notifRestricted,
            keycloakSetup.notifRestrictedAssets
        );

        assetStorageService.storeUserAssetLinks(
            users.stream()
                .map(u -> new UserAssetLink(MASTER_REALM, u.getId(), assetId))
                .toList()
        );

        // Provision a console asset per user so push notification targets resolve to consoles in dev
        for (User user : users) {
            createConsole(MASTER_REALM, user);
        }

        // Persist three notifications per user spaced 1 minute apart, oldest first
        Instant base = Instant.now();
        int slot = 0;
        for (User user : users) {
            persistNotification("Welcome", "Welcome to OpenRemote notifications", user.getId(), assetId, MASTER_REALM, base.minus(++slot, ChronoUnit.MINUTES));
            persistNotification("Test Alert", "This is a pending test notification", user.getId(), assetId, MASTER_REALM, base.minus(++slot, ChronoUnit.MINUTES));
            persistEmailNotification("Email Alert", "<p>This is a test email notification</p>", user.getId(), assetId, MASTER_REALM, base.minus(++slot, ChronoUnit.MINUTES));
        }

        // Smartcity realm: asset, user-asset link, and seeded notifications
        ThingAsset smartCityAsset = new ThingAsset("Smart City Test Asset");
        smartCityAsset.setRealm(KeycloakNotificationSetup.SMARTCITY_REALM);
        smartCityAsset.setNotes(RULE_TRIGGER_VALUE);
        smartCityAsset.getAttribute(Asset.NOTES).ifPresent(attr -> attr.addMeta(new MetaItem<>(RULE_STATE, true)));
        smartCityAsset = assetStorageService.merge(smartCityAsset);
        final String smartCityAssetId = smartCityAsset.getId();

        assetStorageService.storeUserAssetLinks(List.of(
            new UserAssetLink(KeycloakNotificationSetup.SMARTCITY_REALM, keycloakSetup.smartCityUser.getId(), smartCityAssetId)
        ));

        createConsole(KeycloakNotificationSetup.SMARTCITY_REALM, keycloakSetup.smartCityUser);

        persistNotification("Welcome", "Welcome to Smart City", keycloakSetup.smartCityUser.getId(), smartCityAssetId, KeycloakNotificationSetup.SMARTCITY_REALM, base.minus(++slot, ChronoUnit.MINUTES));
        persistNotification("Test Alert", "This is a pending test notification", keycloakSetup.smartCityUser.getId(), smartCityAssetId, KeycloakNotificationSetup.SMARTCITY_REALM, base.minus(++slot, ChronoUnit.MINUTES));

        // Realm ruleset whose Groovy script body sends notifications directly once on deployment, so notifications
        // with the REALM_RULESET source show up for testing the source filter. Plain rules with a when clause
        // don't evaluate reliably on initial deployment.
        rulesetStorageService.merge(new RealmRuleset(
            MASTER_REALM,
            "Notification test realm rule",
            Ruleset.Lang.GROOVY,
            buildNotificationRulesGroovy("Realm rule", assetId)
        ));

        // Global rulesets are too problematic to use for this (see global-ruleset-issues.md) so the
        // GLOBAL_RULESET source records are persisted directly instead
        persistGlobalRuleNotifications(assetId, MASTER_REALM, base.minus(++slot, ChronoUnit.MINUTES));
        persistGlobalRuleNotifications(smartCityAssetId, KeycloakNotificationSetup.SMARTCITY_REALM, base.minus(++slot, ChronoUnit.MINUTES));
    }

    /**
     * Persists a push and an email notification record with the {@link Notification.Source#GLOBAL_RULESET} source
     * targeting the given asset, mimicking what a global ruleset notification action would produce.
     */
    private void persistGlobalRuleNotifications(String assetId, String realm, Instant sentOn) {
        SentNotification push = new SentNotification()
            .setName("Global rule push")
            .setType(PushNotificationMessage.TYPE)
            .setSource(Notification.Source.GLOBAL_RULESET)
            .setSourceId("")
            .setTarget(Notification.TargetType.ASSET)
            .setTargetId(assetId)
            .setMessage(new PushNotificationMessage()
                .setTitle("Global rule push")
                .setBody("Rule triggered push notification"))
            .setRealm(realm)
            .setSentOn(sentOn);

        SentNotification email = new SentNotification()
            .setName("Global rule email")
            .setType(EmailNotificationMessage.TYPE)
            .setSource(Notification.Source.GLOBAL_RULESET)
            .setSourceId("")
            .setTarget(Notification.TargetType.ASSET)
            .setTargetId(assetId)
            .setMessage(new EmailNotificationMessage()
                .setSubject("Global rule email")
                .setHtml("<p>Rule triggered email notification</p>"))
            .setRealm(realm)
            .setSentOn(sentOn);

        persistenceService.doTransaction(em -> {
            em.merge(push);
            em.merge(email);
        });
    }

    /**
     * Builds a Groovy ruleset whose script body executes once on deployment and sends a push and an email
     * notification targeting the given assets; push resolves to the consoles of linked users, email to their addresses.
     */
    private static String buildNotificationRulesGroovy(String namePrefix, String... assetIds) {
        String targets = Arrays.stream(assetIds)
            .map(id -> "new Notification.Target(Notification.TargetType.ASSET, \"" + id + "\")")
            .collect(Collectors.joining(", "));

        return """
            import org.openremote.model.notification.EmailNotificationMessage
            import org.openremote.model.notification.Notification
            import org.openremote.model.notification.PushNotificationMessage

            // Executed once when the ruleset is deployed
            def push = new Notification()
                .setName("%1$s push")
                .setMessage(new PushNotificationMessage()
                    .setTitle("%1$s push")
                    .setBody("Rule triggered push notification"))
            push.setTargets(%2$s)
            notifications.send(push)

            def email = new Notification()
                .setName("%1$s email")
                .setMessage(new EmailNotificationMessage()
                    .setSubject("%1$s email")
                    .setHtml("<p>Rule triggered email notification</p>"))
            email.setTargets(%2$s)
            notifications.send(email)
            """.formatted(namePrefix, targets);
    }

    /**
     * Creates a {@link ConsoleAsset} with a (fake) FCM push provider token and links it to the given user, mirroring
     * what a real console registration does, so USER/ASSET/REALM push targets resolve to consoles in dev.
     */
    private void createConsole(String realm, User user) {
        ConsoleAsset console = new ConsoleAsset(user.getUsername() + " console")
            .setConsoleName(user.getUsername() + " console")
            .setConsoleVersion("1.0.0")
            .setConsolePlatform("Android 14")
            .setConsoleProvider(PushNotificationMessage.TYPE, new ConsoleProvider(
                "fcm", true, true, true, true, false, Map.of("token", "dev-fcm-token-" + user.getUsername())
            ));
        console.setRealm(realm);
        console = assetStorageService.merge(console);

        assetStorageService.storeUserAssetLinks(List.of(new UserAssetLink(realm, user.getId(), console.getId())));
    }

    private void persistNotification(String title, String body, String userId, String assetId, String realm, Instant sentOn) {
        PushNotificationMessage message = new PushNotificationMessage()
            .setTitle(title)
            .setBody(body);

        persist(title, PushNotificationMessage.TYPE, message, userId, assetId, realm, sentOn);
    }

    private void persistEmailNotification(String subject, String html, String userId, String assetId, String realm, Instant sentOn) {
        EmailNotificationMessage message = new EmailNotificationMessage()
            .setSubject(subject)
            .setHtml(html);

        persist(subject, EmailNotificationMessage.TYPE, message, userId, assetId, realm, sentOn);
    }

    private void persist(String name, String type, AbstractNotificationMessage message, String userId, String assetId, String realm, Instant sentOn) {
        // One notification targeting the user directly
        SentNotification toUser = new SentNotification()
            .setName(name)
            .setType(type)
            .setSource(Notification.Source.INTERNAL)
            .setSourceId(userId)
            .setTarget(Notification.TargetType.USER)
            .setTargetId(userId)
            .setMessage(message)
            .setRealm(realm)
            .setSentOn(sentOn);

        // One notification targeting the shared asset
        SentNotification toAsset = new SentNotification()
            .setName(name)
            .setType(type)
            .setSource(Notification.Source.INTERNAL)
            .setSourceId(userId)
            .setTarget(Notification.TargetType.ASSET)
            .setTargetId(assetId)
            .setMessage(message)
            .setRealm(realm)
            .setSentOn(sentOn);

        persistenceService.doTransaction(em -> {
            em.merge(toUser);
            em.merge(toAsset);
        });
    }
}
