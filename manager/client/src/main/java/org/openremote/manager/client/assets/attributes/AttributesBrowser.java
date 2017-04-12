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

import static com.google.gwt.dom.client.Style.Unit.EM;
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
                        refreshAttribute(assetAttribute);
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

    protected void refreshAttribute(AssetAttribute attribute) {

        // Rebuild editor
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

        // "Blink" the editor so users know there might be a new value
        editor.asWidget().addStyleName(environment.getWidgetStyle().HighlightBackground());
        Browser.getWindow().setTimeout(() -> {
            editor.asWidget().removeStyleName(environment.getWidgetStyle().HighlightBackground());
        }, 250);

        // Refresh data point browser
        FormGroup attributeFormGroup = attributeGroups.get(attribute);
        if (attributeFormGroup != null) {
            DatapointBrowser datapointBrowser = attributeFormGroup.getExtensionOfType(DatapointBrowser.class);
            if (datapointBrowser != null) {
                datapointBrowser.updateChart(System.currentTimeMillis());
            }
        }
    }

    protected DatapointBrowser createDatapointBrowser(AssetAttribute attribute) {
        if (!attribute.isStoreDatapoints())
            return null;
        if (!(attribute.getType() == AttributeType.DECIMAL || attribute.getType() == AttributeType.INTEGER))
            return null;
        return new DatapointBrowser(attribute);
    }

    /* ####################################################################### */

    class DatapointBrowser extends FlowPanel {

        final AssetAttribute attribute;
        Chart chart;
        DatapointInterval interval;
        long timestamp;
        FormValueListBox<DatapointInterval> intervalListBox;
        FormOutputText timeOutput;
        FormButton nextButton;

        public DatapointBrowser(AssetAttribute attribute) {
            this.attribute = attribute;
            this.interval  = DatapointInterval.HOUR;
            this.timestamp = System.currentTimeMillis();

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

            FlowPanel chartFormPanel = new FlowPanel();
            chartFormPanel.setStyleName("layout vertical center or-FormGroup");
            add(chartFormPanel);

            canvas.getCanvasElement().setWidth(850);
            canvas.getCanvasElement().setHeight(250);
            FlowPanel canvasContainer = new FlowPanel();
            canvasContainer.setWidth("850px");
            canvasContainer.setHeight("250px");
            canvasContainer.add(canvas);

            chartFormPanel.add(canvasContainer);

            Form controlForm = new Form();
            controlForm.getElement().getStyle().setWidth(850, PX);
            controlForm.getElement().getStyle().setMarginTop(0.4, EM);
            controlForm.getElement().getStyle().setMarginBottom(0.4, EM);
            chartFormPanel.add(controlForm);

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
                updateChart(timestamp);
            });
            intervalListBox.setValue(interval);
            intervalListBox.setAcceptableValues(Arrays.asList(DatapointInterval.values()));
            intervalListBox.setEnabled(true);
            controlFormField.add(intervalListBox);

            FormGroupActions controlFormActions = new FormGroupActions();
            controlFormGroup.addFormGroupActions(controlFormActions);

            timeOutput = new FormOutputText();
            timeOutput.addStyleName("flex");
            timeOutput.getElement().getStyle().setFontSize(0.8, EM);
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

            updateChart(timestamp);
        }

        long calculateTimestamp(boolean subtract) {
            long hour = 1000 * 60 * 60;
            long day = hour * 24;
            long week = day * 7;
            long month = day * 30;
            long year = day * 365;
            switch (intervalListBox.getValue()) {
                case HOUR:
                    return subtract ? (timestamp - hour) : (timestamp + hour);
                case DAY:
                    return subtract ? (timestamp - day) : (timestamp + day);
                case WEEK:
                    return subtract ? (timestamp - week) : (timestamp + week);
                case MONTH:
                    return subtract ? (timestamp - month) : (timestamp + month);
                case YEAR:
                    return subtract ? (timestamp - year) : (timestamp + year);
                default:
                    throw new IllegalArgumentException("Unsupported time period: " + intervalListBox.getValue());
            }
        }

        void updateChart(long timestamp) {
            if (chart == null) {
                return;
            }

            this.timestamp = timestamp;
            this.interval = intervalListBox.getValue();

            timeOutput.setText(
                DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(new Date(timestamp))
            );

            getNumberDatapoints(
                attribute,
                interval,
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
            }
            chart = null;
            intervalListBox = null;
            timeOutput = null;
            nextButton = null;
            clear();
        }
    }

    /* ####################################################################### */

    abstract protected void writeAttributeValue(AssetAttribute attribute);

    abstract protected void getNumberDatapoints(AssetAttribute assetAttribute,
                                                DatapointInterval interval,
                                                long timestamp,
                                                Consumer<NumberDatapoint[]> consumer);
}
