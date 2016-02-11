package org.openremote.manager.server;

import elemental.json.Json;
import org.apache.log4j.Logger;
import org.openremote.manager.server.service.ContextBrokerService;
import org.openremote.manager.server.service.PersistenceService;
import org.openremote.manager.shared.model.ngsi.Attribute;
import org.openremote.manager.shared.model.ngsi.Entity;
import rx.Observable;

import static org.openremote.manager.server.util.IdentifierUtil.generateGlobalUniqueId;

public class SampleData {

    private static final Logger LOG = Logger.getLogger(SampleData.class.getName());

    public void create(ContextBrokerService contextBrokerService,
                       PersistenceService persistenceService) {
        LOG.info("--- CREATING SAMPLE DATA ---");

        Entity room1 = new Entity(Json.createObject());
        room1.setId(generateGlobalUniqueId());
        room1.setType("Room");
        room1.addAttribute(
            new Attribute("temperature", Json.createObject())
                .setType("float")
                .setValue(Json.create(21.3))
        ).addAttribute(
            new Attribute("label", Json.createObject())
                .setType("string")
                .setValue(Json.create("Office 123"))
        );

        Entity room2 = new Entity(Json.createObject());
        room2.setId(generateGlobalUniqueId());
        room2.setType("Room");
        room2.addAttribute(
            new Attribute("temperature", Json.createObject())
                .setType("float")
                .setValue(Json.create(22.1))
        ).addAttribute(
            new Attribute("label", Json.createObject())
                .setType("string")
                .setValue(Json.create("Office 456"))
        );


        contextBrokerService.getClient().listEntities()
            .flatMap(Observable::from)
            .flatMap(entity -> contextBrokerService.getClient().deleteEntity(entity))
            .toList().toBlocking().single();

        contextBrokerService.getClient().postEntity(room1).toBlocking().first();
        contextBrokerService.getClient().postEntity(room2).toBlocking().first();

        /* TODO enable if we use JPA
        persistenceService.createSchema();
        EntityManager em = persistenceService.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(someSampleData);
        tx.commit();
        */

    }

    public void drop(ContextBrokerService contextBrokerService,
                     PersistenceService persistenceService) {
        LOG.info("--- DROPPING SAMPLE DATA ---");

        /* TODO enable if we use JPA
        persistenceService.dropSchema();
        */
    }
}
