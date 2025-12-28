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
import org.openremote.manager.mqtt.OpenRemoteSSLContextFactory
import spock.lang.Specification
import spock.lang.TempDir

import javax.net.ssl.SSLContext
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * Test for OpenRemoteSSLContextFactory which handles dynamic certificate loading
 * for MQTT mTLS support.
 */
class OpenRemoteSSLContextFactoryTest extends Specification {

    @TempDir
    Path tempDir

    // Sample RSA private key (test key - not for production)
    static final String TEST_PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7VJTUt9Us8cKj
MzEfYyjiWA4R4/M2bS1+fWIcPm15A0ImLz7AvuPhzJZxUH5JDp9e3DdJ19I1T0pO
FJ4ZF/AvjW9Q0Sw1HxCgOVpXGDOFwNlLUy0xV5xW8xKWnAQMBPvBmRsKqaEqMOUB
E2cKuPdZGLqQpN9M2sUTXCxWvJ5+jmJ7VXqfTZGx+WYrCBvOSlU8oBvWO2PBYK6h
VwKBgQCFGLQ8I+Dz8HnmQcZCt7X6DKD6TmCqPBzMuHJsIv3TYRx6FfrZ9pFtBEwk
kX5kL3K+S7w9pBDWOmRSI2FLCKPbK0LQZN2YECJ4Y0fUyPyf8EoqWgGxCqNpxQWO
pNWmF6z0JhWBE2KnKDa7QQIDAQABAoIBABcON3RLpGiKwF4ZVYXEjmQnC6vJOKkd
dDnPQQdwCfELN8kWDNNEMJYwWsKWTD+o0Fbb9VHqaYbI0MZPHWvTcKnQPqBw4Dlk
KQBxLZVYGq4JLDo3F5YqgYJt8lQQmMECgYEA3K0CnLLPQ/5QJ4z6Lk+VqGR0xjjD
Pjdnbb1QpNMNPXUXOBdNr8fEPXU5PBDU3lYh6Dw8HCPqEPqBqYvWQ8HJqYpPLj8d
F7chYbpCKQb5vMZBwOqODSCf5T/p6Fvw7QQJQ2FJDEXAMPLE==
-----END PRIVATE KEY-----"""

    // Sample X.509 certificate (self-signed test cert)
    static final String TEST_CERTIFICATE = """-----BEGIN CERTIFICATE-----
MIIC5TCCAc2gAwIBAgIJAKZGPPVoMJhfMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNV
BAMMCWxvY2FsaG9zdDAeFw0yNDAxMDEwMDAwMDBaFw0yNTAxMDEwMDAwMDBaMBQx
EjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
ggEBALtUlNS31SzxwqMzMR9jKOJYDhHj8zZtLX59Yhw+bXkDQiYvPsC+4+HMlnFQ
fkkOn17cN0nX0jVPSk4Unhkf8C+Nb1DRLDUfEKA5WlcYM4XA2UtTLTFXnFbzEpac
BAwE+8GZGwqpoSow5QETZwq491kYupCk30zaxRNcLFa8nn6OYntVep9NkbH5ZisI
G85KVTygG9Y7Y8FgrqFXAoGBAIUYtDwj4PPweeZBxkK3tfoMoPpOYKo8HMy4cmwi
/dNhHHoV+tn2kW0ETCSRGENERATEDCERTFOREXAMPLETEST==
-----END CERTIFICATE-----"""

    // Sample combined PEM (key + cert) as used by HAProxy
    static final String COMBINED_PEM = TEST_PRIVATE_KEY + "\n" + TEST_CERTIFICATE

    def "test extractPemSections correctly parses private key"() {
        given: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "extracting private key sections from PEM content"
        def sections = factory.extractPemSections(TEST_PRIVATE_KEY, "PRIVATE KEY")

        then: "one private key section should be found"
        sections.size() == 1
        sections[0].contains("BEGIN PRIVATE KEY")
        sections[0].contains("END PRIVATE KEY")
    }

    def "test extractPemSections correctly parses certificate"() {
        given: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "extracting certificate sections from PEM content"
        def sections = factory.extractPemSections(TEST_CERTIFICATE, "CERTIFICATE")

        then: "one certificate section should be found"
        sections.size() == 1
        sections[0].contains("BEGIN CERTIFICATE")
        sections[0].contains("END CERTIFICATE")
    }

    def "test extractPemSections handles combined PEM"() {
        given: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "extracting sections from combined PEM"
        def keySection = factory.extractPemSections(COMBINED_PEM, "PRIVATE KEY")
        def certSection = factory.extractPemSections(COMBINED_PEM, "CERTIFICATE")

        then: "both sections should be found"
        keySection.size() == 1
        certSection.size() == 1
    }

    def "test findFirstNumberedCertFile sorts files correctly"() {
        given: "a directory with numbered cert files"
        def certsDir = tempDir.resolve("certs")
        Files.createDirectories(certsDir)
        Files.write(certsDir.resolve("02-example.com"), COMBINED_PEM.bytes)
        Files.write(certsDir.resolve("00-cert"), COMBINED_PEM.bytes)
        Files.write(certsDir.resolve("01-selfsigned"), COMBINED_PEM.bytes)
        Files.write(certsDir.resolve("other.txt"), "not a cert".bytes)

        and: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "finding the first numbered cert file"
        def result = factory.findFirstNumberedCertFile(certsDir)

        then: "the file with the lowest number (00-cert) should be returned"
        result != null
        result.fileName.toString() == "00-cert"
    }

    def "test findFirstNumberedCertFile returns null for empty directory"() {
        given: "an empty directory"
        def certsDir = tempDir.resolve("empty")
        Files.createDirectories(certsDir)

        and: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "finding the first numbered cert file"
        def result = factory.findFirstNumberedCertFile(certsDir)

        then: "null should be returned"
        result == null
    }

    def "test findFile locates correct file from alternatives"() {
        given: "a directory with various cert files"
        def certsDir = tempDir.resolve("certs")
        Files.createDirectories(certsDir)
        Files.write(certsDir.resolve("fullchain.pem"), TEST_CERTIFICATE.bytes)
        Files.write(certsDir.resolve("privkey.pem"), TEST_PRIVATE_KEY.bytes)

        and: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "finding cert file with multiple possible names"
        def certFile = factory.findFile(certsDir, "cert.pem", "certificate.pem", "server.crt", "fullchain.pem")

        and: "finding key file with multiple possible names"
        def keyFile = factory.findFile(certsDir, "key.pem", "privkey.pem", "server.key")

        then: "the first matching file should be found for each"
        certFile != null
        certFile.fileName.toString() == "fullchain.pem"
        keyFile != null
        keyFile.fileName.toString() == "privkey.pem"
    }

    def "test findFile returns null when no files match"() {
        given: "an empty directory"
        def certsDir = tempDir.resolve("certs")
        Files.createDirectories(certsDir)

        and: "an OpenRemoteSSLContextFactory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "finding a file that doesn't exist"
        def result = factory.findFile(certsDir, "nonexistent.pem", "missing.pem")

        then: "null should be returned"
        result == null
    }

    def "test certificatesHaveChanged returns false for non-existent directory"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "checking if certificates have changed when directory doesn't exist"
        def changed = factory.certificatesHaveChanged()

        then: "should return false (no certs directory)"
        changed == false
    }

    def "test hasFileChanged detects file modification"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        and: "a test file"
        def testFile = tempDir.resolve("test.pem")
        Files.write(testFile, "original".bytes)

        when: "checking if file has changed (first time)"
        def firstCheck = factory.hasFileChanged(testFile)

        then: "should return true (file is new)"
        firstCheck == true

        when: "updating file modification times and checking again"
        factory.updateFileModificationTimes(testFile)
        def secondCheck = factory.hasFileChanged(testFile)

        then: "should return false (no change)"
        secondCheck == false

        when: "modifying the file"
        Thread.sleep(100) // Ensure modification time changes
        Files.write(testFile, "modified".bytes)
        def thirdCheck = factory.hasFileChanged(testFile)

        then: "should return true (file was modified)"
        thirdCheck == true
    }

    def "test hasFileChanged returns false for null file"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "checking if null file has changed"
        def result = factory.hasFileChanged(null)

        then: "should return false"
        result == false
    }

    def "test clearSSLContexts clears cached context"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "clearing SSL contexts"
        factory.clearSSLContexts()

        then: "no exception should be thrown"
        notThrown(Exception)
    }

    def "test getPriority returns expected value"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        when: "getting priority"
        def priority = factory.getPriority()

        then: "should return high priority (100)"
        priority == 100
    }

    def "test parsePrivateKey handles malformed key"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        and: "a malformed private key"
        def malformedKey = "-----BEGIN PRIVATE KEY-----\nNOT_VALID_BASE64\n-----END PRIVATE KEY-----"

        when: "parsing the private key"
        factory.parsePrivateKey(malformedKey)

        then: "an exception should be thrown"
        thrown(Exception)
    }

    def "test extractPemSections with empty content"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        and: "empty content"
        def emptyContent = ""

        when: "extracting sections"
        def sections = factory.extractPemSections(emptyContent, "CERTIFICATE")

        then: "should return empty list"
        sections.isEmpty()
    }

    def "test extractPemSections with no matching sections"() {
        given: "a factory instance"
        def factory = new OpenRemoteSSLContextFactory()

        and: "content without matching sections"
        def content = "some random text without PEM sections"

        when: "extracting sections"
        def sections = factory.extractPemSections(content, "CERTIFICATE")

        then: "should return empty list"
        sections.isEmpty()
    }

    // Helper method to create a test keystore
    private void createTestKeystore(Path keystorePath, String password) {
        // Create a minimal PKCS12 keystore for testing
        KeyStore keystore = KeyStore.getInstance("PKCS12")
        keystore.load(null, password.toCharArray())

        try (def fos = Files.newOutputStream(keystorePath)) {
            keystore.store(fos, password.toCharArray())
        }
    }
}
