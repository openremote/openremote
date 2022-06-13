/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.rules;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.query.RulesetQuery;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.util.ValueUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class RulesetStorageService implements ContainerService {

    protected interface ParameterBinder extends Consumer<PreparedStatement> {

        @Override
        default void accept(PreparedStatement st) {
            try {
                acceptStatement(st);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        void acceptStatement(PreparedStatement st) throws SQLException;
    }

    private static final Logger LOG = Logger.getLogger(RulesetStorageService.class.getName());
    public static final int PRIORITY = AssetStorageService.PRIORITY + 200;
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);

        container.getService(ManagerWebService.class).addApiSingleton(
                new RulesResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        this,
                        container.getService(AssetStorageService.class),
                        container.getService(RulesService.class)
                )
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public <T extends Ruleset> T find(Class<T> rulesetType, Long id) {
        return find(rulesetType, id, true);
    }

    public <T extends Ruleset> T find(Class<T> rulesetType, Long id, boolean loadComplete) {
        if (id == null)
            throw new IllegalArgumentException("Can't query null ruleset identifier");
        return find(rulesetType, new RulesetQuery()
            .setFullyPopulate(loadComplete)
            .setIds(id));
    }

    public <T extends Ruleset> T find(Class<T> rulesetType, RulesetQuery query) {
        List<T> result = findAll(rulesetType, query);
        if (result.isEmpty())
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one ruleset");
        }
        return result.get(0);
    }

    public <T extends Ruleset> List<T> findAll(Class<T> rulesetType, RulesetQuery query) {
        return persistenceService.doReturningTransaction(em -> {
            LOG.finer("Building: " + query);
            StringBuilder sb = new StringBuilder();
            List<ParameterBinder> binders = new ArrayList<>();
            appendSelectString(sb, rulesetType, query);
            appendFromString(sb, rulesetType, query);
            appendWhereString(sb, rulesetType, query, binders);
            appendOrder(sb, query);
            appendLimit(sb, query);

            String sqlQuery = sb.toString();

            return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<T>>() {
                @Override
                public List<T> execute(Connection connection) throws SQLException {
                    LOG.finer("Executing: " + sqlQuery);
                    try (PreparedStatement st = connection.prepareStatement(sqlQuery)) {
                        binders.forEach(parameterBinder -> parameterBinder.accept(st));

                        try (ResultSet rs = st.executeQuery()) {
                            List<T> result = new ArrayList<>();
                            while (rs.next()) {
                                result.add(mapResultTuple(rulesetType, query, rs));
                            }
                            return result;
                        }
                    }
                }
            });
        });
    }

    public <T extends Ruleset> T merge(T ruleset) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.merge(ruleset)
        );
    }

    public <T extends Ruleset> void delete(Class<T> rulesetType, Long id) {
        persistenceService.doTransaction(entityManager -> {
            Ruleset ruleset = entityManager.find(rulesetType, id);
            if (ruleset != null)
                entityManager.remove(ruleset);
        });
    }

    protected <T extends Ruleset> void appendSelectString(StringBuilder sb, Class<T> rulesetType, RulesetQuery query) {
        sb.append("SELECT R.ID, R.VERSION, R.RULES_LANG, R.ENABLED, R.LAST_MODIFIED, R.CREATED_ON, R.NAME, R.META");

        if (query.fullyPopulate) {
            sb.append(", R.RULES");
        }

        if (rulesetType == RealmRuleset.class) {
            sb.append(", R.ACCESS_PUBLIC_READ, R.REALM");
        } else if (rulesetType == AssetRuleset.class) {
            sb.append(", R.ACCESS_PUBLIC_READ, R.ASSET_ID, A.REALM AS REALM");
        }
    }

    protected <T extends Ruleset> void appendFromString(StringBuilder sb, Class<T> rulesetType, RulesetQuery query) {
        if (rulesetType == GlobalRuleset.class) {
            sb.append(" FROM GLOBAL_RULESET R");
        }else if (rulesetType == RealmRuleset.class) {
            sb.append(" FROM REALM_RULESET R");
        } else if (rulesetType == AssetRuleset.class) {
            sb.append(" FROM ASSET_RULESET R JOIN ASSET A ON (R.ASSET_ID = A.ID)");
        } else {
            throw new UnsupportedOperationException("Ruleset type not supported: " + rulesetType);
        }
    }

    protected <T extends Ruleset> void appendWhereString(StringBuilder sb, Class<T> rulesetType, RulesetQuery query, List<ParameterBinder> binders) {
        sb.append(" WHERE 1=1");

        if (query.publicOnly) {
            sb.append(" AND R.ACCESS_PUBLIC_READ");
        }
        if (query.enabledOnly) {
            sb.append(" AND R.ENABLED");
        }
        if (query.languages != null && query.languages.length > 0) {
            sb.append(" AND R.RULES_LANG IN (?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.languages[0].toString()));

            for (int i = 1; i < query.languages.length; i++) {
                sb.append(",?");
                final int pos2 = binders.size() + 1;
                final int index = i;
                binders.add(st -> st.setString(pos2, query.languages[index].toString()));
            }
            sb.append(")");
        }
        if (query.ids != null && query.ids.length > 0) {
            sb.append(" AND R.ID IN (?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setLong(pos, query.ids[0]));

            for (int i = 1; i < query.ids.length; i++) {
                sb.append(",?");
                final int pos2 = binders.size() + 1;
                final int index = i;
                binders.add(st -> st.setLong(pos2, query.ids[index]));
            }
            sb.append(")");
        }

        if (query.meta != null) {
            // TODO: Add meta filtering support
        }

        if (query.realm != null && (rulesetType == RealmRuleset.class || rulesetType == AssetRuleset.class)) {
            sb.append(" AND REALM = ?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.realm));
        }
        if (query.assetIds != null && query.assetIds.length > 0 && rulesetType == AssetRuleset.class) {
            sb.append(" AND R.ASSET_ID IN (?");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.assetIds[0]));

            for (int i = 1; i < query.assetIds.length; i++) {
                sb.append(",?");
                final int pos2 = binders.size() + 1;
                final int index = i;
                binders.add(st -> st.setString(pos2, query.assetIds[index]));
            }
            sb.append(")");
        }
    }

    protected void appendOrder(StringBuilder sb, RulesetQuery query) {
        sb.append(" ORDER BY CREATED_ON ASC");
    }

    protected void appendLimit(StringBuilder sb, RulesetQuery query) {
        if (query.limit > 0) {
            sb.append(" LIMIT ").append(query.limit);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends Ruleset> T mapResultTuple(Class<T> rulesetType, RulesetQuery query, ResultSet rs) throws SQLException {
        T ruleset;

        if (rulesetType == GlobalRuleset.class) {
            ruleset = (T) new GlobalRuleset();
        } else if (rulesetType == RealmRuleset.class) {
            RealmRuleset realmRuleset = new RealmRuleset();
            realmRuleset.setRealm(rs.getString("REALM"));
            realmRuleset.setAccessPublicRead(rs.getBoolean("ACCESS_PUBLIC_READ"));
            ruleset = (T) realmRuleset;
        } else if (rulesetType == AssetRuleset.class) {
            AssetRuleset assetRuleset = new AssetRuleset();
            assetRuleset.setAssetId(rs.getString("ASSET_ID"));
            assetRuleset.setRealm(rs.getString("REALM"));
            assetRuleset.setAccessPublicRead(rs.getBoolean("ACCESS_PUBLIC_READ"));
            ruleset = (T) assetRuleset;
        } else {
            throw new UnsupportedOperationException("Ruleset type not supported: " + rulesetType);
        }

        ruleset.setName(rs.getString("NAME"));
        ruleset.setId(rs.getLong("ID"));
        ruleset.setVersion(rs.getLong("VERSION"));
        ruleset.setLang(Ruleset.Lang.valueOf(rs.getString("RULES_LANG")));
        ruleset.setEnabled(rs.getBoolean("ENABLED"));
        ruleset.setLastModified(rs.getTimestamp("LAST_MODIFIED"));
        ruleset.setCreatedOn(rs.getTimestamp("CREATED_ON"));
        if (rs.getString("META") != null) {
            ruleset.setMeta(ValueUtil.parse(rs.getString("META"), MetaMap.class).orElse(null));
        }
        if (query.fullyPopulate) {
            ruleset.setRules(rs.getString("RULES"));
        }

        return ruleset;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
