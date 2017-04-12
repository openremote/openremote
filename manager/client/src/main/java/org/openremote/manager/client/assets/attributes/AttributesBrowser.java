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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import elemental.client.Browser;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.interop.chartjs.Chart;
import org.openremote.manager.client.interop.chartjs.ChartUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.*;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.event.bus.EventRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

public abstract class AttributesBrowser
    extends AttributesView<AttributesBrowser.Container, AttributesBrowser.Style, AssetAttributes, AssetAttribute> {

    private static final Logger LOG = Logger.getLogger(AttributesBrowser.class.getName());

    public interface Container extends AttributesView.Container<AttributesBrowser.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected Asset asset;
    final protected List<FormButton> readButtons = new ArrayList<>();

    protected FormGroup liveUpdatesGroup;
    protected EventRegistration<AttributeEvent> eventRegistration;

    public AttributesBrowser(Environment environment, Container container, Asset asset) {
        super(environment, container, new AssetAttributes(asset));
        this.asset = asset;

        eventRegistration = environment.getEventBus().register(
            AttributeEvent.class,
            event -> {
                for (AssetAttribute assetAttribute : attributeGroups.keySet()) {
                    if (assetAttribute.getReference().equals(event.getAttributeRef())) {
                        assetAttribute.setValueUnchecked(event.getValue(), event.getTimestamp());
                        rebuildEditor(assetAttribute);
                        break;
                    }
                }
            }
        );
    }

    @Override
    protected void createAttributeGroups() {
        if (getAttributes().size() > 0) {
            liveUpdatesGroup = createLiveUpdatesGroup();
            container.getPanel().add(liveUpdatesGroup);
        }
        super.createAttributeGroups();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        liveUpdatesGroup.setOpaque(opaque);
    }

    @Override
    public void close() {
        super.close();

        if (eventRegistration != null) {
            environment.getEventBus().remove(eventRegistration);
            eventRegistration = null;
        }

        subscribeToLiveUpdates(false);
    }

    @Override
    protected void addAttributeActions(AssetAttribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {

        // Allow direct read/write of attribute if we understand attribute type (have an editor)
        if (editor != null) {
            FormButton writeValueButton = new FormButton();
            writeValueButton.setEnabled(!attribute.isReadOnly());
            writeValueButton.setText(container.getMessages().write());
            writeValueButton.setPrimary(true);
            writeValueButton.setIcon("cloud-upload");
            writeValueButton.addClickHandler(clickEvent -> writeAttributeValue(attribute));
            formGroupActions.add(writeValueButton);

            FormButton readValueButton = new FormButton();
            readValueButton.setText(container.getMessages().read());
            readValueButton.setIcon("cloud-download");
            readValueButton.addClickHandler(clickEvent -> readAttributeValue(attribute));
            readButtons.add(readValueButton);
            formGroupActions.add(readValueButton);
        }
    }

    @Override
    protected void addAttributeExtensions(AssetAttribute attribute,
                                          FormGroup formGroup) {
        Widget datapointBrowser = createDatapointBrowser(attribute);
        if (datapointBrowser != null) {
            formGroup.addExtension(datapointBrowser);
        } else {
            formGroup.showDisabledExtensionToggle();
        }
    }

    /* ####################################################################### */

    protected void readAllAttributeValues() {
        environment.getEventService().dispatch(
            new ReadAttributesEvent(
                asset.getId(),
                attributeGroups.keySet().stream().map(Attribute::getName).collect(Collectors.toList())
            )
        );
    }

    protected void readAttributeValue(AssetAttribute attribute) {
        environment.getEventService().dispatch(
            new ReadAttributesEvent(attribute.getAssetId(), attribute.getName())
        );
    }

    protected FormGroup createLiveUpdatesGroup() {
        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = new FormLabel("Show live updates");
        formGroup.addFormLabel(formLabel);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormCheckBox liveUpdatesCheckBox = new FormCheckBox();
        liveUpdatesCheckBox.addValueChangeHandler(event -> subscribeToLiveUpdates(event.getValue()));
        formField.add(liveUpdatesCheckBox);

        if (attributes.size() > 0) {
            FormGroupActions formGroupActions = new FormGroupActions();

            FormButton readAllButton = new FormButton();
            readAllButton.setText("Read and refresh all attributes");
            readAllButton.addClickHandler(event -> readAllAttributeValues());
            formGroupActions.add(readAllButton);

            readButtons.add(readAllButton);

            formGroup.addFormGroupActions(formGroupActions);
        }

        return formGroup;
    }

    protected void subscribeToLiveUpdates(boolean enable) {
        for (FormButton readButton : readButtons) {
            readButton.setEnabled(!enable);
        }

        if (enable) {

            // Poll all values once so we have some state
            readAllAttributeValues();

            environment.getEventService().subscribe(
                AttributeEvent.class,
                new AttributeEvent.EntityIdFilter(asset.getId())
            );
        } else {
            environment.getEventService().unsubscribe(
                AttributeEvent.class
            );
        }
    }

    protected void rebuildEditor(AssetAttribute attribute) {
        FormGroup formGroup = attributeGroups.get(attribute);
        if (formGroup == null)
            return;
        FormField formField = formGroup.getFormField();
        if (formField.getWidgetCount() > 0) {
            formField.getWidget(0).removeFromParent();
        }
        final AttributeEditor editor = createEditor(attribute, formGroup);
        if (editor == null) {
            formField.add(createUnsupportedEditor(attribute));
            return;
        }

        formField.add(editor);

        // "Blink" the editor so users know there is a new value
        editor.asWidget().addStyleName(environment.getWidgetStyle().HighlightBackground());
        Browser.getWindow().setTimeout(() -> {
            editor.asWidget().removeStyleName(environment.getWidgetStyle().HighlightBackground());
        }, 250);
    }

    protected Widget createDatapointBrowser(AssetAttribute attribute) {
        if (!attribute.isStoreDatapoints())
            return null;

        class ChartPanel extends FlowPanel {

            Chart chart;
            FormValueListBox<DatapointInterval> intervalListBox;
            long lastUsedTimestamp;
            FormOutputText timeOutput;
            FormButton nextButton;

            public ChartPanel() {
                setStyleName("layout vertical");
                addAttachHandler(event -> {
                    if (event.isAttached()) {
                        createChart();
                    } else {
                        destroyChart();
                    }
                });
            }

            void createChart() {
                FormSectionLabel sectionLabel = new FormSectionLabel(environment.getMessages().historicalData());
                add(sectionLabel);

                Canvas canvas = Canvas.createIfSupported();
                if (canvas == null) {
                    add(new Label(environment.getMessages().canvasNotSupported()));
                    return;
                }

                FlowPanel charFormPanel = new FlowPanel();
                charFormPanel.setStyleName("layout vertical center or-FormGroup");
                add(charFormPanel);

                canvas.getCanvasElement().setWidth(900);
                canvas.getCanvasElement().setHeight(250);
                FlowPanel canvasContainer = new FlowPanel();
                canvasContainer.setWidth("900px");
                canvasContainer.setHeight("250px");
                canvasContainer.add(canvas);

                charFormPanel.add(canvasContainer);

                Form controlForm = new Form();
                controlForm.getElement().getStyle().setWidth(850, PX);
                charFormPanel.add(controlForm);

                FormGroup controlFormGroup = new FormGroup();
                controlForm.add(controlFormGroup);

                FormLabel controlFormLabel = new FormLabel(environment.getMessages().showChartAggregatedFor());
                controlFormGroup.addFormLabel(controlFormLabel);

                FormField controlFormField = new FormField();
                controlFormGroup.addFormField(controlFormField);
                intervalListBox = new FormValueListBox<>(
                    new AbstractRenderer<DatapointInterval>() {
                        @Override
                        public String render(DatapointInterval interval) {
                            return environment.getMessages().datapointInterval(interval.name());
                        }
                    }
                );
                intervalListBox.addValueChangeHandler(event -> {
                    updateChart(System.currentTimeMillis());
                });
                intervalListBox.setValue(DatapointInterval.HOUR);
                intervalListBox.setAcceptableValues(Arrays.asList(DatapointInterval.values()));
                intervalListBox.setEnabled(true);
                controlFormField.add(intervalListBox);

                FormGroupActions controlFormActions = new FormGroupActions();
                controlFormGroup.addFormGroupActions(controlFormActions);

                timeOutput = new FormOutputText();
                timeOutput.addStyleName("flex");
                controlFormActions.add(timeOutput);

                FormButton previousButton = new FormButton();
                previousButton.setIcon("arrow-circle-left");
                previousButton.setText(environment.getMessages().previous());
                previousButton.addClickHandler(event -> updateChart(calculateTimestamp(true)));
                controlFormActions.add(previousButton);

                nextButton = new FormButton();
                nextButton.setIcon("arrow-circle-right");
                nextButton.setText(environment.getMessages().next());
                nextButton.addClickHandler(event -> updateChart(calculateTimestamp(false)));
                controlFormActions.add(nextButton);

                chart = ChartUtil.createLineChart(canvas.getContext2d());

                updateChart(System.currentTimeMillis());
            }

            long calculateTimestamp(boolean subtract) {
                long hour = 1000 * 60 * 60;
                long day = hour * 24;
                long week = day * 7;
                long month = day * 30;
                long year = day * 365;
                switch (intervalListBox.getValue()) {
                    case HOUR:
                        return subtract ? (lastUsedTimestamp - hour) : (lastUsedTimestamp + hour);
                    case DAY:
                        return subtract ? (lastUsedTimestamp - day) : (lastUsedTimestamp + day);
                    case WEEK:
                        return subtract ? (lastUsedTimestamp - week) : (lastUsedTimestamp + week);
                    case MONTH:
                        return subtract ? (lastUsedTimestamp - month) : (lastUsedTimestamp + month);
                    case YEAR:
                        return subtract ? (lastUsedTimestamp - year) : (lastUsedTimestamp + year);
                    default:
                        throw new IllegalArgumentException("Unsupported time period: " + intervalListBox.getValue());
                }
            }

            void updateChart(long timestamp) {
                this.lastUsedTimestamp = timestamp;

                timeOutput.setText(
                    DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(new Date(timestamp))
                );

                nextButton.setEnabled(timestamp < System.currentTimeMillis());

                getNumberDatapoints(
                    attribute,
                    intervalListBox.getValue(),
                    timestamp,
                    numberDatapoints -> {
                        ChartUtil.update(
                            chart,
                            ChartUtil.convertLabels(numberDatapoints),
                            ChartUtil.convertData(numberDatapoints)
                        );
                    });
            }

            void destroyChart() {
                if (chart != null) {
                    chart.destroy();
                    chart = null;
                }
                clear();
            }
        }

        return new ChartPanel();
    }


    /* ####################################################################### */

    abstract protected void writeAttributeValue(AssetAttribute attribute);

    abstract protected void getNumberDatapoints(AssetAttribute assetAttribute,
                                                DatapointInterval interval,
                                                long timestamp,
                                                Consumer<NumberDatapoint[]> consumer);
}
