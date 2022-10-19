package org.openremote.model.dashboard;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "DASHBOARD")
public class Dashboard {

    @Id
    @Column(name = "ID", length = 22, columnDefinition = "char(22)")
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn;

    @NotBlank(message = "{Dashboard.realm.NotBlank}")
    @Size(min = 1, max = 255, message = "{Asset.realm.Size}")
    @Column(name = "REALM", nullable = false, updatable = false)
    protected String realm;

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Column(name = "OWNER_ID", nullable = false)
    protected String ownerId;

    @Column(name = "VIEW_ACCESS", nullable = false)
    protected DashboardAccess viewAccess;

    @Column(name = "EDIT_ACCESS", nullable = false)
    protected DashboardAccess editAccess;

    @NotBlank(message = "{Dashboard.displayName.NotBlank}")
    @Column(name = "DISPLAY_NAME", nullable = false)
    protected String displayName;

    @NotNull(message = "{Dashboard.template.NotNull}")
    @Column(name = "TEMPLATE", columnDefinition = PERSISTENCE_JSON_VALUE_TYPE, nullable = false)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    @Valid
    protected DashboardTemplate template;


    /* ----------------------------- */

    public void setId(String id) {
        this.id = id;
    }
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }
    public void setRealm(String realm) {
        this.realm = realm;
    }
    public void setVersion(long version) {
        this.version = version;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public void setViewAccess(DashboardAccess access) { this.viewAccess = access; }
    public void setEditAccess(DashboardAccess access) { this.editAccess = access; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setTemplate(@Valid DashboardTemplate template) {
        this.template = template;
    }


    public String getId() { return this.id; }
    public Date getCreatedOn() { return this.createdOn; }
    public String getRealm() { return this.realm; }
    public long getVersion() { return this.version; }
    public String getOwnerId() { return this.ownerId; }
    public DashboardAccess getViewAccess() { return this.viewAccess; }
    public DashboardAccess getEditAccess() { return this.editAccess; }
    public String getDisplayName() { return this.displayName; }
    public DashboardTemplate getTemplate() { return this.template; }
}
