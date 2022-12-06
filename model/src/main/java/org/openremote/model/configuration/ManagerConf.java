package org.openremote.model.configuration;

import java.io.Serializable;
import java.util.Map;

public class ManagerConf implements Serializable {
    protected boolean loadLocales;
    protected Map<String, String> languages;
    protected Map<String, ManagerConfRealm> realms;
    protected Map<String, Object> pages;
}

class ManagerConfRealm {
    protected String appTitle = null;
    protected String styles = null;
    protected String logo = null;
    protected String logoMobile = null;
    protected String favicon = null;
    protected String language = null;
    protected ManagerHeaders[] headers = null;
}

enum ManagerHeaders {
    rules,
    insights,
    gateway,
    logs,
    account,
    users,
    assets,
    roles,
    realms,
    logout,
    language,
    export,
    map,
    appearance,
    provisioning
}
