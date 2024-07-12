package org.openremote.manager.security;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.security.KeystoreService;
import org.openremote.model.security.Realm;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class KeystoreServiceImpl implements KeystoreService {

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
		List<String> storeTypes = Arrays.stream(KeyStoreType.values()).map(KeyStoreType::getFileName).toList();
		for (Realm realm : realmList) {
			for (String storeType : storeTypes){
				Path storePath = Paths.get(persistenceService.getStorageDir().toString(), "keystores", realm.getName() + "." + storeType);
				getLogger().warning("Accessing: " + storePath.toString());
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				File keyStoreFile = storePath.toAbsolutePath().toFile();

				// Ensure parent directories exist
				if (keyStoreFile.getParentFile() != null && !keyStoreFile.getParentFile().exists()) {
				    keyStoreFile.getParentFile().mkdirs();
				}

				// Initialize KeyStore
				try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
				    keyStore.load(fis, getKeystorePassword()); // Assuming keystorePassword is defined elsewhere
				} catch (Exception e) {
				    getLogger().severe("Couldn't find KeyStore file " + storePath + ", initializing new KeyStore");
				    keyStore.load(null, getKeystorePassword());
				    // Save the newly created KeyStore
				    try (OutputStream os = new FileOutputStream(keyStoreFile)) {
				        keyStore.store(os, getKeystorePassword());
				    } catch (Exception saveException) {
				        getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
				    }
				}
			}
		}
	}
	@Override
	public KeyStore getKeyStore(String realm, KeyStoreType type) {
		Path storePath = Paths.get(persistenceService.getStorageDir().toString(), "keystores", realm + "." + type.getFileName());
		try {
			return KeyStore.getInstance(storePath.toFile(), getKeystorePassword());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
		public void StoreKeystore(KeyStore keystore, String realm, KeyStoreType type) {
		Path storePath = Paths.get(persistenceService.getStorageDir().toString(), "keystores", realm + "." + type.getFileName());
		try {
			keystore.store(new FileOutputStream(storePath.toString()), getKeystorePassword());
		} catch (Exception saveException) {
			getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
		}
	}

	public char[] getKeystorePassword(){
		return this.keyStorePassword.toCharArray();
	}


	@Override
	public void stop(Container container) throws Exception {

	}


	@Override
	public Logger getLogger(){return LOG;}


}
