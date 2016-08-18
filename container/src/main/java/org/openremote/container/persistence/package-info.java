@org.hibernate.annotations.GenericGenerators({
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR,
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
        }),
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_UNIQUE_ID_GENERATOR,
        strategy = "org.openremote.container.persistence.UniqueIdentifierGenerator"
    )
})

@org.hibernate.annotations.TypeDefs({
    @org.hibernate.annotations.TypeDef(
        name = "json",
        typeClass = JsonStringType.class
    )
})

package org.openremote.container.persistence;

import org.openremote.container.Constants;
import org.openremote.container.json.JsonStringType;
