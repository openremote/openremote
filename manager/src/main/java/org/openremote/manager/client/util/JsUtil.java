package org.openremote.manager.client.util;

public class JsUtil {

    public native static void log(Object o) /*-{
        console.dir(o);
    }-*/;

    public native static void printType(Object o) /*-{
        console.log(({}).toString.call(o).match(/\s([a-zA-Z]+)/)[1].toLowerCase());
    }-*/;

}
