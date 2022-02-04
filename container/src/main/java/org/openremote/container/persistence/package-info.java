@TypeDefs({
    @TypeDef(
        name = Constants.PERSISTENCE_JSON_VALUE_TYPE,
        typeClass = JsonBinaryType.class
    ),
    @TypeDef(
        name = Constants.PERSISTENCE_STRING_ARRAY_TYPE,
        typeClass = StringArrayType.class
    ),
    @TypeDef(
        name = EpochMillisInstantType.TYPE_NAME,
        typeClass = EpochMillisInstantType.class
    ),
    @TypeDef(
        name = LTreeType.TYPE,
        typeClass = LTreeType.class
    )
})

package org.openremote.container.persistence;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.openremote.model.Constants;
import org.openremote.model.persistence.EpochMillisInstantType;
