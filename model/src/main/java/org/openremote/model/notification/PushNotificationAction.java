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
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import static org.openremote.model.asset.AssetResource.Util.WRITE_ATTRIBUTE_HTTP_METHOD;
import static org.openremote.model.asset.AssetResource.Util.getWriteAttributeUrl;


public class PushNotificationAction {

    protected String url;
    protected String httpMethod;
    protected Value data;
    protected boolean silent;
    protected boolean openInBrowser; // For app based consoles (i.e. Android and iOS)

    public PushNotificationAction(String url) {
        this.url = url;
    }

    @JsonCreator
    public PushNotificationAction(@JsonProperty("url") String url,
                                  @JsonProperty("data") Value data,
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

    public Value getData() {
        return data;
    }

    public void setData(Value data) {
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

    public ObjectValue toValue() {
        ObjectValue val = Values.createObject();
        if (!TextUtil.isNullOrEmpty(url)) {
            val.put("url", url);
        }
        if (!TextUtil.isNullOrEmpty(httpMethod)) {
            val.put("httpMethod", httpMethod);
        }
        val.put("silent", silent);
        val.put("openInBrowser", openInBrowser);
        if (data != null) {
            val.put("data", data);
        }
        return val;
    }

    public static PushNotificationAction writeAttributeValueAction(AttributeRef attributeRef, Value value) {
        String url = getWriteAttributeUrl(attributeRef);
        return new PushNotificationAction(url, value, true, false, WRITE_ATTRIBUTE_HTTP_METHOD);
    }

    public static PushNotificationAction fromValue(ObjectValue value) {
        if (value == null) {
            return null;
        }

        return new PushNotificationAction(
            value.getString("url").orElse(null),
            value.get("data").orElse(null),
            value.getBoolean("silent").orElse(false),
            value.getBoolean("openInBrowser").orElse(false),
            value.getString("httpMethod").orElse(null)
        );
    }
}
