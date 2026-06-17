package org.openremote.test.system

import org.openremote.model.util.VersionUtil
import spock.lang.Specification
import spock.lang.Unroll

class VersionUtilTest extends Specification {

    @Unroll
    def "compareVersions('#v1', '#v2') should return #expected"() {
        expect:
        Integer.signum(VersionUtil.compareVersions(v1, v2)) == Integer.signum(expected)

        where:
        v1          | v2          || expected
        "1.0.0"     | "1.0.0"     || 0
        "1.0.0"     | "1.0.1"     || -1
        "1.0.1"     | "1.0.0"     || 1
        "1.0"       | "1.0.0"     || 0
        "1.0.0"     | "1.0"       || 0
        "2.0.0"     | "1.9.9"     || 1
        "1.9.9"     | "2.0.0"     || -1
        "1.2.3"     | "1.2.3.4"   || -1
        "1.2.3.4"   | "1.2.3"     || 1
        "10.0.0"    | "2.0.0"     || 1
        "2.0.0"     | "10.0.0"    || -1
        "1.0"       | "1.0.0.0"   || 0
    }

    @Unroll
    def "compareVersions with qualifiers: '#v1' vs '#v2'"() {
        expect:
        Integer.signum(VersionUtil.compareVersions(v1, v2)) == Integer.signum(expected)

        where:
        v1              | v2              || expected
        "1.0.0"         | "1.0.0-SNAPSHOT"|| 0  // numeric parts are equal
        "1.1.0-SNAPSHOT"| "1.0.0"         || 1  // 1.1 > 1.0
        "2.0-beta"      | "1.9"           || 1  // 2 > 1
        "1.0-alpha"     | "1.0-beta"      || 0  // both parse to 1.0
    }

    def "compareVersions should handle empty segments"() {
        expect:
        VersionUtil.compareVersions("1..0", "1.0.0") == 0
        VersionUtil.compareVersions("1.0.", "1.0.0") == 0
    }

    def "compareVersions should throw NullPointerException for null inputs"() {
        when:
        VersionUtil.compareVersions(null, "1.0.0")

        then:
        thrown(NullPointerException)

        when:
        VersionUtil.compareVersions("1.0.0", null)

        then:
        thrown(NullPointerException)
    }

    def "isVersionGreater should work correctly"() {
        expect:
        VersionUtil.isVersionGreater("2.0.0", "1.0.0") == true
        VersionUtil.isVersionGreater("1.0.0", "2.0.0") == false
        VersionUtil.isVersionGreater("1.0.0", "1.0.0") == false
    }

    def "isVersionGreaterOrEqual should work correctly"() {
        expect:
        VersionUtil.isVersionGreaterOrEqual("2.0.0", "1.0.0") == true
        VersionUtil.isVersionGreaterOrEqual("1.0.0", "1.0.0") == true
        VersionUtil.isVersionGreaterOrEqual("1.0.0", "2.0.0") == false
    }

    def "isVersionEqual should work correctly"() {
        expect:
        VersionUtil.isVersionEqual("1.0.0", "1.0.0") == true
        VersionUtil.isVersionEqual("1.0", "1.0.0") == true
        VersionUtil.isVersionEqual("1.0.0", "1.0.1") == false
    }

    def "isVersionLess should work correctly"() {
        expect:
        VersionUtil.isVersionLess("1.0.0", "2.0.0") == true
        VersionUtil.isVersionLess("2.0.0", "1.0.0") == false
        VersionUtil.isVersionLess("1.0.0", "1.0.0") == false
    }

    def "isVersionLessOrEqual should work correctly"() {
        expect:
        VersionUtil.isVersionLessOrEqual("1.0.0", "2.0.0") == true
        VersionUtil.isVersionLessOrEqual("1.0.0", "1.0.0") == true
        VersionUtil.isVersionLessOrEqual("2.0.0", "1.0.0") == false
    }

    def "parsePart should handle various formats"() {
        expect:
        VersionUtil.parsePart("123") == 123
        VersionUtil.parsePart("1-SNAPSHOT") == 1
        VersionUtil.parsePart("42-alpha") == 42
        VersionUtil.parsePart("0") == 0
        VersionUtil.parsePart("") == 0
        VersionUtil.parsePart("beta") == 0
    }
}

