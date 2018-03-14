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
package org.openremote.app.client.apps;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.openremote.app.client.polymer.ViewComponent;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.model.apps.ConsoleApp;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "openremote.manager.ConsoleAppsView")
public abstract class ConsoleAppsView extends ViewComponent {

    public static String is;

    @JsType
    interface Presenter {

        ManagerMessages messages();

        void onAppSelected(String realm);
    }

    native void setPresenter(Presenter presenter);

    native void setApps(ConsoleApp[] apps);

    native void openAppUrl(String realm, String appUrl);

    /* TODO Tricks

        List<TodoItem> list = new ArrayList<TodoItem>() {{
            add(new TodoItem(System.currentTimeMillis(), "I need to do A", false));
            add(new TodoItem(System.currentTimeMillis(), "I need to do B", true));
            add(new TodoItem(System.currentTimeMillis(), "I need to do C", false));
        }};
        TodoItem[] array = list.toArray(new TodoItem[list.size()]);

        element.set("todoItems", Js.asAny(array));

        ObjectValue foo = Values.createObject();
        foo.put("bar", Values.create("BAR"));
        foo.put("bbb", Values.create(true));
        foo.put("baz", Values.createArray().add(Values.create("baz1")).add(Values.create("baz2")).add(Values.create("baz3")));

        // Conversion bidirectional possible
        Any fooAny = foo.asAny();
        foo = Values.getObject(Values.fromAny(fooAny).get()).get();

        element.set("foo", foo.asAny());
    */
}
