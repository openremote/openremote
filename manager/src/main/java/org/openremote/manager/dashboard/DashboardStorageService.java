package org.openremote.manager.dashboard;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardAccess;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardStorageService extends RouteBuilder implements ContainerService {

    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        persistenceService = container.getService(PersistenceService.class);
        container.getService(ManagerWebService.class).addApiSingleton(
                new DashboardResourceImpl(
                        container.getService(TimerService.class),
                        identityService,
                        this,
                        container.getService(MessageBrokerService.class)
                )
        );
    }

    @Override
    public void start(Container container) throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */

    }


    /* --------------------------  */

    // Getting ALL dashboards from a realm
    @SuppressWarnings("java:S2326")
    protected <T extends Dashboard> Dashboard[] findAllOfRealm(String realm, String userId) {
        Object[] result = persistenceService.doReturningTransaction(em -> {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Dashboard> cq = cb.createQuery(Dashboard.class);
                Root<Dashboard> root = cq.from(Dashboard.class);
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(root.get("realm"), realm));
                predicates.add(cb.or(
                        root.get("viewAccess").in(DashboardAccess.PUBLIC, DashboardAccess.SHARED),
                        cb.and(root.get("viewAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId))
                ));
                CriteriaQuery<Dashboard> all = cq.select(root).where(predicates.toArray(new Predicate[]{}));
                        //.where(cb.and(root.get("viewAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId)));
                TypedQuery<Dashboard> allQuery = em.createQuery(all);
                Dashboard[] dashboards = allQuery.getResultList().toArray(new Dashboard[0]);
                return dashboards;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ArrayList<Dashboard>().toArray(); // Empty array if nothing found.
        });
        // Object[] to Dashboard[]
        return Arrays.copyOf(result, result.length, Dashboard[].class);
    }

    @SuppressWarnings("java:S2326")
    protected <T extends Dashboard> Dashboard get(String id) {
        return persistenceService.doReturningTransaction(em -> em.find(Dashboard.class, id));
    }


    // Creation of initial dashboard (so not updating yet)
    protected <T extends Dashboard> T createNew(T dashboard) {
        return persistenceService.doReturningTransaction(em -> em.merge(dashboard));
    }

    protected <T extends Dashboard> T save(T dashboard) {
        return persistenceService.doReturningTransaction(em -> {
            try { em.merge(dashboard); }
            catch (Exception e) { e.printStackTrace(); }
            return dashboard;
        });
    }

    protected boolean delete(List<String> dashboardIds) {
        return persistenceService.doReturningTransaction(em -> {
            try {
                for(String id : dashboardIds) {
                    Query query = em.createQuery("DELETE FROM Dashboard d WHERE d.id = :id");
                    query.setParameter("id", id);
                    query.executeUpdate();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}
