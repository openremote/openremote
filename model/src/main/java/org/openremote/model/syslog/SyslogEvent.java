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
package org.openremote.model.syslog;

import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SyslogEvent extends SharedEvent {

    static public class LevelCategoryFilter extends EventFilter<SyslogEvent> {

        public static final String FILTER_TYPE = "level-category-filter";

        protected SyslogLevel level;
        protected List<SyslogCategory> categories = new ArrayList<>();

        protected LevelCategoryFilter() {
        }

        public LevelCategoryFilter(SyslogLevel level, SyslogCategory... categories) {
            this.level = level;
            this.categories = categories != null ? Arrays.asList(categories) : Collections.EMPTY_LIST;
        }

        @Override
        public String getFilterType() {
            return FILTER_TYPE;
        }

        public List<SyslogCategory> getCategories() {
            return categories;
        }

        public SyslogLevel getLevel() {
            return level;
        }

        public void setLevel(SyslogLevel level) {
            this.level = level;
        }

        public void setCategories(List<SyslogCategory> categories) {
            this.categories = categories;
        }

        @Override
        public boolean apply(SyslogEvent event) {
            return (getCategories().isEmpty() || getCategories().contains(event.getCategory()))
                && (getLevel() == null || getLevel().ordinal() <= event.getLevel().ordinal());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "level=" + level +
                ", categories=" + categories +
                '}';
        }
    }

    protected SyslogLevel level;
    protected SyslogCategory category;
    protected String message;

    protected SyslogEvent() {
    }

    public SyslogEvent(SyslogLevel level, SyslogCategory category, String message) {
        this.level = level;
        this.category = category;
        this.message = message;
    }

    public SyslogLevel getLevel() {
        return level;
    }

    public void setLevel(SyslogLevel level) {
        this.level = level;
    }

    public SyslogCategory getCategory() {
        return category;
    }

    public void setCategory(SyslogCategory category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "level=" + level +
            ", category=" + category +
            ", message='" + message + '\'' +
            '}';
    }
}
