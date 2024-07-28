package org.openremote.manager.security;

import org.openremote.agent.protocol.mqtt.CustomKeyManagerFactory;
import org.openremote.agent.protocol.mqtt.CustomX509TrustManagerFactory;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.rules.flow.Option;
import org.openremote.model.security.KeyStoreService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.util.logging.Logger;
import java.util.Optional;

import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.util.MapAccess.getValue;

/**
 * <p>
 * This service is used for retrieving, creating, or editing KeyStore (and TrustStore) files.
 * </p><p>
 * Currently the KeyStores are stored in the Storage directory of OpenRemote, which is usually volumed and persisted.
 *</p><p>
 * Each realm is allocated 2 KeyStores; A Client KeyStore, which is used for storing key-pairs that are used by
 * clients (be that as an Agent or as a plain client), and a TrustStore, which contains trusted certificates, usually SSL
 * certificates. To ensure both security and extensibility, both the default and predefined TrustStores are used to find
 * the correct certificates.
 * </p>
 */
public class KeyStoreServiceImpl implements KeyStoreService {

	protected PersistenceService persistenceService;
	protected ManagerIdentityService identityService;

	private KeyStore keyStore = null;
	private KeyStore trustStore = null;

	private final String OR_SSL_CLIENT_KEYSTORE_FILE = "OR_SSL_CLIENT_KEYSTORE_FILE";
	private final String OR_SSL_CLIENT_TRUSTSTORE_FILE = "OR_SSL_CLIENT_TRUSTSTORE_FILE";

	private final String OR_SSL_CLIENT_KEYSTORE_PASSWORD = "OR_SSL_CLIENT_KEYSTORE_PASSWORD";
	private final String OR_SSL_CLIENT_TRUSTSTORE_PASSWORD = "OR_SSL_CLIENT_TRUSTSTORE_PASSWORD";

	private Path keyStorePath;
	private Path trustStorePath;


	public String OR_KEYSTORE_PASSWORD_DEFAULT;
	public String keyStorePassword;

	@Override
	public int getPriority() {
		return ManagerIdentityService.PRIORITY + 10;
	}

	@Override
	public void init(Container container) throws Exception {
		persistenceService = container.getService(PersistenceService.class);
		identityService = container.getService(ManagerIdentityService.class);

		OR_KEYSTORE_PASSWORD_DEFAULT = getString(container.getConfig(), "OR_ADMIN_PASSWORD", "secret");

		keyStorePassword = getString(container.getConfig(), OR_KEYSTORE_PASSWORD, OR_KEYSTORE_PASSWORD_DEFAULT);
	}

	@Override
	public void start(Container container) throws Exception {

		Optional<Path> keyStorePath = getValue(container.getConfig(), OR_SSL_CLIENT_KEYSTORE_FILE, Path.class);
		Optional<Path> trustStorePath = getValue(container.getConfig(), OR_SSL_CLIENT_TRUSTSTORE_FILE, Path.class);

		String keyStorePassword = getString(container.getConfig(), OR_SSL_CLIENT_KEYSTORE_PASSWORD, String.valueOf(getKeyStorePassword()));
		String trustStorePassword = getString(container.getConfig(), OR_SSL_CLIENT_KEYSTORE_PASSWORD, String.valueOf(getKeyStorePassword()));


		if(keyStorePath.isPresent()){
			try{
				keyStore = KeyStore.getInstance(keyStorePath.get().toFile(), keyStorePassword.toCharArray());
			} catch (Exception e){
				throw e;
			}
		}else{
			Path defaultKeyStorePath = persistenceService.resolvePath(Paths.get("keystores").resolve("client_keystore.p12"));
			if(new File(defaultKeyStorePath.toUri()).exists()) {
				keyStorePath = Optional.of(defaultKeyStorePath);
				this.keyStore = KeyStore.getInstance(new File(defaultKeyStorePath.toUri()), getKeyStorePassword());
			}else{
				keyStore = createKeyStore(defaultKeyStorePath);
			}

			this.keyStorePath = defaultKeyStorePath;
		}

		if(trustStorePath.isPresent()){
			try{
				keyStore = KeyStore.getInstance(trustStorePath.get().toFile(), trustStorePassword.toCharArray());
			} catch (Exception e){
				throw e;
			}
		}else{
			Path defaultTrustStorePath = persistenceService.resolvePath(Paths.get("keystores").resolve("client_truststore.p12"));
			if(new File(defaultTrustStorePath.toUri()).exists()) {
				trustStorePath = Optional.of(defaultTrustStorePath);
				this.trustStore = KeyStore.getInstance(new File(defaultTrustStorePath.toUri()), getKeyStorePassword());
			}else{
				trustStore = createKeyStore(defaultTrustStorePath);
			}
			this.trustStorePath = defaultTrustStorePath;
		}
	}
	@Override
	public KeyStore getKeyStore() {
		try {
			return KeyStore.getInstance(this.keyStorePath.toAbsolutePath().toFile(), getKeyStorePassword());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public KeyStore getTrustStore() {
		try {
			return KeyStore.getInstance(this.trustStorePath.toAbsolutePath().toFile(), getKeyStorePassword());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
		public void storeKeyStore(KeyStore keystore) {
		try {
			keystore.store(new FileOutputStream(this.keyStorePath.toFile()), getKeyStorePassword());
		} catch (Exception saveException) {
			getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
		}
	}
	@Override
	public void storeTrustStore(KeyStore keystore) {
		try {
			keystore.store(new FileOutputStream(this.trustStorePath.toFile()), getKeyStorePassword());
		} catch (Exception saveException) {
			getLogger().severe("Couldn't store TrustStore to Storage! " + saveException.getMessage());
		}
	}

	public char[] getKeyStorePassword(){
		return this.keyStorePassword.toCharArray();
	}

	@Override
	public KeyManagerFactory getKeyManagerFactory(String preferredAlias) throws Exception {
		KeyManagerFactory keyManagerFactory = new CustomKeyManagerFactory(preferredAlias);
		try {
			keyManagerFactory.init(this.keyStore, getKeyStorePassword());
		} catch (Exception e) {
			throw new Exception("Could not retrieve KeyManagerFactory: "+e.getMessage());
		}

		return keyManagerFactory;
	}
	@Override
	public TrustManagerFactory getTrustManagerFactory() throws Exception {

		CustomX509TrustManagerFactory tmf = new CustomX509TrustManagerFactory(this.trustStore, (KeyStore) null);
		try {
			tmf.init((KeyStore) null);
		} catch (Exception e) {
			throw new Exception("Could not retrieve KeyManagerFactory: "+e.getMessage());
		}

		return tmf;
	}
	public KeyStore createKeyStore(Path path) throws Exception {
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(null, getKeyStorePassword());
		File keyStoreFile = path.toAbsolutePath().toFile();
		if (keyStoreFile.getParentFile() != null && !keyStoreFile.getParentFile().exists()) {
			keyStoreFile.getParentFile().mkdirs();
		}
		keyStoreFile.createNewFile();

		// Save the newly created KeyStore
		try (OutputStream os = new FileOutputStream(keyStoreFile)) {
			keystore.store(os, getKeyStorePassword());
		} catch (Exception saveException) {
			getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
			throw saveException;
		}
		return keystore;
	}

	@Override
	public void stop(Container container) throws Exception {

	}
	public Logger getLogger(){return LOG;}
}
