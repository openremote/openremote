package org.openremote.test.model

import org.locationtech.jts.geom.Coordinate
import org.openremote.model.geo.GeoJSONMultiPolygon
import org.openremote.model.geo.GeoJSONPolygon
import org.openremote.model.util.ValueUtil
import spock.lang.Specification

class GeoJSONPolygonTest extends Specification {

    def "Simple polygon serialization and deserialization"() {
        given: "A simple rectangular polygon"
        def exteriorRing = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.6, 48.1),
            new Coordinate(16.6, 48.3),
            new Coordinate(16.2, 48.3),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        when: "Creating a GeoJSONPolygon"
        def polygon = new GeoJSONPolygon(exteriorRing)

        then: "It should be properly initialized"
        assert polygon.getCoordinates().size() == 1
        assert polygon.getExteriorRing().length == 5
        assert !polygon.hasHoles()

        when: "Serializing to JSON"
        def json = ValueUtil.asJSON(polygon).orElse(null)

        then: "JSON should be properly formatted"
        assert json != null
        assert json.contains('"type":"Polygon"')
        assert json.contains('"coordinates"')

        when: "Deserializing from JSON"
        def deserialized = ValueUtil.parse(json, GeoJSONPolygon.class).orElse(null)

        then: "Should get the same polygon"
        assert deserialized != null
        assert deserialized.getCoordinates().size() == 1
        assert deserialized.getExteriorRing().length == 5
    }

    def "Polygon with hole"() {
        given: "A polygon with a hole (donut shape)"
        def exteriorRing = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.6, 48.1),
            new Coordinate(16.6, 48.3),
            new Coordinate(16.2, 48.3),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        def hole = [
            new Coordinate(16.3, 48.15),
            new Coordinate(16.5, 48.15),
            new Coordinate(16.5, 48.25),
            new Coordinate(16.3, 48.25),
            new Coordinate(16.3, 48.15)
        ] as Coordinate[]

        when: "Creating a polygon with a hole"
        def polygon = new GeoJSONPolygon(exteriorRing, [hole] as Coordinate[][])

        then: "It should have both rings"
        assert polygon.getCoordinates().size() == 2
        assert polygon.hasHoles()
        assert polygon.getHoles().size() == 1
        assert polygon.getExteriorRing().length == 5

        when: "Serializing and deserializing"
        def json = ValueUtil.asJSON(polygon).orElse(null)
        def deserialized = ValueUtil.parse(json, GeoJSONPolygon.class).orElse(null)

        then: "Should preserve the structure"
        assert deserialized != null
        assert deserialized.getCoordinates().size() == 2
        assert deserialized.hasHoles()
        assert deserialized.getHoles().size() == 1
    }

    def "Polygon from double array constructor"() {
        given: "Raw coordinate data as double arrays"
        def coordinates = [
            [
                [16.2d, 48.1d],
                [16.6d, 48.1d],
                [16.6d, 48.3d],
                [16.2d, 48.3d],
                [16.2d, 48.1d]
            ]
        ] as double[][][]

        when: "Creating polygon from raw data"
        def polygon = new GeoJSONPolygon(coordinates)

        then: "Should be properly initialized"
        assert polygon.getCoordinates().size() == 1
        assert polygon.getExteriorRing().length == 5
        assert !polygon.hasHoles()
    }

    def "Polygon coordinate clamping"() {
        given: "Coordinates with out-of-bounds values"
        def exteriorRing = [
            new Coordinate(-200.0, -100.0),  // Out of bounds
            new Coordinate(200.0, 100.0),     // Out of bounds
            new Coordinate(16.6, 48.3),
            new Coordinate(16.2, 48.3),
            new Coordinate(-200.0, -100.0)
        ] as Coordinate[]

        when: "Creating polygon"
        def polygon = new GeoJSONPolygon(exteriorRing)

        then: "Coordinates should be clamped"
        def ring = polygon.getExteriorRing()
        assert ring[0].x == -180.0d
        assert ring[0].y == -90.0d
        assert ring[1].x == 180.0d
        assert ring[1].y == 90.0d
    }

    def "Polygon validation errors"() {
        when: "Creating polygon with too few coordinates"
        GeoJSONPolygon ignored1 = new GeoJSONPolygon([
            new Coordinate(16.2, 48.1),
            new Coordinate(16.6, 48.1),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[])

        then: "Should throw exception"
        def ex1 = thrown(IllegalArgumentException)
        assert ex1 != null

        when: "Creating polygon from empty array"
        GeoJSONPolygon ignored2 = new GeoJSONPolygon(new double[0][][])

        then: "Should throw exception"
        def ex2 = thrown(IllegalArgumentException)
        assert ex2 != null
    }

    def "Polygon with 3D coordinates"() {
        given: "Coordinates with elevation"
        def exteriorRing = [
            new Coordinate(16.2, 48.1, 200.0),
            new Coordinate(16.6, 48.1, 250.0),
            new Coordinate(16.6, 48.3, 300.0),
            new Coordinate(16.2, 48.3, 250.0),
            new Coordinate(16.2, 48.1, 200.0)
        ] as Coordinate[]

        when: "Creating and serializing polygon"
        def polygon = new GeoJSONPolygon(exteriorRing)
        def json = ValueUtil.asJSON(polygon).orElse(null)

        then: "Should include Z coordinates in JSON"
        assert json != null
        assert json.contains("200")
        assert json.contains("250")
        assert json.contains("300")

        when: "Deserializing"
        def deserialized = ValueUtil.parse(json, GeoJSONPolygon.class).orElse(null)

        then: "Should preserve Z coordinates"
        assert deserialized != null
        assert deserialized.getExteriorRing()[0].getZ() == 200.0d
        assert deserialized.getExteriorRing()[1].getZ() == 250.0d
    }
}

