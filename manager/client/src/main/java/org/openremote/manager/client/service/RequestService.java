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
package org.openremote.manager.client.service;

import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.http.EntityReader;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.http.RequestException;
import org.openremote.manager.shared.http.RequestParams;

public interface RequestService {

    int ANY_STATUS_CODE = -1;

    void execute(Consumer<RequestParams<Void>> onRequest,
                 int expectedStatusCode,
                 Runnable onComplete,
                 Runnable onResponse,
                 Consumer<RequestException> onException);

    <OUT> void execute(EntityReader<OUT> entityReader,
                       Consumer<RequestParams<OUT>> onRequest,
                       int expectedStatusCode,
                       Runnable onComplete,
                       Consumer<OUT> onResponse,
                       Consumer<RequestException> onException);

    <IN> void execute(EntityWriter<IN> entityWriter,
                      Consumer<RequestParams<Void>> onRequest,
                      int expectedStatusCode,
                      Runnable onComplete,
                      Runnable onResponse,
                      Consumer<RequestException> onException);

    <IN, OUT> void execute(EntityReader<OUT> entityReader,
                           EntityWriter<IN> entityWriter,
                           Consumer<RequestParams<OUT>> onRequest,
                           int expectedStatusCode,
                           Runnable onComplete,
                           Consumer<OUT> onResponse,
                           Consumer<RequestException> onException);
}
