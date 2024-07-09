package org.openremote.agent.protocol.mqtt;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;

public class CustomKeyManagerFactorySpi extends KeyManagerFactorySpi {

	private X509ExtendedKeyManager keyManager;
	private final String userRequestedAlias;

	public CustomKeyManagerFactorySpi(String userRequestedAlias) {
		this.userRequestedAlias = userRequestedAlias;
	}

	@Override
	protected void engineInit(KeyStore keyStore, char[] password) {
		KeyManagerFactory factory = null;
		try {
			factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init(keyStore, password);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		for (KeyManager keyManager : factory.getKeyManagers()) {
			if (keyManager instanceof X509ExtendedKeyManager) {
				this.keyManager = new CustomX509KeyManager((X509ExtendedKeyManager) keyManager, userRequestedAlias);
				return;
			}
		}
		throw new IllegalStateException("No X509KeyManager found");
	}


	@Override
	protected void engineInit(ManagerFactoryParameters parameters) {
		throw new UnsupportedOperationException("engineInit(KeyManagerFactorySpi.Parameters) not supported");
	}

	@Override
	protected KeyManager[] engineGetKeyManagers() {
		return new KeyManager[]{keyManager};
	}

	private static class CustomX509KeyManager extends X509ExtendedKeyManager {

		private final X509ExtendedKeyManager keyManager;
		private final String userRequestedAlias;

		public CustomX509KeyManager(X509ExtendedKeyManager keyManager, String userRequestedAlias) {
			this.keyManager = keyManager;
			this.userRequestedAlias = userRequestedAlias;
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return keyManager.getClientAliases(keyType, issuers);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			// Check if the requested alias exists
			for (String type: keyType){
				String[] aliases = getClientAliases(type, issuers);
				if (aliases == null) continue;

				for (String alias : aliases) {
					if (alias.equals(this.userRequestedAlias)) {
						return alias;
					}
				}
			}

			// If the requested alias does not exist, return null or a default alias
			return keyManager.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return keyManager.getServerAliases(keyType, issuers);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return keyManager.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return keyManager.getCertificateChain(alias);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return keyManager.getPrivateKey(alias);
		}
	}
}
