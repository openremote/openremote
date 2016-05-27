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
package org.openremote.manager.client.http;

import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;
import org.openremote.manager.shared.http.BadRequestException;
import org.openremote.manager.shared.http.RequestException;
import org.openremote.manager.shared.http.UnauthorizedRequestException;
import org.openremote.manager.shared.validation.ConstraintViolation;

public class ResourceUpdateExceptionHandler implements Consumer<RequestException> {

    protected final EventBus eventBus;
    protected final SecurityService securityService;
    protected final ManagerMessages managerMessages;
    protected final Runnable onComplete;
    protected final Consumer<ConstraintViolation[]> constraintViolationHandler;

    public ResourceUpdateExceptionHandler(EventBus eventBus,
                                          SecurityService securityService,
                                          ManagerMessages managerMessages,
                                          Runnable onComplete,
                                          Consumer<ConstraintViolation[]> constraintViolationHandler) {
        this.eventBus = eventBus;
        this.securityService = securityService;
        this.managerMessages = managerMessages;
        this.onComplete = onComplete;
        this.constraintViolationHandler = constraintViolationHandler;
    }

    @Override
    public void accept(RequestException ex) {
        if (ex instanceof UnauthorizedRequestException) {
            securityService.logout();
            return;
        }
        onComplete.run();

        if (ex instanceof BadRequestException) {
            BadRequestException badRequestException = (BadRequestException) ex;
            if (badRequestException.getConstraintViolationReport() != null
                && badRequestException.getConstraintViolationReport().hasViolations()
                && constraintViolationHandler != null) {
                constraintViolationHandler.accept(
                    badRequestException.getConstraintViolationReport().getAllViolations()
                );
                return;
            } else {
                eventBus.dispatch(new ShowFailureEvent(
                    managerMessages.failureUpdatingResourceBadRequest(),
                    5000
                ));
                return;
            }
        }
        eventBus.dispatch(new ShowFailureEvent(
            managerMessages.failureUpdatingResource(ex.getMessage()),
            10000
        ));
    }
}
