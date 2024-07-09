/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.mqtt;

import com.hivemq.client.util.KeyStoreUtil;
import org.apache.http.client.utils.URIBuilder;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import javax.net.ssl.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class MQTTProtocol extends AbstractMQTTClientProtocol<MQTTProtocol, MQTTAgent, String, MQTT_IOClient, MQTTAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MQTTProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "MQTT Client";
    protected final Map<AttributeRef, Consumer<MQTTMessage<String>>> protocolMessageConsumers = new HashMap<>();

    protected MQTTProtocol(MQTTAgent agent) {
        super(agent);
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, MQTTAgentLink agentLink) throws RuntimeException {
        agentLink.getSubscriptionTopic().ifPresent(topic -> {
            Consumer<MQTTMessage<String>> messageConsumer = msg -> updateLinkedAttribute(
                new AttributeRef(assetId, attribute.getName()), msg.payload
            );
            client.addMessageConsumer(topic, messageConsumer);
            protocolMessageConsumers.put(new AttributeRef(assetId, attribute.getName()), messageConsumer);
        });
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, MQTTAgentLink agentLink) {
        agentLink.getSubscriptionTopic().ifPresent(topic -> {
            AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
            Consumer<MQTTMessage<String>> messageConsumer = protocolMessageConsumers.remove(attributeRef);
            if (messageConsumer != null) {
                client.removeMessageConsumer(topic, messageConsumer);
            }
        });
    }

    @Override
    protected MQTT_IOClient createIoClient() throws Exception {
        MQTT_IOClient client = super.createIoClient();
        // Don't want the default message consumer, topic specific consumers will do the message routing for us
        client.removeAllMessageConsumers();
        return client;
    }

    @Override
    protected MQTT_IOClient doCreateIoClient() throws Exception {
        String host = agent.getHost().orElse(null);
        int port = agent.getPort().orElseGet(() -> {
            if (agent.isSecureMode().orElse(false)) {
                return agent.isWebsocketMode().orElse(false) ? 443 : 8883;
            } else {
                return agent.isWebsocketMode().orElse(false) ? 80 : 1883;
            }
        });

        URI websocketURI = null;

        if (agent.isWebsocketMode().orElse(false)) {
            URIBuilder builder = new URIBuilder()
                .setHost(host)
                .setPort(port);
            agent.getWebsocketPath().ifPresent(builder::setPath);
            agent.getWebsocketQuery().map(query -> query.startsWith("?") ? query.substring(1) : query).ifPresent(builder::setCustomQuery);
            websocketURI = builder.build();
        }

        MQTTLastWill lastWill = null;

        if (agent.getLastWillTopic().isPresent()) {
            String topic = agent.getLastWillTopic().get();
            String payload = agent.getLastWillPayload().orElse(null);
            boolean retain = agent.isLastWillRetain().orElse(false);
            lastWill = new MQTTLastWill(topic, payload, retain);
        }


	    File keyStoreFile = new File(this.getClass().getResource("keystore.jks").toURI());
	    File trustStoreFile = new File(this.getClass().getResource("truststore.jks").toURI());

//		KeyManagerFactory keyManagerFactory = KeyStoreUtil.keyManagerFromKeystore(keyStoreFile, "secret", "secret");
		TrustManagerFactory userTmFactory = KeyStoreUtil.trustManagerFromKeystore(trustStoreFile, "secret");


	    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	    try (InputStream keyStoreStream = this.getClass().getResourceAsStream("keystore.jks")) {
		    keyStore.load(keyStoreStream, "secret".toCharArray());
	    }
//
//	    // Load the truststore
//	    KeyStore trustStore = KeyStore.getInstance("JKS");
//	    try (InputStream trustStoreStream = this.getClass().getResourceAsStream("truststore.jks")) {
//		    trustStore.load(trustStoreStream, "secret".toCharArray());
//	    }
//
//	    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//	    keyManagerFactory.init(keyStore, "secret".toCharArray());
//		KeyManagerFactory fac = CustomKeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//	    // Initialize the TrustManagerFactory
//	    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//	    trustManagerFactory.init(trustStore);

	    TrustManagerFactory defaultTmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    defaultTmFactory.init((KeyStore) null);

	    System.setProperty("javax.net.debug", "ssl,handshake");
//	    KeyManagerFactory keyManagerFactory = CustomKeyManagerFactory.getInstance(agent.getCertificateAlias());
//	    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
//	    X509KeyManager customKeyManager = new CustomX509KeyManager((X509KeyManager) keyManagers[0], agent.getCertificateAlias());

	    // Initialize CustomKeyManagerFactory
	    KeyManagerFactory keyManagerFactory = new CustomKeyManagerFactory(agent.getCertificateAlias());

	    keyManagerFactory.init(keyStore, "secret".toCharArray());



	    // Create SSLContext and initialize it with key managers and trust managers
//	    SSLContext sslContext = SSLContext.getInstance("TLS");
//	    sslContext.init(new KeyManager[]{customKeyManager}, defaultTmFactory.getTrustManagers(), null);
//	    sslContext.init(keyManagerFactory.getKeyManagers(), userTmFactory.getTrustManagers(), null);

		// Create an SSLSocketFactory from the SSLContext
//	    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

	    // Connect to a server using the SSLSocketFactory
//	    try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("certauth.cryptomix.com", 443)) {

//		    SSLSession sslSession = sslSocket.getSession();
//		    X509Certificate[] serverCertChain = (X509Certificate[]) sslSession.getPeerCertificates();
//		    System.out.println("Server Certificate Chain:");
//		    for (X509Certificate cert : serverCertChain) {
//			    System.out.println(cert);
//		    }
//
//		    // Send a simple HTTP GET request
//		    OutputStream outputStream = sslSocket.getOutputStream();
//		    outputStream.write("GET /json/ HTTP/1.1\r\nHost: certauth.cryptomix.com\r\n\r\n".getBytes());
//		    outputStream.flush();
//
//		    // Read the response
//		    InputStream inputStream = sslSocket.getInputStream();
//		    int data;
//		    while ((data = inputStream.read()) != -1) {
//			    System.out.print(String.valueOf((char) data));
//		    }
//	    }

	    return new MQTT_IOClient(agent.getClientId().orElseGet(UniqueIdentifierGenerator::generateId), host, port, agent.isSecureMode().orElse(false), !agent.isResumeSession().orElse(false), agent.getUsernamePassword().orElse(null), websocketURI, lastWill, keyManagerFactory, userTmFactory);
    }

    @Override
    protected void onMessageReceived(MQTTMessage<String> message) {
        // This isn't used instead messages are targeted by topic
    }

    @Override
    protected MQTTMessage<String> createWriteMessage(MQTTAgentLink agentLink, AttributeEvent event, Object processedValue) {
        Optional<String> topic = agentLink.getPublishTopic();

        if (!topic.isPresent()) {
            LOG.fine(prefixLogMessage("Publish topic is not set in agent link so cannot publish message"));
            return null;
        }

        String valueStr = ValueUtil.convert(processedValue, String.class);
        return new MQTTMessage<>(topic.get(), valueStr);
    }

    @Override
    public String getProtocolName() {
        return "MQTT Client";
    }

	// Custom X509KeyManager to intercept and print certificate chain
