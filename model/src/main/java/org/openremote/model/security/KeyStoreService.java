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

	KeyStore getKeyStore();
	void storeKeyStore(KeyStore keyStore);
	KeyStore getTrustStore();
	void storeTrustStore(KeyStore keyStore);
	char[] getKeyStorePassword();
	KeyManagerFactory getKeyManagerFactory(String preferredAlias) throws Exception;
	TrustManagerFactory getTrustManagerFactory() throws Exception;
	static Boolean isRequestedAlias(String s, String requestedAlias){
		return s.equals(requestedAlias);
	}
}
