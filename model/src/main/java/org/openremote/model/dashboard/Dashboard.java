package org.openremote.model.dashboard;

import org.openremote.model.attribute.AttributeMap;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "DASHBOARD")
public abstract class Dashboard {

    // Completely unfinished and very basic
    // Not been busy with Models since we're still designing it.
    // For now, I ust copy-pasted stuff from the Asset model and refactored it. ;)

    @Id
    @Column(name = "ID", length = 22, columnDefinition = "char(22)")
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn;

    @NotBlank(message = "{Asset.realm.NotBlank}")
    @Size(min = 1, max = 255, message = "{Asset.realm.Size}")
    @Column(name = "REALM", nullable = false, updatable = false)
    protected String realm;

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Column(name = "OWNER_ID", nullable = false)
    protected String ownerId;

    @Column(name = "DISPLAY_NAME", nullable = false)
    protected String displayName;

    @Column(name = "TEMPLATE", columnDefinition = PERSISTENCE_JSON_VALUE_TYPE)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    @Valid
    protected DashboardTemplate template;
}
