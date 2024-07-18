package org.openremote.manager.security;

import org.openremote.agent.protocol.mqtt.CustomKeyManagerFactory;
import org.openremote.agent.protocol.mqtt.CustomX509TrustManagerFactory;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.security.KeyStoreService;
import org.openremote.model.security.Realm;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getString;
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
		List<Realm> realmList = Arrays.stream(identityService.getIdentityProvider().getRealms()).toList();
		List<KeyStoreType> storeTypes = Arrays.stream(KeyStoreType.values()).toList();
		for (KeyStoreType storeType : storeTypes){
			String storeFilename = storeType.getFileName();

			// If the store does not exist, check if there is an environment variable. If it's there, check if it does
			// exist, if it's not, create your own.
			if(!storeExists(storeType)){
				String OR_STORE_FILE = getString(container.getConfig(), storeType.getEnvironmentVariableName(), null);
				if (OR_STORE_FILE != null){
					if (new File(OR_STORE_FILE).exists()){
						continue;
					}
				}
			}

			Path storePath = getStorePath(storeType);
//			Path storePath = Paths.get(persistenceService.getStorageDir().toString(), "keystores", storeType + "."+"p12");
			getLogger().warning("Accessing: " + storePath);
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			File keyStoreFile = storePath.toAbsolutePath().toFile();

			// Ensure parent directories exist
			if (keyStoreFile.getParentFile() != null && !keyStoreFile.getParentFile().exists()) {
				keyStoreFile.getParentFile().mkdirs();
			}

			// Initialize KeyStore
			try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
				keyStore.load(fis, getKeyStorePassword()); // Assuming keystorePassword is defined elsewhere
			} catch (Exception e) {
				getLogger().severe("Couldn't find KeyStore file " + storePath + ", initializing new KeyStore");
				keyStore.load(null, getKeyStorePassword());
				// Save the newly created KeyStore
				try (OutputStream os = new FileOutputStream(keyStoreFile)) {
					keyStore.store(os, getKeyStorePassword());
				} catch (Exception saveException) {
					getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
				}
			}
		}
	}
	@Override
	public KeyStore getKeyStore(String realm, KeyStoreType type) {
		Path storePath = persistenceService.resolvePath("keystores").resolve(type.getFileName() + "."+"p12");
		try {
			return KeyStore.getInstance(storePath.toFile(), getKeyStorePassword());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
		public void StoreKeyStore(KeyStore keystore, String realm, KeyStoreType type) {
		Path storePath = persistenceService.resolvePath("keystores").resolve(type.getFileName() + "."+"p12");
		try {
			keystore.store(new FileOutputStream(storePath.toString()), getKeyStorePassword());
		} catch (Exception saveException) {
			getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
		}
	}

	public char[] getKeyStorePassword(){
		return this.keyStorePassword.toCharArray();
	}

	@Override
	public KeyManagerFactory getKeyManagerFactory(String realm, String preferredAlias) throws Exception {
		KeyManagerFactory keyManagerFactory = new CustomKeyManagerFactory(preferredAlias, realm);
		try {
			keyManagerFactory.init(getKeyStore(realm, KeyStoreType.CLIENT_KEYSTORE), getKeyStorePassword());
		} catch (Exception e) {
			throw new Exception("Could not retrieve KeyManagerFactory: "+e.getMessage());
		}

		return keyManagerFactory;
	}

	private Boolean storeExists(KeyStoreType type){
		return new File(getStorePath(type).toAbsolutePath().toUri()).exists();
	}

	private Path getStorePath(KeyStoreType type){
		return persistenceService.resolvePath("keystores").resolve(type.getFileName() + "."+"p12");
	}

	@Override
	public TrustManagerFactory getTrustManagerFactory(String realm) throws Exception {

		CustomX509TrustManagerFactory tmf = new CustomX509TrustManagerFactory(getKeyStore(realm, KeyStoreType.CLIENT_TRUSTSTORE), (KeyStore) null);
		try {
			tmf.init((KeyStore) null);
		} catch (Exception e) {
			throw new Exception("Could not retrieve KeyManagerFactory: "+e.getMessage());
		}

		return tmf;
	}


	@Override
	public void stop(Container container) throws Exception {

	}


	public Logger getLogger(){return LOG;}


}
