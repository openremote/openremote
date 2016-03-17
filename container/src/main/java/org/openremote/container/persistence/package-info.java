@org.hibernate.annotations.GenericGenerator(
    name = Constants.PERSISTENCE_ID_GENERATOR,
    strategy = "enhanced-sequence",
    parameters = {
        @org.hibernate.annotations.Parameter(
            name = "sequence_name",
            value = "OR_MANAGER_SEQUENCE"
        ),
        @org.hibernate.annotations.Parameter(
            name = "initial_value",
            value = "1000"
        )
    }
)
package org.openremote.container.persistence;

import org.openremote.container.Constants;