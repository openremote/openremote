package org.openremote.model.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GeoJSONPointTest {

    @Test
    public void parseRawLocationReturnsNullForInvalidInput() {
        assertNull(GeoJSONPoint.parseRawLocation(null));
        assertNull(GeoJSONPoint.parseRawLocation(""));
        assertNull(GeoJSONPoint.parseRawLocation("1,2,3"));
        assertNull(GeoJSONPoint.parseRawLocation("abc,2"));
        assertNull(GeoJSONPoint.parseRawLocation("1,def"));
    }

    @Test
    public void parseRawLocationParsesValidInput() {
        GeoJSONPoint point = GeoJSONPoint.parseRawLocation("12.5, -3.75");
        assertNotNull(point);
        assertEquals(12.5d, point.getX());
        assertEquals(-3.75d, point.getY());
    }

    @Test
    public void offsetByMetersOffsetsEastAndNorth() {
        GeoJSONPoint origin = new GeoJSONPoint(0d, 0d);
        GeoJSONPoint offset = origin.offsetByMeters(1000d, 1000d);
        double expectedLat = 0.0090436947d;
        double expectedLon = 0.0089831528d;
        assertEquals(expectedLat, offset.getY(), 1e-9);
        assertEquals(expectedLon, offset.getX(), 1e-9);
    }
}
