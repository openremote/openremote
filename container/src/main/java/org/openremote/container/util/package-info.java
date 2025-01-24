@org.hibernate.annotations.GenericGenerators({
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_UNIQUE_ID_GENERATOR,
        type = org.openremote.container.util.HibernateUniqueIdentifierGenerator.class
    )
})

package org.openremote.container.util;

import org.openremote.model.Constants;
