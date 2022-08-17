package org.openremote.manager.dashboard;

import com.google.gson.Gson;
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
import java.util.Collection;
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
    protected <T extends Dashboard> Dashboard[] findAllOfRealm(String realm, String userId) {
        return this.findAllOfRealm(realm, userId, false);
    }

    @SuppressWarnings("java:S2326")
    protected <T extends Dashboard> Dashboard[] findAllOfRealm(String realm, String userId, Boolean editable) {
        Object[] result = persistenceService.doReturningTransaction(em -> {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Dashboard> cq = cb.createQuery(Dashboard.class);
                Root<Dashboard> root = cq.from(Dashboard.class);
                List<Predicate> predicates = new ArrayList<>();
                if(realm != null) {
                    predicates.add(cb.like(root.get("realm"), realm));
                }
                if(editable) {
                    predicates.add(cb.or(
                            root.get("editAccess").in(DashboardAccess.PUBLIC, DashboardAccess.SHARED),
                            cb.and(root.get("editAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId))
                    ));
                } else {
                    predicates.add(cb.or(
                            root.get("viewAccess").in(DashboardAccess.PUBLIC, DashboardAccess.SHARED),
                            cb.and(root.get("viewAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId))
                    ));
                }
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


    // Creation of initial dashboard (so no updating!)
    protected <T extends Dashboard> T createNew(T dashboard) {
        return persistenceService.doReturningTransaction(em -> {
            if(dashboard.getId() != null && dashboard.getId().length() > 0) {
                Dashboard d = em.find(Dashboard.class, dashboard.getId()); // checking whether dashboard is already in database
                System.out.println(d);
                if(d != null) {
                    throw new IllegalArgumentException("This dashboard has already been created.");
                }
            }
            return em.merge(dashboard);
        });
    }

    // Update of an existing dashboard
    protected <T extends Dashboard> T update(T dashboard, String userId) {
        return persistenceService.doReturningTransaction(em -> {
            Dashboard d = em.find(Dashboard.class, dashboard.getId());
            System.out.println("StorageService update() has following dashboard:");
            System.out.println(new Gson().toJson(d));
            if(d != null) {
                if(d.getEditAccess() == DashboardAccess.PRIVATE) {
                    if(!(d.getOwnerId().equals(userId))) {
                        throw new IllegalArgumentException("You are not allowed to edit this dashboard!");
                    }
                }
                T merged = em.merge(dashboard);
                return merged;
            } else {
                throw new IllegalArgumentException("This dashboard does not exist!");
            }
        });
    }

    protected boolean delete(List<String> dashboardIds, String userId) {
        return persistenceService.doReturningTransaction(em -> {
            try {
                Dashboard[] dashboards = this.findAllOfRealm(null, userId, true); // Get dashboards that userId is able to EDIT.
                Collection<String> toDelete = new ArrayList<>();
                for(Dashboard d : dashboards) {
                    if(dashboardIds.contains(d.getId())) {
                        toDelete.add(d.getId());
                    }
                }
                if(toDelete.size() > 0) {
                    Query query = em.createQuery("DELETE from Dashboard d WHERE d.id in (?1)");
                    query.setParameter(1, toDelete);
                    query.executeUpdate();
                }
                /*for(String id : dashboardIds) {
                    Query query = em.createQuery("DELETE FROM Dashboard d WHERE d.id = :id");
                    query.setParameter("id", id);
                    query.executeUpdate();
                }*/
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}
