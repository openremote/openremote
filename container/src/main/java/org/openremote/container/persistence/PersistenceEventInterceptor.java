/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.persistence;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.openremote.container.message.MessageBrokerService;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Intercept Hibernate lifecycle events and publish a message.
 */
public class PersistenceEventInterceptor extends EmptyInterceptor {

    protected MessageBrokerService messageBrokerService;
    protected Set<Object> inserts = new HashSet<>();
    protected Set<Object> updates = new HashSet<>();
    protected Set<Object> deletes = new HashSet<>();

    public MessageBrokerService getMessageBrokerService() {
        if (messageBrokerService == null)
            throw new IllegalStateException("MessageBrokerService must be set on interceptor before use");
        return messageBrokerService;
    }

    public void setMessageBrokerService(MessageBrokerService messageBrokerService) {
        this.messageBrokerService = messageBrokerService;
    }

    @Override
    public boolean onSave(Object entity, Serializable id,
                          Object[] state, String[] propertyNames, Type[] types)
        throws CallbackException {
        inserts.add(entity);
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id,
                                Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types)
        throws CallbackException {
        updates.add(entity);
        return false;
    }

    @Override
    public void onDelete(Object entity, Serializable id,
                         Object[] state,
                         String[] propertyNames,
                         Type[] types) {
        deletes.add(entity);
    }

    public void postFlush(Iterator iterator) throws CallbackException {
        try {
            ProducerTemplate producerTemplate =
                messageBrokerService.getContext().createProducerTemplate();

            for (Object insert : inserts) {
                publishEvent(producerTemplate, insert, PersistenceEvent.INSERT);
            }
            for (Object update : updates) {
                publishEvent(producerTemplate, update, PersistenceEvent.UPDATE);
            }
            for (Object delete : deletes) {
                publishEvent(producerTemplate, delete, PersistenceEvent.DELETE);

            }
        } finally {
            inserts.clear();
            updates.clear();
            deletes.clear();
        }
    }

    protected void publishEvent(ProducerTemplate producerTemplate, Object object, PersistenceEvent eventAction) {
        producerTemplate.sendBodyAndHeader(
            PersistenceEvent.PERSISTENCE_EVENT_TOPIC,
            ExchangePattern.InOnly,
            object,
            PersistenceEvent.PERSISTENCE_EVENT_HEADER,
            eventAction
        );
    }
}