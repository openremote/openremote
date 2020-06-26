/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.apps;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.apps.ConsoleAppConfig;

import java.nio.file.Files;
import java.util.logging.Logger;

public class ConsoleAppService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ConsoleAppService.class.getName());

    protected TimerService timerService;
    protected ManagerWebService managerWebService;
    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        this.timerService = container.getService(TimerService.class);
        this.managerWebService = container.getService(ManagerWebService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.persistenceService = container.getService(PersistenceService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new ConsoleAppResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public String[] getInstalled() throws Exception {
        return Files.list(managerWebService.getAppDocRoot())
            .filter(Files::isDirectory)
            .map(dir -> dir.getFileName().toString())
            .toArray(String[]::new);
    }

    public ConsoleAppConfig getAppConfig(String realm) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                        "select ac from ConsoleAppConfig ac " +
                                "where ac.realm = :realm",
                        ConsoleAppConfig.class)
                        .setParameter("realm", realm)
                        .getResultList()).stream().findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
