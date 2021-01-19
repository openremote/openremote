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
package org.openremote.model.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.openremote.model.attribute.AttributeRef;

import static org.openremote.model.asset.AssetResource.Util.WRITE_ATTRIBUTE_HTTP_METHOD;
import static org.openremote.model.asset.AssetResource.Util.getWriteAttributeUrl;


public class PushNotificationAction {

    protected String url;
    protected String httpMethod;
    protected Object data;
    protected boolean silent;
    protected boolean openInBrowser; // For app based consoles (i.e. Android and iOS)

    public PushNotificationAction(String url) {
        this.url = url;
    }

    @JsonCreator
    public PushNotificationAction(@JsonProperty("url") String url,
                                  @JsonProperty("data") Object data,
                                  @JsonProperty("silent") boolean silent,
                                  @JsonProperty("openInBrowser") boolean openInBrowser,
                                  @JsonProperty("httpMethod") String httpMethod) {
        this.url = url;
        this.data = data;
        this.silent = silent;
        this.openInBrowser = openInBrowser;
        this.httpMethod = httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Object getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isOpenInBrowser() {
        return openInBrowser;
    }

    public void setOpenInBrowser(boolean openInBrowser) {
        this.openInBrowser = openInBrowser;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public static PushNotificationAction writeAttributeValueAction(AttributeRef attributeRef, Object value) {
        String url = getWriteAttributeUrl(attributeRef);
        return new PushNotificationAction(url, value, true, false, WRITE_ATTRIBUTE_HTTP_METHOD);
    }
}
