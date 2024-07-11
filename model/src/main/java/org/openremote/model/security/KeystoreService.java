package org.openremote.model.security;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.logging.Logger;

public interface KeystoreService extends ContainerService {
	String OR_KEYSTORE_PASSWORD = "OR_KEYSTORE_PASSWORD";
	Logger LOG = Logger.getLogger(KeystoreService.class.getName());

	public KeyStore getClientKeyStore(String realm);

	public KeyStore getClientTrustStore(String realm);
	public char[] getKeystorePassword();

	@Override
	int getPriority();

	@Override
	void init(Container container) throws Exception;

	@Override
	void start(Container container) throws Exception;

	@Override
	void stop(Container container) throws Exception;

	Logger getLogger();
}
