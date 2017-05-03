package org.openremote.test.model

import org.openremote.model.value.ArrayValue
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Values
import spock.lang.Specification

class ModelValueTest extends Specification {

    def "Read and write JSON"() {
        expect:
        ObjectValue sampleObject1 = Values.createObject()
        sampleObject1.put("object1A", "O1-AAA")
        sampleObject1.put("object1B", 456)

        ArrayValue sampleArray1 = Values.createArray()
        sampleArray1.set(0, "A0")
        sampleArray1.set(1, "A1")
        sampleArray1.set(2, true)
        sampleArray1.set(3, 123.45)
        sampleArray1.set(4, sampleObject1)

        String rawValue = sampleArray1.toJson()
        println rawValue
        ArrayValue parsedValue = Values.<ArrayValue>parse(rawValue).get()
        parsedValue == sampleArray1
    }

    def "Compare scalar values"() {
        expect:
        Values.create(true) == Values.create(true)
        Values.create(true).hashCode() == Values.create(true).hashCode()

        Values.create(false) != Values.create(true)
        Values.create(false).hashCode() != Values.create(true).hashCode()

        Values.create(true) != Values.create(false)
        Values.create(true).hashCode() != Values.create(false).hashCode()

        Values.create(123) != Values.create(false)
        Values.create(123).hashCode() != Values.create(false).hashCode()

        Values.create(0) != Values.create(false)
        Values.create(0).hashCode() != Values.create(false).hashCode()

        Values.create(1) != Values.create(true)
        Values.create(1).hashCode() != Values.create(true).hashCode()

        Values.create(123) == Values.create(123)
        Values.create(123).hashCode() == Values.create(123).hashCode()

        Values.create(123) != Values.create(456)
        Values.create(123).hashCode() != Values.create(456).hashCode()

        Values.create(456) != Values.create(123)
        Values.create(456).hashCode() != Values.create(123).hashCode()

        Values.create(0) != Values.create("")
        Values.create(0).hashCode() != Values.create("").hashCode()

        Values.create("abc") == Values.create("abc")
        Values.create("abc").hashCode() == Values.create("abc").hashCode()

        Values.create("abcd") != Values.create("abc")
        Values.create("abcd").hashCode() != Values.create("abc").hashCode()

        Values.create("abc") != Values.create("abcd")
        Values.create("abc").hashCode() != Values.create("abcd").hashCode()

        Values.create("abc") != Values.create(123)
        Values.create("abc").hashCode() != Values.create(123).hashCode()

        Values.create("") != Values.create(0)
        Values.create("").hashCode() != Values.create(0).hashCode()
    }

    def "Compare non-scalar Json values"() {

        given:
        ObjectValue sampleObject1 = Values.createObject()
        sampleObject1.put("object1A", "O1-AAA")
        sampleObject1.put("object1B", 456)

        ObjectValue sampleObject2 = Values.createObject()
        sampleObject2.put("object2A", "O2-AAA")
        sampleObject2.put("object2B", 789)

        ArrayValue sampleArray1 = Values.createArray()
        sampleArray1.set(0, "A0")
        sampleArray1.set(1, "A1")
        sampleArray1.set(2, sampleObject1)

        ArrayValue sampleArray2 = Values.createArray()
        sampleArray1.set(0, sampleObject1)
        sampleArray1.set(1, "A1")
        sampleArray1.set(2, "A0")

        expect: "Array equality comparisons to be correct"
        Values.createArray() == Values.createArray()
        Values.createArray().hashCode() == Values.createArray().hashCode()

        sampleArray1 == sampleArray1
        sampleArray1.hashCode() == sampleArray1.hashCode()

        sampleArray2 != sampleArray1
        sampleArray2.hashCode() != sampleArray1.hashCode()

        sampleArray1 != sampleArray2
        sampleArray1.hashCode() != sampleArray2.hashCode()

        and: "Objects with no fields to be equal"
        Values.createObject() == Values.createObject()
        Values.createObject().hashCode() == Values.createObject().hashCode()

        Values.createArray() != Values.createObject()
        Values.createArray().hashCode() != Values.createObject().hashCode()

        and: "Objects with the same fields to be equal"
        ObjectValue sameFields1 = Values.createObject()
        sameFields1.put("fieldA", "AAA")
        sameFields1.put("fieldB", 123)
        sameFields1.put("fieldC", true)
        sameFields1.put("fieldD", sampleArray1)
        ObjectValue sameFields2 = Values.createObject()
        sameFields2.put("fieldD", sampleArray1)
        sameFields2.put("fieldC", true)
        sameFields2.put("fieldB", 123)
        sameFields2.put("fieldA", "AAA")
        sameFields1 == sameFields2
        sameFields1.hashCode() == sameFields2.hashCode()

        and: "Objects with the different fields to be not equal"
        ObjectValue otherFields1 = Values.createObject()
        otherFields1.put("fieldA", "AAA")
        ObjectValue otherFields2 = Values.createObject()
        otherFields2.put("fieldB", 123)
        otherFields1 != otherFields2
        otherFields1.hashCode() != otherFields2.hashCode()

        and: "Objects with the different field values to be not equal"
        ObjectValue differentValues1 = Values.createObject()
        differentValues1.put("fieldA", "AAA")
        ObjectValue differentValues2 = Values.createObject()
        differentValues2.put("fieldA", "AAAAA")
        differentValues1 != differentValues2
        differentValues1.hashCode() != differentValues2.hashCode()
    }

}
