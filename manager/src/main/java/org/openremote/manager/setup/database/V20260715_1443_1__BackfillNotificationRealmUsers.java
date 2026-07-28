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
package org.openremote.manager.setup.database;

import org.flywaydb.core.api.migration.Context;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Companion to {@code V20260715_1443__AddNotificationRealm.sql}: backfills the {@code realm} column for
 * USER-targeted and CLIENT-sourced notifications by looking up each referenced user's realm via the Keycloak
 * Admin API (the stable public interface, not the internal schema). The Keycloak lookup is skipped entirely when
 * no rows reference a user (e.g. clean installs) or on non-Keycloak deployments. Any rows still unresolved after
 * the backfill (CUSTOM targets, deleted users/assets, INTERNAL/GLOBAL_RULESET sources) fall back to {@code master}.
 * Also applies the master fallback and {@code NOT NULL} constraint that the SQL migration deferred until all
 * resolvable rows were filled.
 */
public class V20260715_1443_1__BackfillNotificationRealmUsers extends AbstractKeycloakMigration {

    private static final int USER_PAGE_SIZE = 100;

    @Override
    public void migrate(Context context) throws Exception {
        if (isKeycloakDeployment()) {
            Set<String> unresolvedUserIds = findUnresolvedUserIds(context);
            if (unresolvedUserIds.isEmpty()) {
                LOG.info("No notifications reference a user; skipping Keycloak lookup");
            } else {
                Map<String, List<String>> realmUserIds = buildRealmUserIdsMap(unresolvedUserIds);
                if (!realmUserIds.isEmpty()) {
                    applyRealmUserIds(context, realmUserIds);
                }
            }
        } else {
            LOG.info("Identity provider is not keycloak; skipping user realm backfill");
        }

        // Master fallback for anything still unresolved, then enforce NOT NULL
        try (var stmt = context.getConnection().createStatement()) {
            stmt.executeUpdate("UPDATE NOTIFICATION SET realm = 'master' WHERE realm IS NULL");
            stmt.executeUpdate("ALTER TABLE NOTIFICATION ALTER COLUMN realm SET NOT NULL");
        }
    }

    private Set<String> findUnresolvedUserIds(Context context) throws Exception {
        Set<String> userIds = new HashSet<>();
        // USER-targeted rows hold the recipient's user id in TARGET_ID, CLIENT-sourced rows hold the
        // sender's user id in SOURCE_ID; UNION deduplicates ids appearing in both
        try (var stmt = context.getConnection().createStatement();
             var rs = stmt.executeQuery(
                     "SELECT TARGET_ID FROM NOTIFICATION WHERE realm IS NULL AND TARGET = 'USER'"
                     + " UNION SELECT SOURCE_ID FROM NOTIFICATION WHERE realm IS NULL AND SOURCE = 'CLIENT'")) {
            while (rs.next()) {
                String id = rs.getString(1);
                if (id != null) {
                    userIds.add(id);
                }
            }
        }
        return userIds;
    }

    private Map<String, List<String>> buildRealmUserIdsMap(Set<String> unresolvedUserIds) {
        Map<String, List<String>> realmUserIds = new HashMap<>();
        Set<String> remaining = new HashSet<>(unresolvedUserIds);

        try (Keycloak keycloak = openKeycloak()) {
            List<String> realmNames = keycloak.realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .toList();

            for (String realmName : realmNames) {
                List<String> matched = findUserIdsInRealm(keycloak, realmName, remaining);
                if (!matched.isEmpty()) {
                    realmUserIds.put(realmName, matched);
                    matched.forEach(remaining::remove);
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }
        }

        LOG.info("Resolved realm for " + (unresolvedUserIds.size() - remaining.size()) + " of "
                + unresolvedUserIds.size() + " users referenced by notifications");
        return realmUserIds;
    }

    /**
     * Pages through the realm's users and returns the ids present in {@code wanted}, stopping as soon as all of
     * them have been found.
     */
    private List<String> findUserIdsInRealm(Keycloak keycloak, String realmName, Set<String> wanted) {
        List<String> matched = new ArrayList<>();
        int first = 0;
        List<UserRepresentation> page;
        do {
            page = keycloak.realm(realmName).users().list(first, USER_PAGE_SIZE);
            page.stream()
                    .map(UserRepresentation::getId)
                    .filter(wanted::contains)
                    .forEach(matched::add);
            first += page.size();
        } while (page.size() == USER_PAGE_SIZE && matched.size() < wanted.size());
        return matched;
    }

    private void applyRealmUserIds(Context context, Map<String, List<String>> realmUserIds) throws Exception {
        // USER-targeted notifications: targetId is the user id
        updateRealmByUserIds(context, realmUserIds,
                "UPDATE NOTIFICATION SET realm = ? WHERE realm IS NULL AND TARGET = 'USER' AND TARGET_ID = ANY(?)");
        // CLIENT-sourced notifications: sourceId is the sending user id
        updateRealmByUserIds(context, realmUserIds,
                "UPDATE NOTIFICATION SET realm = ? WHERE realm IS NULL AND SOURCE = 'CLIENT' AND SOURCE_ID = ANY(?)");
    }

    private void updateRealmByUserIds(Context context, Map<String, List<String>> realmUserIds, String sql) throws Exception {
        try (PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, List<String>> entry : realmUserIds.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setArray(2, context.getConnection().createArrayOf("varchar", entry.getValue().toArray()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
