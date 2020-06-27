package org.openremote.model.apps;

import org.openremote.model.value.ObjectValue;

import javax.persistence.*;

import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "CONSOLE_APP_CONFIG")
public class ConsoleAppConfig {

    public enum MenuPosition {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT
    }

    public ConsoleAppConfig() {
    }

    public ConsoleAppConfig(String realm, String url, Boolean menuEnabled, MenuPosition menuPosition, String menuImage, String primaryColor, String secondaryColor, ObjectValue links) {
        this.realm = realm;
        this.url = url;
        this.menuEnabled = menuEnabled;
        this.menuPosition = menuPosition;
        this.menuImage = menuImage;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.links = links;
    }

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Column(name = "REALM", nullable = false)
    protected String realm;

    @Column(name = "URL", nullable = false)
    protected String url;

    @Column(name = "MENU_ENABLED", nullable = false)
    protected Boolean menuEnabled;

    @Column(name = "MENU_POSITION", nullable = false)
    @Enumerated(EnumType.STRING)
    protected MenuPosition menuPosition;

    @Column(name = "MENU_IMAGE", nullable = false)
    protected String menuImage;

    @Column(name = "PRIMARY_COLOR", nullable = false)
    protected String primaryColor;

    @Column(name = "SECONDARY_COLOR", nullable = false)
    protected String secondaryColor;

    @Column(name = "LINKS", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    protected ObjectValue links;
}
