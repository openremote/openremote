package org.openremote.container.persistence;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.openremote.container.util.IdentifierUtil;

import java.io.Serializable;

public class UniqueIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        return IdentifierUtil.generateGlobalUniqueId();
    }
}
