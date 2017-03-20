package org.openremote.manager.shared.notification;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;


@Entity
@Table(name = "ALERT_ACTION")
public class AlertAction {

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    Long id;

    @NotNull
    @Column(name = "TYPE", nullable = false)
    String type;

    @NotNull
    @Column(name = "NAME", nullable = false)
    String name;

    /*
    * action (button) name	◦	type & parameters
    * */
}