class GeoJSONMultiPolygonTest extends Specification {

    def "Simple MultiPolygon serialization"() {
        given: "Two separate polygons"
        def polygon1 = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.4, 48.1),
            new Coordinate(16.4, 48.2),
            new Coordinate(16.2, 48.2),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        def polygon2 = [
            new Coordinate(16.5, 48.1),
            new Coordinate(16.7, 48.1),
            new Coordinate(16.7, 48.2),
            new Coordinate(16.5, 48.2),
            new Coordinate(16.5, 48.1)
        ] as Coordinate[]

        when: "Creating MultiPolygon"
        def multiPolygon = new GeoJSONMultiPolygon([polygon1, polygon2])

        then: "Should have two polygons"
        assert multiPolygon.getPolygonCount() == 2
        assert multiPolygon.getCoordinates().size() == 2

        when: "Serializing to JSON"
        def json = ValueUtil.asJSON(multiPolygon).orElse(null)

        then: "JSON should be properly formatted"
        assert json != null
        assert json.contains('"type":"MultiPolygon"')
        assert json.contains('"coordinates"')

        when: "Deserializing"
        def deserialized = ValueUtil.parse(json, GeoJSONMultiPolygon.class).orElse(null)

        then: "Should preserve structure"
        assert deserialized != null
        assert deserialized.getPolygonCount() == 2
    }

    def "MultiPolygon from GeoJSONPolygon objects"() {
        given: "Two GeoJSONPolygon objects"
        def exteriorRing1 = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.4, 48.1),
            new Coordinate(16.4, 48.2),
            new Coordinate(16.2, 48.2),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        def exteriorRing2 = [
            new Coordinate(16.5, 48.1),
            new Coordinate(16.7, 48.1),
            new Coordinate(16.7, 48.2),
            new Coordinate(16.5, 48.2),
            new Coordinate(16.5, 48.1)
        ] as Coordinate[]

        def polygon1 = new GeoJSONPolygon(exteriorRing1)
        def polygon2 = new GeoJSONPolygon(exteriorRing2)

        when: "Creating MultiPolygon from polygons"
        def multiPolygon = new GeoJSONMultiPolygon(polygon1, polygon2)

        then: "Should have two polygons"
        assert multiPolygon.getPolygonCount() == 2
        assert multiPolygon.getPolygons().size() == 2
    }

    def "MultiPolygon with polygons with holes"() {
        given: "A polygon with a hole"
        def exteriorRing = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.6, 48.1),
            new Coordinate(16.6, 48.3),
            new Coordinate(16.2, 48.3),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        def hole = [
            new Coordinate(16.3, 48.15),
            new Coordinate(16.5, 48.15),
            new Coordinate(16.5, 48.25),
            new Coordinate(16.3, 48.25),
            new Coordinate(16.3, 48.15)
        ] as Coordinate[]

        def polygon = new GeoJSONPolygon(exteriorRing, [hole] as Coordinate[][])

        when: "Creating MultiPolygon with complex polygon"
        def multiPolygon = new GeoJSONMultiPolygon(polygon)

        then: "Should preserve polygon structure"
        assert multiPolygon.getPolygonCount() == 1
        assert multiPolygon.getPolygon(0).size() == 2  // exterior + hole

        when: "Converting back to polygons"
        def polygons = multiPolygon.getPolygons()

        then: "Should preserve holes"
        assert polygons.size() == 1
        assert polygons[0].hasHoles()
    }

    def "MultiPolygon from raw double arrays"() {
        given: "Raw coordinate data"
        def coordinates = [
            [
                [
                    [16.2d, 48.1d],
                    [16.4d, 48.1d],
                    [16.4d, 48.2d],
                    [16.2d, 48.2d],
                    [16.2d, 48.1d]
                ]
            ],
            [
                [
                    [16.5d, 48.1d],
                    [16.7d, 48.1d],
                    [16.7d, 48.2d],
                    [16.5d, 48.2d],
                    [16.5d, 48.1d]
                ]
            ]
        ] as double[][][][]

        when: "Creating MultiPolygon"
        def multiPolygon = new GeoJSONMultiPolygon(coordinates)

        then: "Should be properly initialized"
        assert multiPolygon.getPolygonCount() == 2
    }

    def "MultiPolygon validation errors"() {
        when: "Creating empty MultiPolygon"
        GeoJSONMultiPolygon ignored1 = new GeoJSONMultiPolygon(new double[0][][][])

        then: "Should throw exception"
        def ex1 = thrown(IllegalArgumentException)
        assert ex1 != null

        when: "Creating MultiPolygon with invalid polygon"
        GeoJSONMultiPolygon ignored2 = new GeoJSONMultiPolygon([
            [
                new Coordinate(16.2, 48.1),
                new Coordinate(16.4, 48.1),
                new Coordinate(16.2, 48.1)  // Only 3 coordinates
            ] as Coordinate[]
        ])

        then: "Should throw exception"
        def ex2 = thrown(IllegalArgumentException)
        assert ex2 != null
    }

    def "MultiPolygon getPolygon bounds checking"() {
        given: "A MultiPolygon with 2 polygons"
        def polygon1 = [
            new Coordinate(16.2, 48.1),
            new Coordinate(16.4, 48.1),
            new Coordinate(16.4, 48.2),
            new Coordinate(16.2, 48.2),
            new Coordinate(16.2, 48.1)
        ] as Coordinate[]

        def multiPolygon = new GeoJSONMultiPolygon([polygon1])

        when: "Accessing valid index"
        def polygon = multiPolygon.getPolygon(0)

        then: "Should return polygon"
        assert polygon != null
        assert polygon.size() == 1

        when: "Accessing invalid index"
        multiPolygon.getPolygon(5)

        then: "Should throw exception"
        thrown(IndexOutOfBoundsException)
    }
}
