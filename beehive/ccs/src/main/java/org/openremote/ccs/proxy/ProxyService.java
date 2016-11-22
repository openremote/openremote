package org.openremote.ccs.proxy;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;

public class ProxyService implements ContainerService {

    @Override
    public void init(Container container) throws Exception {

    }

    @Override
    public void configure(Container container) throws Exception {
/*

        String proxyHostname = config.getProperty("proxy.hostname", "localhost");
        Integer proxyTimeout = getIntegerConfiguration(config, "proxy.timeout", 10000);
        Integer proxyPort = getIntegerConfiguration(config, "proxy.port", 10000);
        String proxyClientPortRange = config.getProperty("proxy.clientPortRange", "30000-30010");
        Boolean useSSL = getBooleanConfiguration(config, "proxy.useSSL", true);
        String keystore = config.getProperty("proxy.keystore", "keystore.jks");
        String keystorePassword = config.getProperty("proxy.keystorePassword", "storepass");
*/
    }

    @Override
    public void start(Container container) throws Exception {
/*
        ProxyServer ps = new ProxyServer(proxyHostname, proxyTimeout, proxyPort, proxyClientPortRange, useSSL, keystore, keystorePassword, controllerCommandService, accountService, this);
        ps.start();
*/

    }

    @Override
    public void stop(Container container) throws Exception {

    }
}
