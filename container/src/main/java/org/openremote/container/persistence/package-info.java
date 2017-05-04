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
        strategy = "org.openremote.container.persistence.UniqueIdentifierGenerator"
    )
})

@org.hibernate.annotations.TypeDefs({
    @org.hibernate.annotations.TypeDef(
        name = Constants.PERSISTENCE_JSON_VALUE_TYPE,
        typeClass = ModelValuePersistentType.class
    ),
    @org.hibernate.annotations.TypeDef(
        name = Constants.PERSISTENCE_JSON_OBJECT_TYPE,
        typeClass = ModelObjectValuePersistentType.class
    ),
    @org.hibernate.annotations.TypeDef(
        name = Constants.PERSISTENCE_JSON_ARRAY_TYPE,
        typeClass = ModelArrayValuePersistentType.class
    )
})

package org.openremote.container.persistence;

import org.openremote.container.json.ModelArrayValuePersistentType;
import org.openremote.container.json.ModelObjectValuePersistentType;
import org.openremote.container.json.ModelValuePersistentType;
import org.openremote.model.Constants;