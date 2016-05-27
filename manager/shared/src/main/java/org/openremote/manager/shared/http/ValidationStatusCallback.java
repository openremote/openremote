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
package org.openremote.manager.shared.http;

import elemental.json.JsonValue;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.validation.ConstraintViolationReport;

public class ValidationStatusCallback extends StatusCallback {

    final protected EntityReader<ConstraintViolationReport> violationReportEntityReader;

    public ValidationStatusCallback(EntityReader<ConstraintViolationReport> violationReportEntityReader,
                                    Consumer<Integer> onSuccess,
                                    Consumer<RequestException> onFailure) {
        super(onSuccess, onFailure);
        this.violationReportEntityReader = violationReportEntityReader;
    }

    public ValidationStatusCallback(EntityReader<ConstraintViolationReport> violationReportEntityReader,
                                    int expectedStatusCode,
                                    Consumer<Integer> onSuccess,
                                    Consumer<RequestException> onFailure) {
        super(expectedStatusCode, onSuccess, onFailure);
        this.violationReportEntityReader = violationReportEntityReader;
    }

    @Override
    protected ConstraintViolationReport getConstraintViolationReport(Object entity) {
        return violationReportEntityReader.read(((JsonValue) entity).toJson());
    }
}