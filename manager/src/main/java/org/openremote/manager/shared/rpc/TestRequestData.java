package org.openremote.manager.shared.rpc;

import jsinterop.annotations.JsType;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * TODO https://issues.jboss.org/browse/RESTEASY-1315
 */
@JsType
@Path("test/{zoom}/{column}/{row}")
public class TestRequestData extends RequestData {
    @PathParam("zoom")
    public int zoom;

    @PathParam("column")
    public int column;

    @PathParam("row")
    public int row;
}
