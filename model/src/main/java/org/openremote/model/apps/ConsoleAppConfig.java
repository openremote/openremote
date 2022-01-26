package org.openremote.model.apps;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ConsoleAppConfig {

    public static class AppLink implements Serializable {
        protected String displayText;
        protected String pageLink;

        protected AppLink() {}

        public AppLink(String displayText, String pageLink) {
            this.displayText = displayText;
            this.pageLink = pageLink;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getPageLink() {
            return pageLink;
        }
    }

    public enum MenuPosition {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT
    }

    public ConsoleAppConfig() {
    }

    public ConsoleAppConfig(String realm, String initialUrl, String url, Boolean menuEnabled, MenuPosition menuPosition, String primaryColor, String secondaryColor, AppLink[] links) {
        this.realm = realm;
        this.initialUrl = initialUrl;
        this.url = url;
        this.menuEnabled = menuEnabled;
        this.menuPosition = menuPosition;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.links = links;
    }

    @JsonProperty
    protected Long id;

    @JsonProperty
    protected String realm;

    @JsonProperty
    protected String initialUrl;

    @JsonProperty
    protected String url;

    @JsonProperty
    protected Boolean menuEnabled;

    @JsonProperty
    protected MenuPosition menuPosition;

    @JsonProperty
    protected String primaryColor;

    @JsonProperty
    protected String secondaryColor;

    @JsonProperty
    protected AppLink[] links;

    public String getRealm() {
        return realm;
    }
}
