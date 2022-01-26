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

import jline.internal.Log;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.apps.ConsoleAppConfig;
import org.openremote.model.util.ValueUtil;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.container.util.MapAccess.getString;

public class ConsoleAppService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ConsoleAppService.class.getName());

    public static final String CONSOLE_APP_CONFIG_ROOT = "CONSOLE_APP_CONFIG_ROOT";
    public static final String CONSOLE_APP_CONFIG_ROOT_DEFAULT = "manager/src/consoleappconfig";

    protected TimerService timerService;
    protected ManagerWebService managerWebService;
    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected Path consoleAppDocRoot;
    protected Map<String, ConsoleAppConfig> consoleAppConfigMap;

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

        consoleAppDocRoot = Paths.get(getString(container.getConfig(), CONSOLE_APP_CONFIG_ROOT, CONSOLE_APP_CONFIG_ROOT_DEFAULT));
        consoleAppConfigMap = new HashMap<>();
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public String[] getInstalled() throws Exception {
        return Stream.concat(
                Files.list(managerWebService.getBuiltInAppDocRoot()),
                Files.list(managerWebService.getCustomAppDocRoot()))
            .filter(Files::isDirectory)
            .map(dir -> dir.getFileName().toString())
            .toArray(String[]::new);
    }

    public ConsoleAppConfig getAppConfig(String realm) {
        return consoleAppConfigMap.computeIfAbsent(realm, key -> {
            try {
                return Files.list(consoleAppDocRoot)
                        .filter(dir -> dir.getFileName().toString().startsWith(key))
                        .map(dir -> {
                            try {
                                return ValueUtil.JSON.readValue(dir.toFile(), ConsoleAppConfig.class);
                            } catch (IOException e) {
                                throw new WebApplicationException(e);
                            }
                        })
                        .findFirst().orElseThrow(NotFoundException::new);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "consoleAppDocRoot=" + consoleAppDocRoot +
            '}';
    }
}
