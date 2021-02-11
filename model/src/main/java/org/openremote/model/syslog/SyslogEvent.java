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

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "SYSLOG_EVENT")
public class SyslogEvent extends SharedEvent {

    static public class LevelCategoryFilter extends EventFilter<SyslogEvent> {

        public static final String FILTER_TYPE = "level-category-filter";

        protected SyslogLevel level;
        protected List<SyslogCategory> categories = new ArrayList<>();

        protected LevelCategoryFilter() {
        }

        @SuppressWarnings("unchecked")
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

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "LEVEL", nullable = false)
    protected SyslogLevel level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false)
    protected SyslogCategory category;

    @Column(name = "SUBCATEGORY", length = 1024)
    protected String subCategory;

    @Column(name = "MESSAGE", length = 131072)
    protected String message;

    protected SyslogEvent() {
    }

    public SyslogEvent(long timestamp, SyslogLevel level, SyslogCategory category, String subCategory, String message) {
        super(timestamp);
        this.level = level;
        this.category = category;
        this.subCategory = subCategory;
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

    public String getCategoryLabel() {
        return getCategory().name() + getSubCategory().map(s -> " - " + s).orElse("");
    }

    public void setCategory(SyslogCategory category) {
        this.category = category;
    }

    public Optional<String> getSubCategory() {
        return Optional.ofNullable(subCategory);
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
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
            ", subCategory=" + subCategory +
            ", message='" + message + '\'' +
            '}';
    }
}
