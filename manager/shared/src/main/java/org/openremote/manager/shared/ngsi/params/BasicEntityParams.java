/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.ngsi.params;

import com.fasterxml.jackson.annotation.JsonInclude;

public class BasicEntityParams {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String idPattern;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String type;

    public BasicEntityParams(String idParam, boolean isPattern) {
        if (isPattern) {
            this.idPattern = idParam;
        } else {
            this.id = idParam;
        }
    }

    public BasicEntityParams(String idParam, boolean isPattern, String type) {
        this(idParam, isPattern);
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getIdPattern() {
        return idPattern;
    }

    public String getType() {
        return type;
    }
}
