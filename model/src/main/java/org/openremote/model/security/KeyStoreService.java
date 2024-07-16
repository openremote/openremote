package org.openremote.model.security;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.logging.Logger;

public interface KeyStoreService extends ContainerService {
	String OR_KEYSTORE_PASSWORD = "OR_KEYSTORE_PASSWORD";
	Logger LOG = Logger.getLogger(KeyStoreService.class.getName());

	KeyStore getKeyStore(String realm, KeyStoreType type);
	void StoreKeyStore(KeyStore keyStore, String realm, KeyStoreType type);
	char[] getKeyStorePassword();

	KeyManagerFactory getKeyManagerFactory(String realm, String preferredAlias) throws Exception;
	TrustManagerFactory getTrustManagerFactory(String realm) throws Exception;

//	Logger getLogger();

	enum KeyStoreType {
		CLIENT_KEYSTORE("client_keystore"),
		CLIENT_TRUSTSTORE("client_truststore"),
		SERVER_KEYSTORE("server_keystore");

		private final String fileName;

		KeyStoreType(String fileName) {
			this.fileName = fileName;
		}

		public String getFileName() {
			return fileName;
		}

		@Override
		public String toString() {
			return String.format("%s (File Extension: %s)", this.name(), fileName);
		}
	}
	@Override
	int getPriority();

	@Override
	void init(Container container) throws Exception;

	@Override
	void start(Container container) throws Exception;

	@Override
	void stop(Container container) throws Exception;
}
