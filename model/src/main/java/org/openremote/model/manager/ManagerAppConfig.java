package org.openremote.model.manager;

import java.io.Serializable;
import java.util.Map;

public class ManagerAppConfig implements Serializable {
    protected boolean loadLocales;
    protected Map<String, String> languages;
    protected Map<String, ManagerAppRealmConfig> realms;
    protected Map<String, Object> pages;
    protected ManagerConfig manager;
}
