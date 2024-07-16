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
	void StoreKeystore(KeyStore keyStore, String realm, KeyStoreType type);
	char[] getKeystorePassword();

	public KeyManagerFactory getKeyManagerFactory(String realm, String preferredAlias) throws Exception;
	public TrustManagerFactory getTrustManagerFactory(String realm) throws Exception;

	@Override
	int getPriority();

	@Override
	void init(Container container) throws Exception;

	@Override
	void start(Container container) throws Exception;

	@Override
	void stop(Container container) throws Exception;

	Logger getLogger();

	public enum KeyStoreType {
		CLIENT_KEYSTORE("client_keystore"),
		CLIENT_TRUSTSTORE("client_truststore"),
		SERVER_KEYSTORE("server_keystore");

		private final String fileName;

		// Constructor to initialize the fileName field
		KeyStoreType(String fileName) {
			this.fileName = fileName;
		}

		// Getter method to access the fileName field
		public String getFileName() {
			return fileName;
		}

		// Overriding toString() for better representation
		@Override
		public String toString() {
			return String.format("%s (File Extension: %s)", this.name(), fileName);
		}
	}
}
