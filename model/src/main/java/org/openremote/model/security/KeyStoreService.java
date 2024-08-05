package org.openremote.model.security;

import org.openremote.model.ContainerService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.util.logging.Logger;

public interface KeyStoreService extends ContainerService {
	Logger LOG = Logger.getLogger(KeyStoreService.class.getName());

	KeyManagerFactory getKeyManagerFactory(String alias) throws Exception;
	TrustManagerFactory getTrustManagerFactory() throws Exception;
}
