/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.apps;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

public class ConsoleAppService implements ContainerService {

  private static final Logger LOG = Logger.getLogger(ConsoleAppService.class.getName());

  protected TimerService timerService;
  protected ManagerWebService managerWebService;
  protected ManagerIdentityService identityService;
  protected PersistenceService persistenceService;
  Path consoleAppDocRoot;

  @Override
  public void init(Container container) throws Exception {

    this.timerService = container.getService(TimerService.class);
    this.managerWebService = container.getService(ManagerWebService.class);
    this.identityService = container.getService(ManagerIdentityService.class);
    this.persistenceService = container.getService(PersistenceService.class);

    container.getService(ManagerWebService.class).addApiSingleton(new AppResourceImpl(this));
  }

  @Override
  public void start(Container container) throws Exception {}

  @Override
  public void stop(Container container) throws Exception {}

  public String[] getInstalled() throws Exception {
    return Stream.concat(
            Files.list(managerWebService.getBuiltInAppDocRoot()),
            Files.list(managerWebService.getCustomAppDocRoot()))
        .filter(Files::isDirectory)
        .filter(path -> !new File(path.toString(), ".appignore").exists())
        .map(dir -> dir.getFileName().toString())
        .distinct()
        .toArray(String[]::new);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "consoleAppDocRoot=" + consoleAppDocRoot + '}';
  }
}
