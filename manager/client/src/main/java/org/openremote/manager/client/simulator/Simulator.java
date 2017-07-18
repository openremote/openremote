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
package org.openremote.manager.client.simulator;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import elemental.client.Browser;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.widget.*;
import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.simulator.RequestSimulatorState;
import org.openremote.model.simulator.SimulatorElement;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.function.Consumer;

import static org.openremote.manager.client.widget.ValueEditors.*;

public class Simulator extends FlowPanel implements IsWidget {

    final protected Environment environment;
    final protected AttributeRef protocolConfiguration;
    final protected Runnable onCreate;
    final protected Runnable onClose;
    protected EventRegistration<SimulatorState> eventRegistration;
    protected SimulatorState simulatorState;
    protected Map<AttributeRef, FormGroup> formGroups = new HashMap<>();

    public Simulator(Environment environment, AttributeRef protocolConfiguration, Runnable onCreate, Runnable onClose) {
        this.environment = environment;
        this.protocolConfiguration = protocolConfiguration;
        this.onCreate = onCreate;
        this.onClose = onClose;

        setStyleName("layout vertical center or-Simulator");

        addAttachHandler(event -> {
            if (event.isAttached()) {
                createSimulator();
            } else {
                destroySimulator();
            }
        });
    }

    protected void createSimulator() {
        eventRegistration = environment.getEventBus().register(SimulatorState.class, simulatorState -> {
            if (!simulatorState.getProtocolConfigurationRef().equals(protocolConfiguration))
                return;
            this.simulatorState = simulatorState;
            writeView();
        });
        environment.getEventService().dispatch(new RequestSimulatorState(protocolConfiguration));
        onCreate.run();
    }

    protected void destroySimulator() {
        clear();
        if (eventRegistration != null) {
            environment.getEventBus().remove(eventRegistration);
        }
        onClose.run();
    }

    protected void writeView() {
        clear();
        formGroups.clear();

        List<SimulatorElement> sortedElements = Arrays.asList(simulatorState.getElements());
        Collections.sort(sortedElements, Comparator.comparing(o -> simulatorState.getElementName(o)));

        for (SimulatorElement element : sortedElements) {
            FormGroup formGroup = new FormGroup();

            formGroup.getElement().getStyle().setWidth(80, Style.Unit.PCT);

            String elementName = simulatorState.getElementName(element);
            FormLabel formLabel = new FormLabel(elementName);
            formLabel.addStyleName("larger");
            formGroup.addFormLabel(formLabel);

            FormField formField = new FormField();
            formGroup.addFormField(formField);

            Consumer<List<ValidationFailure>> validationResultConsumer =
                failures -> {
                    for (ValidationFailure failure : failures) {
                        showValidationError(environment, elementName, failure);
                    }
                };

            IsWidget editor;
            ValueType valueType = element.getExpectedType().getValueType();
            if (valueType.equals(ValueType.STRING)) {
                String currentValue = element.getValue().map(Object::toString).orElse(null);
                 editor = createStringEditor(
                    element, currentValue, null, false, "or-SimulatorElement", formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.NUMBER)) {
                String currentValue = element.getValue().map(Object::toString).orElse(null);
                editor =  createNumberEditor(
                    element, currentValue, null, false, "or-SimulatorElement", formGroup, false, validationResultConsumer
                );
            } else if (valueType.equals(ValueType.BOOLEAN)) {
                Boolean currentValue = element.getValueAsBoolean().orElse(null);
                editor =  createBooleanEditor(
                    element, currentValue, null, false, "or-SimulatorElement", formGroup, false, validationResultConsumer
                );
                // TODO Support JSON editors
            } else {
                editor= new FormOutputText(
                    environment.getMessages().unsupportedValueType(valueType.name())
                );
            }
            formField.add(editor);

            formGroups.put(element.getAttributeRef(), formGroup);
            add(formGroup);
        }

        FormGroup submitGroup = new FormGroup();
        submitGroup.getElement().getStyle().setWidth(80, Style.Unit.PCT);

        FormField submitField = new FormField();
        submitGroup.addFormField(submitField);

        FormButton writeButton = new FormButton(environment.getMessages().writeSimulatorState());
        writeButton.setPrimary(true);
        writeButton.addClickHandler(event -> {
            if (validateElements()) {
                environment.getEventService().dispatch(simulatorState);
                environment.getEventBus().dispatch(
                    new ShowSuccessEvent(environment.getMessages().simulatorStateSubmitted())
                );
            }
        });
        submitField.add(writeButton);

        add(submitGroup);

        // "Blink" the editor so users know there might be a new value
        for (FormGroup formGroup : formGroups.values()) {
            formGroup.addStyleName(environment.getWidgetStyle().HighlightBackground());
        }
        Browser.getWindow().setTimeout(() -> {
            for (FormGroup formGroup : formGroups.values())
                formGroup.removeStyleName(environment.getWidgetStyle().HighlightBackground());
        }, 250);
    }

    protected boolean validateElements() {
        boolean isValid = true;
        for (SimulatorElement element : simulatorState.getElements()) {
            if (!validateElement(element))
                isValid = false;
        }
        if (!isValid) {
            showValidationError(environment, environment.getMessages().invalidValues());
        }
        return isValid;
    }

    public boolean validateElement(SimulatorElement element) {
        // If there is already an error displayed, don't do other validation
        FormGroup formGroup = formGroups.get(element.getAttributeRef());
        if (formGroup.isError()) {
            return false;
        }
        List<ValidationFailure> failures = element.getValidationFailures();
        if (!failures.isEmpty()) {
            formGroups.get(element.getAttributeRef()).setError(true);
        }
        return true;
    }

}
