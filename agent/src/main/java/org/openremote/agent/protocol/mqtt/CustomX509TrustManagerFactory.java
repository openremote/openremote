package org.openremote.agent.protocol.mqtt;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;

public class CustomX509TrustManagerFactory extends TrustManagerFactory {
	public CustomX509TrustManagerFactory(KeyStore... keystores) throws KeyStoreException {
		super(new CustomX509TrustManagerFactorySpi(keystores), new Provider("CustomX509TrustManagerFactory", "1.0", "Custom X509 Trust Manager Factory") {}, TrustManagerFactory.getDefaultAlgorithm());
	}
}
