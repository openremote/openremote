package org.openremote.model.dashboard;

import org.openremote.model.IdentifiableEntity;

import javax.persistence.*;

import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

@Entity
@Table(name = "DASHBOARD")
public abstract class Dashboard<T extends Dashboard<?>> implements IdentifiableEntity<T> {

    // Completely unfinished and very basic
    // Not been busy with Models since we're still designing it.
    // For now, I ust copy-pasted stuff from the Asset model and refactored it. ;)

    @Id
    @Column(name = "ID", length = 22, columnDefinition = "char(22)")
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Column(name = "NAME", nullable = false, length = 1023)
    protected String name;
}
