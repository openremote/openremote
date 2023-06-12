package org.openremote.manager.dashboard;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

import java.util.*;

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

    // Querying dashboards from the database
    // userId is required for checking dashboard ownership. If userId is NULL, we assume the user is not logged in.
    // editable can be used to only return dashboards where the user has edit access.
    protected Dashboard[] query(List<String> dashboardIds, String realm, String userId, Boolean publicOnly, Boolean editable) {
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Dashboard> cq = cb.createQuery(Dashboard.class);
                Root<Dashboard> root = cq.from(Dashboard.class);

                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(root.get("realm"), realm));

                if(dashboardIds != null) {
                    predicates.add(root.get("id").in(dashboardIds));
                }
                // Apply EDIT ACCESS filters; always return PUBLIC dashboards, SHARED dashboards if access to the realm,
                // and PRIVATE if you are the creator (ownerId) of the dashboard.
                if(Boolean.TRUE.equals(editable)) {
                    if(publicOnly) {
                        predicates.add(cb.equal(root.get("editAccess"), DashboardAccess.PUBLIC));
                    } else {
                        predicates.add(cb.or(
                                root.get("editAccess").in(DashboardAccess.PUBLIC, (userId != null ? DashboardAccess.SHARED : null)),
                                cb.and(root.get("editAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId))
                        ));
                    }
                }
                // Apply VIEW ACCESS filters; always return PUBLIC dashboards, SHARED dashboards if access to the realm,
                // and PRIVATE if you are the creator (ownerId) of the dashboard.
                if(publicOnly) {
                    predicates.add(cb.equal(root.get("viewAccess"), DashboardAccess.PUBLIC));
                } else {
                    predicates.add(cb.or(
                            root.get("viewAccess").in(DashboardAccess.PUBLIC, (userId != null ? DashboardAccess.SHARED : null)),
                            cb.and(root.get("viewAccess").in(DashboardAccess.PRIVATE), root.get("ownerId").in(userId))
                    ));
                }

                CriteriaQuery<Dashboard> all = cq.select(root).where(predicates.toArray(new Predicate[]{}));
                return em.createQuery(all).getResultList().toArray(new Dashboard[0]);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Dashboard[0]; // Empty array if nothing found.
        });
    }

    // Method to check if a dashboardId actually exists in the database
    // Useful for when query() does not return any accessible dashboard for that user, and check if it does however exist.
    protected boolean exists(String dashboardId, String realm) {
        if(dashboardId == null) {
            throw new IllegalArgumentException("No dashboardId is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Dashboard> cq = cb.createQuery(Dashboard.class);
            Root<Dashboard> root = cq.from(Dashboard.class);
            return em.createQuery(cq.select(root)
                    .where(cb.like(root.get("realm"), realm))
                    .where(cb.like(root.get("id"), dashboardId))
            );
        }) != null;
    }


    // Creation of initial dashboard (so no updating!)
    protected Dashboard createNew(Dashboard dashboard) {
        if(dashboard == null) {
            throw new IllegalArgumentException("No dashboard is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {
            if(dashboard.getId() != null && dashboard.getId().length() > 0) {
                Dashboard d = em.find(Dashboard.class, dashboard.getId()); // checking whether dashboard is already in database
                if(d != null) {
                    throw new IllegalArgumentException("This dashboard has already been created.");
                }
            }
            return em.merge(dashboard);
        });
    }

    // Update of an existing dashboard
    protected Dashboard update(Dashboard dashboard, String realm, String userId) throws IllegalArgumentException {
        if(dashboard == null) {
            throw new IllegalArgumentException("No dashboard is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        if(userId == null) {
            throw new IllegalArgumentException("No userId is specified.");
        }
        Dashboard[] dashboards = this.query(Collections.singletonList(dashboard.getId()), realm, userId, false, true); // Get dashboards that userId is able to EDIT.
        if(dashboards != null && dashboards.length > 0) {
            Dashboard d = dashboards[0];
            return persistenceService.doReturningTransaction(em -> {
                dashboard.setVersion(d.getVersion());
                return em.merge(dashboard);
            });
        } else {
            throw new IllegalArgumentException("This dashboard does not exist!");
        }
    }

    protected boolean delete(String dashboardId, String realm, String userId) throws IllegalArgumentException {
        if(dashboardId == null) {
            throw new IllegalArgumentException("No dashboardId is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        if(userId == null) {
            throw new IllegalArgumentException("No userId is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {

            // Query the dashboards with the same ID (which is only 1), and that userId is able to EDIT
            Dashboard[] dashboards = this.query(Collections.singletonList(dashboardId), realm, userId, false, true);
            if(dashboards == null || dashboards.length == 0) {
                throw new IllegalArgumentException("No dashboards could be found.");
            }
            Query query = em.createQuery("DELETE from Dashboard d where d.id=?1");
            query.setParameter(1, dashboardId);
            query.executeUpdate();
            return true;
        });
    }
}
