package org.openremote.test.system

import org.openremote.manager.system.VersionInfo
import spock.lang.Specification
import spock.lang.Unroll

class VersionInfoTest extends Specification {

    @Unroll
    def "compareVersions('#v1', '#v2') should return #expected"() {
        expect:
        Integer.signum(VersionInfo.compareVersions(v1, v2)) == Integer.signum(expected)

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
        Integer.signum(VersionInfo.compareVersions(v1, v2)) == Integer.signum(expected)

        where:
        v1              | v2              || expected
        "1.0.0"         | "1.0.0-SNAPSHOT"|| 0  // numeric parts are equal
        "1.1.0-SNAPSHOT"| "1.0.0"         || 1  // 1.1 > 1.0
        "2.0-beta"      | "1.9"           || 1  // 2 > 1
        "1.0-alpha"     | "1.0-beta"      || 0  // both parse to 1.0
    }

    def "compareVersions should handle empty segments"() {
        expect:
        VersionInfo.compareVersions("1..0", "1.0.0") == 0
        VersionInfo.compareVersions("1.0.", "1.0.0") == 0
    }

    def "compareVersions should throw NullPointerException for null inputs"() {
        when:
        VersionInfo.compareVersions(null, "1.0.0")

        then:
        thrown(NullPointerException)

        when:
        VersionInfo.compareVersions("1.0.0", null)

        then:
        thrown(NullPointerException)
    }

    def "isVersionGreater should work correctly"() {
        expect:
        VersionInfo.isVersionGreater("2.0.0", "1.0.0") == true
        VersionInfo.isVersionGreater("1.0.0", "2.0.0") == false
        VersionInfo.isVersionGreater("1.0.0", "1.0.0") == false
    }

    def "isVersionGreaterOrEqual should work correctly"() {
        expect:
        VersionInfo.isVersionGreaterOrEqual("2.0.0", "1.0.0") == true
        VersionInfo.isVersionGreaterOrEqual("1.0.0", "1.0.0") == true
        VersionInfo.isVersionGreaterOrEqual("1.0.0", "2.0.0") == false
    }

    def "isVersionEqual should work correctly"() {
        expect:
        VersionInfo.isVersionEqual("1.0.0", "1.0.0") == true
        VersionInfo.isVersionEqual("1.0", "1.0.0") == true
        VersionInfo.isVersionEqual("1.0.0", "1.0.1") == false
    }

    def "isVersionLess should work correctly"() {
        expect:
        VersionInfo.isVersionLess("1.0.0", "2.0.0") == true
        VersionInfo.isVersionLess("2.0.0", "1.0.0") == false
        VersionInfo.isVersionLess("1.0.0", "1.0.0") == false
    }

    def "isVersionLessOrEqual should work correctly"() {
        expect:
        VersionInfo.isVersionLessOrEqual("1.0.0", "2.0.0") == true
        VersionInfo.isVersionLessOrEqual("1.0.0", "1.0.0") == true
        VersionInfo.isVersionLessOrEqual("2.0.0", "1.0.0") == false
    }

    def "parsePart should handle various formats"() {
        expect:
        VersionInfo.parsePart("123") == 123
        VersionInfo.parsePart("1-SNAPSHOT") == 1
        VersionInfo.parsePart("42-alpha") == 42
        VersionInfo.parsePart("0") == 0
        VersionInfo.parsePart("") == 0
        VersionInfo.parsePart("beta") == 0
    }
}

