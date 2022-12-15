package org.openremote.model.manager;

import java.sql.CallableStatement;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ManagerConfig {
    protected String managerUrl;
    protected String keycloakUrl;
    protected String appVersion;
    protected String realm;
    protected String clientId;
    protected boolean autoLogin;
    protected boolean consoleAutoEnable;
    protected boolean loadIcons;
    protected int pollingIntervalMillis;
    protected String[] loadTranslations;
    protected boolean loadDescriptors;
    protected String translationsLoadPath;
    protected boolean skipFallbackToBasicAuth;
    protected Auth auth;
    protected Credentials credentials;
    protected EventProviderType eventProviderType;
    protected MapType mapType;
    protected Object configureTranslationsOptions;
    protected Object basicLoginProvider;

}
