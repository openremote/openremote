package org.openremote.agent.protocol.mqtt;

import org.openremote.agent.protocol.mqtt.CustomKeyManagerFactorySpi;

import javax.net.ssl.KeyManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

public class CustomKeyManagerFactory extends KeyManagerFactory {

	public CustomKeyManagerFactory(String userRequestedAlias) {
		super(new CustomKeyManagerFactorySpi(userRequestedAlias), new Provider("CustomKeyManagerFactory", 1.0, "Custom Key Manager Factory") {}, KeyManagerFactory.getDefaultAlgorithm());
	}

//	public static final KeyManagerFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
//		CustomKeyManagerFactory factory = new CustomKeyManagerFactory(algorithm);
//		Security.addProvider(factory.getProvider());
//		return factory;
//	}
}
