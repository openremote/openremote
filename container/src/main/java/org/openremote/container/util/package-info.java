@org.hibernate.annotations.GenericGenerators({
    @org.hibernate.annotations.GenericGenerator(
        name = Constants.PERSISTENCE_UNIQUE_ID_GENERATOR,
        strategy = "org.openremote.container.util.UniqueIdentifierGenerator"
    )
})

package org.openremote.container.util;

import org.openremote.model.Constants;
