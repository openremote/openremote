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
package org.openremote.app.client.simulator;

import com.google.gwt.user.client.ui.IsWidget;
import elemental.client.Browser;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.attributes.AttributeViewImpl;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.interop.Consumer;
import org.openremote.model.simulator.RequestSimulatorState;
import org.openremote.model.simulator.SimulatorElement;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.*;

public class Simulator extends AbstractAttributeViewExtension {

    final protected Environment environment;
    final protected AttributeRef protocolConfiguration;
    final protected Runnable onCreate;
    final protected Runnable onClose;
    protected EventRegistration<SimulatorState> eventRegistration;
    protected SimulatorState simulatorState;
    protected Map<AttributeRef, FormGroup> formGroups = new HashMap<>();
    final protected AttributeView.Style style;

    public Simulator(Environment environment, AttributeView.Style style, AttributeViewImpl parentView, AssetAttribute attribute, AttributeRef protocolConfiguration, Runnable onCreate, Runnable onClose) {
        super(environment, style, parentView, attribute, environment.getMessages().simulator());
        this.environment = environment;
        this.style = style;
        this.protocolConfiguration = protocolConfiguration;
        this.onCreate = onCreate;
        this.onClose = onClose;

        setStyleName("layout vertical or-Simulator");
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        createSimulator();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        destroySimulator();
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
        addLabel(environment.getMessages().simulator());
        formGroups.clear();

        List<SimulatorElement> sortedElements = Arrays.asList(simulatorState.getElements());
        sortedElements.sort(Comparator.comparing(o -> simulatorState.getElementName(o)));

        for (SimulatorElement element : sortedElements) {
            FormGroup formGroup = new FormGroup();

            String elementName = simulatorState.getElementName(element);
            FormLabel formLabel = new FormLabel(elementName);
            formLabel.addStyleName("larger");
            formGroup.setFormLabel(formLabel);

            FormField formField = new FormField();
            formGroup.setFormField(formField);

            // Don't push simulator value validation up to the presenter as it is a special case that should
            // just be evaluated in-situ and shouldn't invalidate the parent attribute
            Consumer<Value> onModified = value -> {
                element.setValue(value);
                List<ValidationFailure> failures = element.getValidationFailures();
                formGroup.setError(failures != null && !failures.isEmpty());
            };

            ValueType valueType = element.getExpectedType().getValueType();
            IsWidget editor = valueEditorSupplier.createValueEditor(element, valueType, style, parentView, onModified);
            formField.add(editor);
            formGroups.put(element.getAttributeRef(), formGroup);
            add(formGroup);
        }

        if (sortedElements.size() > 0) {
            FormGroup submitGroup = new FormGroup();
            submitGroup.getElement().getStyle().setWidth(80, com.google.gwt.dom.client.Style.Unit.PCT);

            FormField submitField = new FormField();
            submitGroup.setFormField(submitField);

            FormButton writeButton = new FormButton(environment.getMessages().writeSimulatorState());
            writeButton.setPrimary(true);
            writeButton.addClickHandler(event -> {
                if (isValid()) {
                    environment.getEventService().dispatch(simulatorState);
                    environment.getEventBus().dispatch(
                        new ShowSuccessEvent(environment.getMessages().simulatorStateSubmitted())
                    );
                }
            });
            submitField.add(writeButton);

            add(submitGroup);
        } else {
            add(new FormInlineLabel(environment.getMessages().noAttributesLinkedToSimulator()));
        }

        // "Blink" the editor so users know there might be a new value
        for (FormGroup formGroup : formGroups.values()) {
            formGroup.addStyleName(environment.getWidgetStyle().HighlightBackground());
        }
        Browser.getWindow().setTimeout(() -> {
            for (FormGroup formGroup : formGroups.values())
                formGroup.removeStyleName(environment.getWidgetStyle().HighlightBackground());
        }, 250);
    }

    protected boolean isValid() {
        return formGroups.values()
            .stream()
            .noneMatch(FormGroup::isError);
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {
        // Ignore any validation on the parent attribute
    }

    @Override
    public void onAttributeChanged(long timestamp) {
        // the host attribute change doesn't impact the simulator
    }

    @Override
    public void setBusy(boolean busy) {

    }
}
