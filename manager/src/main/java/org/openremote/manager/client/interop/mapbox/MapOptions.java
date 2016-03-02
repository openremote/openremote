package org.openremote.manager.client.interop.mapbox;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
 public class MapOptions {
    @JsProperty
    public boolean touchZoomRotate;

    @JsProperty
    public int zoom;

    @JsProperty
    public int minZoom;

    @JsProperty
    public int maxZoom;

    @JsProperty
    public JsonObject style;

    @JsProperty
    public boolean hash;

    @JsProperty
    public boolean interactive;

    @JsProperty
    public int bearingSnap;

    @JsProperty
    public JsonArray classes;

    @JsProperty
    public boolean attributionControl;

    @JsProperty
    public String container;

    @JsProperty
    public boolean preserveDrawingBuffer;

    @JsProperty
    public LngLatBounds maxBounds;

    @JsProperty
    public boolean scrollZoom;

    @JsProperty
    public boolean boxZoom;

    @JsProperty
    public boolean dragRotate;

    @JsProperty
    public boolean dragPan;

    @JsProperty
    public boolean keyboard;

    @JsProperty
    public boolean doubleclickZoom;

    @JsProperty
    public boolean failIfMajorPerformanceCaveat;

//    @JsProperty
//    boolean getTouchZoomRotate();
//    @JsProperty
//    void setTouchZoomRotate(boolean value);
//
//    @JsProperty
//    int getZoom();
//    @JsProperty
//    void setZoom(int value);
//
//    @JsProperty
//    int getMinZoom();
//    @JsProperty
//    void setMinZoom(int value);
//
//    @JsProperty
//    int getMaxZoom();
//    @JsProperty
//    void setMaxZoom(int value);
//
//    @JsProperty
//    JsonObject getStyle();
//    @JsProperty
//    void setStyle(JsonObject value);
//
//    @JsProperty
//    boolean getHash();
//    @JsProperty
//    void setHash(boolean value);
//
//    @JsProperty
//    boolean getInteractive();
//    @JsProperty
//    void setInteractive(boolean value);
//
//    @JsProperty
//    int getBearingSnap();
//    @JsProperty
//    void setBearingSnap(int value);
//
//    @JsProperty
//    JsonArray getClasses();
//    @JsProperty
//    void setClasses(JsonArray value);
//
//    @JsProperty
//    boolean getAttributionControl();
//    @JsProperty
//    void setAttributionControl(boolean value);
//
//    @JsProperty
//    String getContainer();
//    @JsProperty
//    void setContainer(String value);
//
//    @JsProperty
//    boolean getPreserveDrawingBuffer();
//    @JsProperty
//    void setPreserveDrawingBuffer(boolean value);
//
//    @JsProperty
//    LngLatBounds getMaxBounds();
//    @JsProperty
//    void setMaxBounds(LngLatBounds value);
//
//    @JsProperty
//    boolean getScrollZoom();
//    @JsProperty
//    void setScrollZoom(boolean value);
//
//    @JsProperty
//    boolean getBoxZoom();
//    @JsProperty
//    void setBoxZoom(boolean value);
//
//    @JsProperty
//    boolean getDragRotate();
//    @JsProperty
//    void setDragRotate(boolean value);
//
//    @JsProperty
//    boolean getDragPan();
//    @JsProperty
//    void setDragPen(boolean value);
//
//    @JsProperty
//    boolean getKeyboard();
//    @JsProperty
//    void setKeyboard(boolean value);
//
//    @JsProperty
//    boolean getDoubleclickZoom();
//    @JsProperty
//    void setDoubleClickZoom(boolean value);
//
//    @JsProperty
//    boolean getFailIfMajorPerformanceCaveat();
//    @JsProperty
//    void setFailIfMajorPerformanceCaveat(boolean value);
}
