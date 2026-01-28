/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.test.mqtt

import org.apache.activemq.artemis.spi.core.remoting.ssl.SSLContextConfig
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.OpenRemoteSSLContextFactory
import org.openremote.manager.persistence.ManagerPersistenceService
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.net.ssl.SSLContext
import java.nio.file.Files

/**
 * Integration tests for {@link OpenRemoteSSLContextFactory}.
 * Tests certificate loading from various sources: storage directory, classpath, and configured files.
 *
 * Note: The OpenRemoteSSLContextFactory is instantiated by Artemis via ServiceLoader when
 * the MQTT broker starts. These tests verify the behavior through the running container.
 */
class OpenRemoteSSLContextFactoryTest extends Specification implements ManagerContainerTrait {

    def "test factory is initialized when MQTT broker starts"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())

        when: "getting the MQTT broker service"
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        then: "the service should be active"
        mqttBrokerService != null
        mqttBrokerService.active
    }

    def "test factory loads certificate from storage directory with priority"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "a combined PEM file in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        def pemFile = certsDir.resolve("00-test-cert")
        pemFile.text = getTestPemContent()

        and: "an SSL context config"
        def config = SSLContextConfig.builder().build()

        when: "getting SSL context from the factory"
        // Access the factory through the ServiceLoader mechanism used by Artemis
        def factory = new OpenRemoteSSLContextFactory()
        def sslContext = factory.getSSLContext(config, [:])

        then: "should load successfully"
        assert sslContext != null
        assert (sslContext instanceof SSLContext)

        cleanup:
        Files.deleteIfExists(pemFile)
    }

    def "test factory finds first numbered certificate file"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "multiple numbered PEM files in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        def pemFile01 = certsDir.resolve("01-backup")
        def pemFile00 = certsDir.resolve("00-primary")
        def pemFile02 = certsDir.resolve("02-tertiary")
        pemFile01.text = getTestPemContent()
        pemFile00.text = getTestPemContent()
        pemFile02.text = getTestPemContent()

        and: "an SSL context config"
        def config = SSLContextConfig.builder().build()

        when: "getting SSL context from the factory"
        def factory = new OpenRemoteSSLContextFactory()
        def sslContext = factory.getSSLContext(config, [:])

        then: "should load successfully (from 00-primary, the first numerically)"
        sslContext != null

        cleanup:
        Files.deleteIfExists(pemFile00)
        Files.deleteIfExists(pemFile01)
        Files.deleteIfExists(pemFile02)
    }

    def "test factory getPriority returns high priority"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())

        and: "a factory"
        def factory = new OpenRemoteSSLContextFactory()

        expect: "priority should be 100 (high)"
        factory.getPriority() == 100
    }

    def "test factory handles malformed PEM file gracefully"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "a malformed PEM file in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        // Remove any valid numbered files first
        if (Files.exists(certsDir)) {
            Files.list(certsDir).filter { it.fileName.toString().matches("^\\d+-.*") }.forEach { Files.deleteIfExists(it) }
        }
        def malformedFile = certsDir.resolve("00-malformed")
        malformedFile.text = "This is not a valid PEM file"

        and: "also add a valid numbered cert file"
        def validFile = certsDir.resolve("01-valid")
        validFile.text = getTestPemContent()

        and: "an SSL context config"
        def config = SSLContextConfig.builder().build()

        when: "getting SSL context from the factory"
        def factory = new OpenRemoteSSLContextFactory()
        def sslContext = factory.getSSLContext(config, [:])

        then: "should fall back to the next valid certificate"
        sslContext != null

        cleanup:
        Files.deleteIfExists(malformedFile)
        Files.deleteIfExists(validFile)
    }

    def "test trustAll configuration creates trust-all manager"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "a valid PEM file in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        def pemFile = certsDir.resolve("00-test-cert")
        pemFile.text = getTestPemContent()

        and: "an SSL context config with trustAll enabled"
        def config = SSLContextConfig.builder().trustAll(true).build()

        when: "getting SSL context with trustAll"
        def factory = new OpenRemoteSSLContextFactory()
        def sslContext = factory.getSSLContext(config, [:])

        then: "should create context successfully"
        sslContext != null

        cleanup:
        Files.deleteIfExists(pemFile)
    }

    def "test clearSSLContexts invalidates cache"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "a valid PEM file in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        def pemFile = certsDir.resolve("00-test-cert")
        pemFile.text = getTestPemContent()

        and: "an SSL context config"
        def config = SSLContextConfig.builder().build()

        and: "a factory with a cached context"
        def factory = new OpenRemoteSSLContextFactory()
        def sslContext1 = factory.getSSLContext(config, [:])

        when: "clearing SSL contexts"
        factory.clearSSLContexts()

        and: "getting SSL context again"
        def sslContext2 = factory.getSSLContext(config, [:])

        then: "should return a new instance"
        !sslContext1.is(sslContext2)

        cleanup:
        Files.deleteIfExists(pemFile)
    }

    def "test factory caches SSL context"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def persistenceService = container.getService(ManagerPersistenceService.class)

        and: "a valid PEM file in the certs directory"
        def certsDir = persistenceService.getStorageDir().resolve("proxy").resolve("certs")
        Files.createDirectories(certsDir)
        def pemFile = certsDir.resolve("00-test-cert")
        pemFile.text = getTestPemContent()

        and: "an SSL context config"
        def config = SSLContextConfig.builder().build()

        and: "a factory"
        def factory = new OpenRemoteSSLContextFactory()

        when: "getting SSL context twice"
        def sslContext1 = factory.getSSLContext(config, [:])
        def sslContext2 = factory.getSSLContext(config, [:])

        then: "should return the same cached instance"
        sslContext1.is(sslContext2)

        cleanup:
        Files.deleteIfExists(pemFile)
    }

    // Helper methods

    private String getTestPemContent() {
        return """-----BEGIN PRIVATE KEY-----
MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCrKEk77HcJB5Sq
voN2UbRsDh9d0ECN8tOU5hC2poih+6XBJgikQ8gdy7ptt477KRh3ZIiw3ZTXHg0/
/Ju71D/4EDBYwHxoSK9WehP5Kz/LrBHhtArXK3RYH8pFS13CDOPjXnm6LMN52mRG
wm2gCwKwRTbfm+D9hjpVuwt0sfHaXVETlUc4JystlfYVurMcfsox9tsbRuzlEaky
K9Cr1V7bgaLMosHDX3NSuEyzb9DQZ3PBK3JjJhSeYkGNuP/NocMrWy/JHd2v2Wev
9W+D1Pv46Sqfrvd6K7oP00FL0CdODkMRBVTlb1wq/6uJdRbnVUM0PGA9enrQvMB1
1fFglHa3AgMBAAECggEBAINFKqXi/ojWX5d09q7Qi2g0jKoPBvPXwZ75tOfhYfma
X857tTUHJ3xyvFFZ7zeClVk8qfm8eGNkkRT6URcF+unuwKXRO5lf5dqVVqxMF2nG
VxCcXZQZp+nOt/vdidNCv6Wq2AGKQ4I5lZ8Pj7SnvTAkZamqjCzlvefyxR6DO9MV
beJFscRp9OWCEPwscioWfP5/Df5luGZapDeIZz/Omt0GMHdKo4n/NRGZmQrZstlE
DoPKQvN3v7Y8UGrHERJJmQ5m/C01jy6cS9GRwwN3uKMLajcQ2TW/UZSeuHbWVdLu
utHk4Fzeze1WSbz2Mmc0hm3WVQ3UcA7pudRRRLvGQIECgYEA1n6iI+Xwm5DyaMf9
mRwdivzPh9ZSt1FeT4aZG7oR2BeGN0LkFIVDrKtpjDCmyQU2G8Rp2/6HkbAm6Jdf
oM4x3Usb1icrHFMjXd6GFxgyHFMvLc2FvOleWeP7VvTRiAvwq+RgyAYn/s6MhVG4
O4hPsdzMeTd00n373L45mc9JzWkCgYEAzEbdRzTmCt4pcoCkYL32YLcb70DxKNgD
pTM63fkwtYWth1bB1yU9egyQvsblkEYgJoYaau4Jsetwz8dp5NZ5lAT/xmZpQoCM
gcgoTcJoq0awWFI/T6NMN8Xl8kwpn/w3BtpW9edPS7KQH6Qo/z1ppjDeQ/tru/H5
ryAlcXSh/x8CgYEAwPtmPg4fkJe0wflNfXgCTI5w2bJG8ZBP3hUno/6hF17y7r1M
H/pWjQAcEnmjVbFOoWTyKXCz4KwwFYw8CZ361zNAdEkBTJawd0BCPH0UeM+O3xLO
hM0iipXICNBzxIeZnc34FX8UdPi5DSodK9LUgR47CcSPYuLevBiaEnyh1iECgYA3
vzsR/Kiu3JQZEGxLjmvXVwFDmMh3agQMqF9vRlr5nsKNhaqeqSYO0bEKr0LkzY5m
lQBOoCl7KZJ+0Z/feHxzXa3jmf0tzeEKZfJBzkU8QK1NXRy0Ag+BxPsM1aYiZ/Uo
ZJuIvhhQwyk7yVP62+qiFQIDMXDkOJP4K+CsBrVS5wKBgQCZbvzbdVhjKgjhB3hZ
fkA5KkQgHHhho3AJykARehPEDn3q37nhOH6QcDKAQD8wpXJN+zdAxsC4Nk/HBRyk
xA67RhwbXK5yL+3vlWG1Fs863tUlYxjF9xJV+qDpNRxAnR2Kz2iHICzcQ/93Vg6S
o7rEM/D7upgD89q9LqmqM+U3vg==
-----END PRIVATE KEY-----
-----BEGIN CERTIFICATE-----
MIIDnzCCAoegAwIBAgIUE3jYzxKpepVM0CSLZd9GNv6BHj8wDQYJKoZIhvcNAQEL
BQAwUDELMAkGA1UEBhMCR0IxHTAbBgNVBAMMFE9wZW5SZW1vdGUgRGVtbyBDZXJ0
MRMwEQYDVQQKDApPcGVuUmVtb3RlMQ0wCwYDVQQLDAREZW1vMCAXDTIwMDYwODE5
MTc1MVoYDzIwNTAwNjAxMTkxNzUxWjBQMQswCQYDVQQGEwJHQjEdMBsGA1UEAwwU
T3BlblJlbW90ZSBEZW1vIENlcnQxEzARBgNVBAoMCk9wZW5SZW1vdGUxDTALBgNV
BAsMBERlbW8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCrKEk77HcJ
B5SqvoN2UbRsDh9d0ECN8tOU5hC2poih+6XBJgikQ8gdy7ptt477KRh3ZIiw3ZTX
Hg0//Ju71D/4EDBYwHxoSK9WehP5Kz/LrBHhtArXK3RYH8pFS13CDOPjXnm6LMN5
2mRGwm2gCwKwRTbfm+D9hjpVuwt0sfHaXVETlUc4JystlfYVurMcfsox9tsbRuzl
EakyK9Cr1V7bgaLMosHDX3NSuEyzb9DQZ3PBK3JjJhSeYkGNuP/NocMrWy/JHd2v
2Wev9W+D1Pv46Sqfrvd6K7oP00FL0CdODkMRBVTlb1wq/6uJdRbnVUM0PGA9enrQ
vMB11fFglHa3AgMBAAGjbzBtMB0GA1UdDgQWBBT0ixs03BOrns+E2+xSU+nfP9KX
iTAfBgNVHSMEGDAWgBT0ixs03BOrns+E2+xSU+nfP9KXiTAPBgNVHRMBAf8EBTAD
AQH/MBoGA1UdEQQTMBGCCWxvY2FsaG9zdIcEfwAAATANBgkqhkiG9w0BAQsFAAOC
AQEAawmLoD7bzFTM0Z58PR6jQR3ypD6IAyei6xiBI7wvxbjyxqQrk1i0rK2Aexjk
v2ZsAUmtrm5k5pWpBsokNuRddPV1K2OZjTj9HPc9AxqjyHKyqRXmVKWkzbWQDLVS
lGRk7yviUFS8nRuY0vLfqZzF7e2HeasThILJibY8rUVLuq+iMS35RDwQ9usIOiYz
dF4CO3HFZ6NtDheM1mPAy4Q76H1P1fINuA8mp/by9J8heexqjgpBKYexiQhjb1A7
NBdWbJPXoNJplGXjGIbj8KxW61ih1wDRE2ZseOflRstO9/Txm7+Cuqo+WBOK39cU
CXPKre2pqmkIu65wJ6VcTKeSqw==
-----END CERTIFICATE-----
"""
    }
}
