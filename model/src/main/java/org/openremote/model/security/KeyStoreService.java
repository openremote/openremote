package org.openremote.model.security;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.logging.Logger;

public interface KeyStoreService extends ContainerService {
	String OR_KEYSTORE_PASSWORD = "OR_KEYSTORE_PASSWORD";
	Logger LOG = Logger.getLogger(KeyStoreService.class.getName());

	KeyStore getKeyStore(String realm, KeyStoreType type);
	void storeKeyStore(KeyStore keyStore, String realm, KeyStoreType type);
	char[] getKeyStorePassword();

	KeyManagerFactory getKeyManagerFactory(String realm, String preferredAlias) throws Exception;
	TrustManagerFactory getTrustManagerFactory(String realm) throws Exception;
	/**
	 * This method is used by all ManagerFactories, that returns True if the provided string s
	 * @param s The string to check
	 * @param realm The realm in which we need the alias to exist in
	 * @param requestedAlias the alias that the user specified in the client creation
	 * @return True, if all criteria are matched.
	 */
	static Boolean isRequestedAlias(String s, String realm, String requestedAlias){
		return s.equals(realm+"."+requestedAlias);
	}
//	Logger getLogger();

	/**
	 * Enum that contains the different types of KeyStores that are required to contain different types
	 * of certificates and keypairs
	 *
	 * TODO: Implement per-keystore env-var'd password
	 */
	enum KeyStoreType {
		CLIENT_KEYSTORE("client_keystore", "OR_SSL_CLIENT_KEYSTORE_FILE"),
		CLIENT_TRUSTSTORE("client_truststore", "OR_SSL_CLIENT_TRUSTSTORE_FILE"),
		SERVER_KEYSTORE("server_keystore","OR_SSL_SERVER_KEYSTORE_FILE");

		private final String fileName;
		private final String environmentVariableName;
		private Path filePath;
		KeyStoreType(String fileName, String envVarName) {
			this.fileName = fileName;
			this.environmentVariableName = envVarName;
			this.filePath = Paths.get("keystores").resolve(this.getFileName()+".p12");
		}

		public String getFileName() {
			return fileName;
		}

		public String getEnvironmentVariableName() {return environmentVariableName;}

		public void setPath(Path f){
			this.filePath = f;
		}

		public Path getPath(){
			return filePath;
		}

		@Override
		public String toString() {
			return String.format("%s (File Extension: %s, OpenRemote environment variable: %s)", this.name(), fileName, environmentVariableName);
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
