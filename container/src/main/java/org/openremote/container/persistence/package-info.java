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
    ),
    @org.hibernate.annotations.TypeDef(
        name = Constants.PERSISTENCE_STRING_ARRAY_TYPE,
        typeClass = ArrayUserType.class
    )
})

package org.openremote.container.persistence;

import org.openremote.container.json.ModelArrayValuePersistentType;
import org.openremote.container.json.ModelObjectValuePersistentType;
import org.openremote.container.json.ModelValuePersistentType;
import org.openremote.model.Constants;