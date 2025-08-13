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

package org.openremote.agent.protocol.mqtt;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

public class CustomKeyManagerFactorySpi extends KeyManagerFactorySpi {

	private X509ExtendedKeyManager keyManager;
	private final String realmPrefixedAlias;

	public CustomKeyManagerFactorySpi(String realmPrefixedAlias) {
		this.realmPrefixedAlias = realmPrefixedAlias;
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
				this.keyManager = new CustomX509KeyManager((X509ExtendedKeyManager) keyManager, realmPrefixedAlias);
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
		private final String realmPrefixedAlias;
		private static final Logger LOG = Logger.getLogger(CustomX509KeyManager.class.getName());

		public CustomX509KeyManager(X509ExtendedKeyManager keyManager, String realmPrefixedAlias) {
			this.keyManager = keyManager;
			this.realmPrefixedAlias = realmPrefixedAlias;
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return keyManager.getClientAliases(keyType, issuers);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return keyManager.chooseClientAlias(keyType,issuers,socket);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return keyManager.getServerAliases(keyType, issuers);
		}

		@Override
		public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
			for (String key : keyType) {
				String[] aliasArray = keyManager.getClientAliases(key, issuers);
				if (aliasArray == null) continue;
				Optional<String> found = Arrays.stream(aliasArray).filter(this::isCorrectAlias).findFirst();
				if(found.isPresent()){
					return found.get();
				}
			}
			//TODO: Not sure how this should be handled. This would mean that the keypair with realmPrefixedAlias
			//      either wasn't found, or didn't match up with the KeyType, or does not have the correct issuer.
			//      for now, log the issue that the certificate wasn't found, and then return null.

			LOG.severe("Could not find a certificate with Alias "+ this.realmPrefixedAlias);
			return null;
		}

		private boolean isCorrectAlias(String s) {
			return s.equals(this.realmPrefixedAlias);
 		}

		@Override
		public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
			return keyManager.chooseEngineServerAlias(keyType, issuers, engine);
		}

		public String chooseServerAlias(String keyType, Principal[] issuers, java.net.Socket socket) {
			return keyManager.chooseServerAlias(keyType,issuers,socket);
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
