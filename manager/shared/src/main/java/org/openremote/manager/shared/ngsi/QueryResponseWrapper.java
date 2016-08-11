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
package org.openremote.manager.shared.ngsi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class QueryResponseWrapper {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected List<QueryResponse> contextResponses;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected StatusCode errorCode;

    public QueryResponseWrapper(@JsonProperty("contextResponses") List<QueryResponse> contextResponses, @JsonProperty("errorCode") StatusCode errorCode) {
        this.contextResponses = contextResponses;
        this.errorCode = errorCode;
    }

    public QueryResponseWrapper(List<QueryResponse> contextResponses) {
        this.contextResponses = contextResponses;
    }

    public QueryResponseWrapper(StatusCode errorCode) {
        this.errorCode = errorCode;
    }

    public List<QueryResponse> getContextResponses() {
        return contextResponses;
    }

    public StatusCode getErrorCode() {
        return errorCode;
    }
}
