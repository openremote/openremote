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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class NotificationParams {
    @JsonProperty("attrs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected List<String> includeAttributes;
    @JsonProperty("exceptAttrs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected List<String> excludeAttributes;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected HttpParams http;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected HttpCustomParams httpCustom;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Date lastNotification;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Integer timesSent;

    public NotificationParams() {
    }

    public void setIncludeAttributes(List<String> includeAttributes) {
        this.includeAttributes = includeAttributes;
    }

    public void setExcludeAttributes(List<String> excludeAttributes) {
        this.excludeAttributes = excludeAttributes;
    }

    public void setHttp(HttpParams http) {
        this.http = http;
    }

    public void setHttpCustom(HttpCustomParams httpCustom) {
        this.httpCustom = httpCustom;
    }

    public List<String> getIncludeAttributes() {
        return includeAttributes;
    }

    public List<String> getExcludeAttributes() {
        return excludeAttributes;
    }

    public HttpParams getHttp() {
        return http;
    }

    public HttpCustomParams getHttpCustom() {
        return httpCustom;
    }

    public Date getLastNotification() {
        return lastNotification;
    }

    public int getTimesSent() {
        return timesSent;
    }
}
