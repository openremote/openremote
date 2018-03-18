@org.hibernate.annotations.GenericGenerators({
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR,
        strategy = "enhanced-sequence",
        parameters = {
            @org.hibernate.annotations.Parameter(
                name = "sequence_name",
                value = "OPENREMOTE_SEQUENCE"
            ),
            @org.hibernate.annotations.Parameter(
                name = "initial_value",
                value = "1000"
            )
        }),
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_UNIQUE_ID_GENERATOR,
        strategy = "org.openremote.container.util.UniqueIdentifierGenerator"
    )
})

package org.openremote.container.util;

import org.openremote.model.Constants;