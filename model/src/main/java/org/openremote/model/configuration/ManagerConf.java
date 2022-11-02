package org.openremote.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

public class ManagerConf implements Serializable {
    protected Map<String, ManagerConfRealm> realms;
}

class ManagerConfRealm {
    @JsonProperty
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
    map
}
