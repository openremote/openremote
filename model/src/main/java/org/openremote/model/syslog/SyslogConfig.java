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

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

public class SyslogConfig {

    public static final int DEFAULT_LIMIT = 50;

    @NotNull
    protected SyslogLevel storedLevel;

    @NotNull
    protected SyslogCategory[] storedCategories;

    @NotNull
    @Range(max = 10080) // TODO 1 week max or we run out of space?
    protected int storedMaxAgeMinutes;

    public SyslogConfig() {
    }

    public SyslogConfig(SyslogLevel storedLevel, SyslogCategory[] storedCategories, int storedMaxAgeMinutes) {
        this.storedLevel = storedLevel;
        this.storedCategories = storedCategories;
        this.storedMaxAgeMinutes = storedMaxAgeMinutes;
    }

    public SyslogLevel getStoredLevel() {
        return storedLevel;
    }

    public void setStoredLevel(SyslogLevel storedLevel) {
        this.storedLevel = storedLevel;
    }

    public SyslogCategory[] getStoredCategories() {
        return storedCategories;
    }

    public void setStoredCategories(SyslogCategory[] storedCategories) {
        this.storedCategories = storedCategories;
    }

    public int getStoredMaxAgeMinutes() {
        return storedMaxAgeMinutes;
    }

    public void setStoredMaxAgeMinutes(int storedMaxAgeMinutes) {
        this.storedMaxAgeMinutes = storedMaxAgeMinutes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "storedLevel=" + storedLevel +
            ", storedCategories=" + Arrays.toString(storedCategories) +
            ", storedMaxAgeMinutes=" + storedMaxAgeMinutes +
            '}';
    }
}
