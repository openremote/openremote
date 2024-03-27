/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.query;

import org.openremote.model.dashboard.DashboardAccess;
import org.openremote.model.query.filter.ParentPredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;

import java.io.Serializable;
import java.util.Arrays;

public class DashboardQuery implements Serializable {

    /**
     * A top-level object of {@link DashboardQuery}, that is meant for filtering its output.
     * For example used to only return metadata of dashboards, instead of ALL data.
     */
    public static class Select {
        protected boolean metadata;
        protected boolean template;

        public Select() {
            this.metadata = true;
            this.template = true;
        }

        public Select(boolean excludeMetadata, boolean excludeTemplate) {
            this.metadata = !excludeMetadata;
            this.template = !excludeTemplate;
        }

        public Select excludeMetadata(boolean excludeMetadata) {
            this.metadata = !excludeMetadata;
            return this;
        }

        public Select excludeTemplate(boolean excludeTemplate) {
            this.template = !excludeTemplate;
            return this;
        }

        public boolean hasMetadata() {
            return metadata;
        }

        public boolean hasTemplateData() {
            return template;
        }
    }

    /**
     *
     */
    public static class DashboardConditions {
        protected DashboardAccess[] viewAccess;
        protected DashboardAccess[] editAccess;
        protected Integer minWidgets;

        public DashboardConditions() {
            this.viewAccess = new DashboardAccess[]{DashboardAccess.PUBLIC, DashboardAccess.SHARED, DashboardAccess.PRIVATE};
            this.editAccess = new DashboardAccess[]{DashboardAccess.PUBLIC, DashboardAccess.SHARED, DashboardAccess.PRIVATE};
        }

        public DashboardConditions(DashboardAccess[] viewAccess, DashboardAccess[] editAccess, Integer minWidgets) {
            this.viewAccess = viewAccess;
            this.editAccess = editAccess;
            this.minWidgets = minWidgets;
        }

        public DashboardConditions viewAccess(DashboardAccess[] access) {
            this.viewAccess = access;
            return this;
        }

        public DashboardConditions editAccess(DashboardAccess[] editAccess) {
            this.editAccess = editAccess;
            return this;
        }

        public DashboardConditions minWidgets(Integer minWidgets) {
            this.minWidgets = minWidgets;
            return this;
        }

        public DashboardAccess[] getViewAccess() {
            return viewAccess;
        }

        public DashboardAccess[] getEditAccess() {
            return editAccess;
        }

        public Integer getMinWidgets() {
            return minWidgets;
        }
    }

    /**
     *
     */
    public enum AssetAccess {
        RESTRICTED, LINKED, REALM
    }

    /**
     *
     */
    public enum ConditionMinAmount {
        AT_LEAST_ONE, ALL, NONE
    }

    /**
     *
     */
    public static class AssetConditions {
        public AssetAccess[] access;
        public ConditionMinAmount minAmount;
        public ParentPredicate[] parents;

        public AssetConditions() {
            this.access = new AssetAccess[]{AssetAccess.REALM, AssetAccess.LINKED};
            this.minAmount = ConditionMinAmount.ALL;
            this.parents = new ParentPredicate[]{};
        }

        public AssetConditions(AssetAccess[] access, ConditionMinAmount minAmount, ParentPredicate[] parents) {
            this.access = access;
            this.minAmount = minAmount;
            this.parents = parents;
        }

        public AssetConditions access(AssetAccess... access) {
            this.access = access;
            return this;
        }

        public AssetConditions minAmount(ConditionMinAmount minAmount) {
            this.minAmount = minAmount;
            return this;
        }

        public AssetConditions parents(ParentPredicate... parents) {
            this.parents = parents;
            return this;
        }

        public AssetAccess[] getAccess() {
            return access;
        }

        public ConditionMinAmount getMinAmount() {
            return minAmount;
        }

        public ParentPredicate[] getParents() {
            return parents;
        }
    }

    public static class Conditions {
        protected DashboardConditions dashboard;
        protected AssetConditions asset;

        public Conditions() {
            this(new DashboardConditions(), new AssetConditions());
        }
        public Conditions(DashboardConditions dashboard) {
            this(dashboard, null);
        }
        public Conditions(AssetConditions asset) {
            this(null, asset);
        }
        public Conditions(DashboardConditions dashboard, AssetConditions asset) {
            this.dashboard = dashboard;
            this.asset = asset;
        }

        public Conditions dashboard(DashboardConditions dashboard) {
            this.dashboard = dashboard;
            return this;
        }

        public Conditions asset(AssetConditions asset) {
            this.asset = asset;
            return this;
        }

        public DashboardConditions getDashboard() {
            return dashboard;
        }

        public AssetConditions getAsset() {
            return asset;
        }
    }

    // Response mapping
    public Select select;

    // Filtering predicates
    public Conditions conditions;
    public String[] ids;
    public StringPredicate[] names;
    public String[] userIds;
    public RealmPredicate realm;

    // Pagination fields
    public Integer start;
    public Integer limit;

    public DashboardQuery() {
        this.select = new Select();
        this.conditions = new Conditions();
        this.start = 0;
        this.limit = 50;
    }

    public DashboardQuery select(Select select) {
        this.select = select;
        return this;
    }

    public DashboardQuery conditions(Conditions conditions) {
        this.conditions = conditions;
        return this;
    }

    public DashboardQuery ids(String... ids) {
        this.ids = ids;
        return this;
    }

    public DashboardQuery names(String... names) {
        if (names == null || names.length == 0) {
            this.names = null;
            return this;
        }
        this.names = Arrays.stream(names).map(StringPredicate::new).toArray(StringPredicate[]::new);
        return this;
    }

    public DashboardQuery names(StringPredicate... names) {
        this.names = names;
        return this;
    }

    public DashboardQuery userIds(String... ids) {
        this.userIds = ids;
        return this;
    }

    public DashboardQuery realm(RealmPredicate realm) {
        this.realm = realm;
        return this;
    }

    public DashboardQuery start(Integer start) {
        this.start = start;
        return this;
    }

    public DashboardQuery limit(Integer limit) {
        this.limit = limit;
        return this;
    }
}
