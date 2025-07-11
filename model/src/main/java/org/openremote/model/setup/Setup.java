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
package org.openremote.model.setup;

/**
 * A task that runs when the application starts. Typically this means a procedure that
 *
 * <ul>
 *   <li>initializes some (database) state when the application starts for the first time
 *   <li>cleans up some state when the application is restarted (e.g. during development)
 *   <li>imports some state for demo/testing purposes
 * </ul>
 */
public interface Setup {

  default void onInit() throws Exception {}

  default void onStart() throws Exception {}
}
