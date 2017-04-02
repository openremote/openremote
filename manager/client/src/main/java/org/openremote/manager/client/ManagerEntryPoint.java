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
package org.openremote.manager.client;

import com.google.gwt.core.client.GWT;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;
import org.openremote.manager.client.util.JsUtil;

import java.util.logging.Logger;

public class ManagerEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    private static final Logger LOG = Logger.getLogger(ManagerEntryPoint.class.getName());

    @Override
    public void onModuleLoad() {

        //JsonArray array = (JsonArray) Json.parse("[0, false]");

//        printJsonValue(array.get(0));
/*
        if (array.get(0).asNumber() < 0)
            throw new IllegalStateException("Should not be negative");

        if (array.get(0) == null)
            throw new IllegalStateException("This is wrong!");

        if (array.get(1) == null)
            throw new IllegalStateException("This is also wrong!");
*/

        injector.getAppController().start();

        /*
        TODO Elemental experiments...https://github.com/gwtproject/gwt/issues/9484
        JsonObject jsonObject = Json.createObject();
        AbstractValueHolder abstractValueHolder = new AbstractValueHolder(jsonObject) {};
        jsonObject.put("value", true);
        LOG.info("### GOT: " + jsonObject.getBoolean("value"));
        LOG.info("### GOT: " + jsonObject.get("value").asBoolean());
        LOG.info("### GOT: " + abstractValueHolder.getValueAsBoolean());
        */
        /*
       JsonArray array = (JsonArray) Json.parse("[false]");

        if (array.get(0).asNumber() < 0)
            throw new IllegalStateException("Should not be negative");

        if (array.get(0) == null)
            throw new IllegalStateException("This is wrong!");
        */

    }

    public void printJsonValue(JsonValue jsonValue) {
        LOG.info("### GOT: " + jsonValue);
    }
}