//	static class CustomX509KeyManager implements X509KeyManager {
//		private final X509KeyManager originalKeyManager;
//		private final String userDefinedAlias;
//
//		CustomX509KeyManager(X509KeyManager originalKeyManager, String userDefinedAlias) {
//			this.originalKeyManager = originalKeyManager;
//			this.userDefinedAlias = userDefinedAlias;
//		}
//
//		@Override
//		public String[] getClientAliases(String keyType, Principal[] issuers) {
//			return originalKeyManager.getClientAliases(keyType, issuers);
//		}
//
//		@Override
//		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
//			// Check if the requested alias exists
//			for (String type: keyType){
//				String[] aliases = getClientAliases(type, issuers);
//				if (aliases == null) continue;
//
//				for (String alias : aliases) {
//					if (alias.equals(this.userDefinedAlias)) {
//						return alias;
//					}
//				}
//			}
//
//			// If the requested alias does not exist, return null or a default alias
//			return originalKeyManager.chooseClientAlias(keyType, issuers, socket);
//		}
//
//		@Override
//		public String[] getServerAliases(String keyType, Principal[] issuers) {
//			return originalKeyManager.getServerAliases(keyType, issuers);
//		}
//
//		@Override
//		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
//			return originalKeyManager.chooseServerAlias(keyType, issuers, socket);
//		}
//
//		@Override
//		public X509Certificate[] getCertificateChain(String alias) {
//			return originalKeyManager.getCertificateChain(alias);
//		}
//
//		@Override
//		public PrivateKey getPrivateKey(String alias) {
//			return originalKeyManager.getPrivateKey(alias);
//		}
//	}
}



