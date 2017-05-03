package org.openremote.test.json

import elemental.json.Json
import elemental.json.JsonArray
import elemental.json.JsonObject
import elemental.json.JsonValue
import spock.lang.Specification

import org.openremote.model.util.JsonUtil

class JsonEqualityTest extends Specification {

    def "Compare scalar Json values"() {
        expect:
        JsonUtil.equals(Json.create(true), Json.create(true))
        JsonUtil.hashCode(Json.create(true)) == JsonUtil.hashCode(Json.create(true))

        !JsonUtil.equals(Json.create(false), Json.create(true))
        JsonUtil.hashCode(Json.create(false)) != JsonUtil.hashCode(Json.create(true))

        !JsonUtil.equals(Json.create(true), Json.create(false))
        JsonUtil.hashCode(Json.create(true)) != JsonUtil.hashCode(Json.create(false))

        !JsonUtil.equals(Json.create(123), Json.create(false))
        JsonUtil.hashCode(Json.create(123)) != JsonUtil.hashCode(Json.create(false))

        !JsonUtil.equals(Json.create(0), Json.create(false))
        JsonUtil.hashCode(Json.create(0)) != JsonUtil.hashCode(Json.create(false))

        !JsonUtil.equals(Json.create(1), Json.create(true))
        JsonUtil.hashCode(Json.create(1)) != JsonUtil.hashCode(Json.create(true))

        JsonUtil.equals(null, null)
        JsonUtil.hashCode(null) == JsonUtil.hashCode(null)

        !JsonUtil.equals(null, Json.create(123))
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.create(123))

        !JsonUtil.equals(null, Json.create(false))
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.create(false))

        !JsonUtil.equals(null, Json.create(true))
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.create(true))

        !JsonUtil.equals(null, Json.create("abc"))
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.create("abc"))

        JsonUtil.equals(Json.create(123), Json.create(123))
        JsonUtil.hashCode(Json.create(123)) == JsonUtil.hashCode(Json.create(123))

        !JsonUtil.equals(Json.create(123), Json.create(456))
        JsonUtil.hashCode(Json.create(123)) != JsonUtil.hashCode(Json.create(456))

        !JsonUtil.equals(Json.create(456), Json.create(123))
        JsonUtil.hashCode(Json.create(456)) != JsonUtil.hashCode(Json.create(123))

        !JsonUtil.equals(Json.create(0), Json.create(""))
        JsonUtil.hashCode(Json.create(0)) != JsonUtil.hashCode(Json.create(""))

        JsonUtil.equals(Json.create("abc"), Json.create("abc"))
        JsonUtil.hashCode(Json.create("abc")) == JsonUtil.hashCode(Json.create("abc"))

        !JsonUtil.equals(Json.create("abcd"), Json.create("abc"))
        JsonUtil.hashCode(Json.create("abcd")) != JsonUtil.hashCode(Json.create("abc"))

        !JsonUtil.equals(Json.create("abc"), Json.create("abcd"))
        JsonUtil.hashCode(Json.create("abc")) != JsonUtil.hashCode(Json.create("abcd"))

        !JsonUtil.equals(Json.create("abc"), null)
        JsonUtil.hashCode(Json.create("abc")) != JsonUtil.hashCode(null)

        !JsonUtil.equals(Json.create("abc"), Json.create(123))
        JsonUtil.hashCode(Json.create("abc")) != JsonUtil.hashCode(Json.create(123))

        !JsonUtil.equals(Json.create(""), Json.create(0))
        JsonUtil.hashCode(Json.create("")) != JsonUtil.hashCode(Json.create(0))
    }

    def "Compare non-scalar Json values"() {

        given:
        JsonObject sampleObject1 = Json.createObject()
        sampleObject1.put("object1A", "O1-AAA")
        sampleObject1.put("object1B", 456)

        JsonObject sampleObject2 = Json.createObject()
        sampleObject2.put("object2A", "O2-AAA")
        sampleObject2.put("object2B", 789)

        JsonArray sampleArray1 = Json.createArray()
        sampleArray1.set(0, "A0")
        sampleArray1.set(1, "A1")
        sampleArray1.set(2, sampleObject1)

        JsonArray sampleArray2 = Json.createArray()
        sampleArray1.set(0, sampleObject1)
        sampleArray1.set(1, "A1")
        sampleArray1.set(2, "A0")

        expect: "Array equality comparisons to be correct"
        JsonUtil.equals(Json.createArray(), Json.createArray())
        JsonUtil.hashCode(Json.createArray()) == JsonUtil.hashCode(Json.createArray())

        !JsonUtil.equals(Json.createArray(), null)
        JsonUtil.hashCode(Json.createArray()) != JsonUtil.hashCode(null)

        JsonUtil.equals(sampleArray1, sampleArray1)
        JsonUtil.hashCode(sampleArray1) == JsonUtil.hashCode(sampleArray1)

        !JsonUtil.equals(sampleArray2, sampleArray1)
        JsonUtil.hashCode(sampleArray2) != JsonUtil.hashCode(sampleArray1)

        !JsonUtil.equals(sampleArray1, sampleArray2)
        JsonUtil.hashCode(sampleArray1) != JsonUtil.hashCode(sampleArray2)

        and: "Objects with no fields to be equal"
        JsonUtil.hashCode(null) == JsonUtil.hashCode(null)

        JsonUtil.equals(Json.createObject(), Json.createObject())
        JsonUtil.hashCode(Json.createObject()) == JsonUtil.hashCode(Json.createObject())

        !JsonUtil.equals(null, Json.createObject())
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.createObject())

        !JsonUtil.equals(null, Json.createObject())
        JsonUtil.hashCode(null) != JsonUtil.hashCode(Json.createObject())

        !JsonUtil.equals(Json.createArray(), Json.createObject())
        JsonUtil.hashCode(Json.createArray()) != JsonUtil.hashCode(Json.createObject())

        and: "Objects with the same fields to be equal"
        JsonObject sameFields1 = Json.createObject()
        sameFields1.put("fieldA", "AAA")
        sameFields1.put("fieldB", 123)
        sameFields1.put("fieldC", true)
        sameFields1.put("fieldD", sampleArray1)
        JsonObject sameFields2 = Json.createObject()
        sameFields2.put("fieldD", sampleArray1)
        sameFields2.put("fieldC", true)
        sameFields2.put("fieldB", 123)
        sameFields2.put("fieldA", "AAA")
        JsonUtil.equals(sameFields1, sameFields2)
        JsonUtil.hashCode(sameFields1) == JsonUtil.hashCode(sameFields2)

        and: "Objects with the different fields to be not equal"
        JsonObject otherFields1 = Json.createObject()
        otherFields1.put("fieldA", "AAA")
        JsonObject otherFields2 = Json.createObject()
        otherFields2.put("fieldB", 123)
        !JsonUtil.equals(otherFields1, otherFields2)
        JsonUtil.hashCode(otherFields1) != JsonUtil.hashCode(otherFields2)

        and: "Objects with the different field values to be not equal"
        JsonObject differentValues1 = Json.createObject()
        differentValues1.put("fieldA", "AAA")
        JsonObject differentValues2  = Json.createObject()
        differentValues2.put("fieldA", "AAAAA")
        !JsonUtil.equals(differentValues1, differentValues2)
        JsonUtil.hashCode(differentValues1) != JsonUtil.hashCode(differentValues2)
    }
}