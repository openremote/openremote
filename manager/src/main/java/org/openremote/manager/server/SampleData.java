package org.openremote.manager.server;

import org.apache.log4j.Logger;
import org.openremote.manager.shared.model.inventory.Device;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import static org.openremote.manager.server.util.IdentifierUtil.generateGlobalUniqueId;

public class SampleData {

    private static final Logger LOG = Logger.getLogger(SampleData.class.getName());

    public void create(PersistenceService persistenceService) {
        LOG.info("--- CREATING TEST DATA ---");

        persistenceService.createSchema();

        EntityManager em = persistenceService.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Device a1 = new Device("My Device A", generateGlobalUniqueId(), "urn:org-openremote:device");
        a1.setSensorEndpoints(new String[]{
            "zwave://123/foo",
            "zwave://123/bar"
        });
        em.persist(a1);

        Device a2 = new Device("Device B", generateGlobalUniqueId(), "urn:org-openremote:device");
        em.persist(a2);

        tx.commit();
    }

    public void drop(PersistenceService persistenceService) {
        LOG.info("--- DROPPING TEST DATA ---");
        persistenceService.dropSchema();
    }
}
