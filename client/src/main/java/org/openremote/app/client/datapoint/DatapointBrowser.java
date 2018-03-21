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
package org.openremote.app.client.datapoint;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import elemental.client.Browser;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.attributes.AttributeViewImpl;
import org.openremote.app.client.interop.chartjs.Chart;
import org.openremote.app.client.interop.chartjs.ChartUtil;
import org.openremote.app.client.widget.*;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.interop.Consumer;

import java.util.Arrays;
import java.util.Date;

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.PX;

public abstract class DatapointBrowser extends AbstractAttributeViewExtension {

    final Environment environment;
    final int width;
    final int height;

    DatapointInterval interval;
    long timestamp;

    Chart chart;
    FormValueListBox<DatapointInterval> intervalListBox;
    FormOutputText timeOutput;
    FormButton nextButton;

    public DatapointBrowser(Environment environment, AttributeView.Style style, AttributeViewImpl parentView, AssetAttribute attribute, int width, int height, DatapointInterval interval, long timestamp) {
        super(environment, style, parentView, attribute, environment.getMessages().historicalData());
        this.environment = environment;
        this.width = width;
        this.height = height;
        this.interval = interval;
        this.timestamp = timestamp;

        setStyleName("layout vertical or-DatapointBrowser");
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        createChart();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        destroyChart();
    }

    @Override
    public void onValidationStateChange(AttributeValidationResult validationResult) {

    }

    @Override
    public void onAttributeChanged(long timestamp) {
        refresh(timestamp);
    }

    @Override
    public void setBusy(boolean busy) {

    }

    public void refresh(long timestamp) {
        // Keep timestamp so when chart is != null at some point, it has the latest refresh timestamp
        this.timestamp = timestamp;

        if (chart == null) {
            return;
        }
        refresh(timestamp, intervalListBox.getValue());
    }

    public void refresh(long timestamp, DatapointInterval interval) {
        // Keep timestamp so when chart is != null at some point, it has the latest refresh timestamp
        this.timestamp = timestamp;
        this.interval = interval;

        if (chart == null) {
            return;
        }

        timeOutput.setText(
            DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(new Date(timestamp))
        );

        queryDatapoints(interval, timestamp, numberDatapoints -> ChartUtil.update(
            chart,
            ChartUtil.convertLabels(numberDatapoints),
            ChartUtil.convertData(numberDatapoints)
        ));
    }

    protected void createChart() {

        addLabel(environment.getMessages().historicalData());

        Canvas canvas = Canvas.createIfSupported();
        if (canvas == null) {
            add(new Label(environment.getMessages().canvasNotSupported()));
            return;
        }

        canvas.getCanvasElement().setWidth(width);
        canvas.getCanvasElement().setHeight(height);
        FlowPanel canvasContainer = new FlowPanel();
        canvasContainer.setWidth(width + "px");
        canvasContainer.setHeight(height + "px");
        canvasContainer.getElement().getStyle().setProperty("margin", "0 auto");
        canvasContainer.add(canvas);
        add(canvasContainer);

        Form controlForm = new Form();
        controlForm.getElement().getStyle().setWidth(width, PX);
        controlForm.getElement().getStyle().setMarginTop(0.4, EM);
        controlForm.getElement().getStyle().setProperty("margin", "0 auto");
        add(controlForm);

        FormGroup controlFormGroup = new FormGroup();
        controlForm.add(controlFormGroup);

        FormLabel controlFormLabel = new FormLabel(environment.getMessages().showChartAggregatedFor());
        controlFormLabel.addStyleName("end-justified");
        controlFormGroup.setFormLabel(controlFormLabel);

        FormField controlFormField = new FormField();
        controlFormGroup.setFormField(controlFormField);
        intervalListBox = new FormValueListBox<>(
            new AbstractRenderer<DatapointInterval>() {
                @Override
                public String render(DatapointInterval interval) {
                    return environment.getMessages().datapointInterval(interval.name());
                }
            }
        );
        intervalListBox.addValueChangeHandler(event -> refresh(timestamp));
        intervalListBox.setValue(interval);
        intervalListBox.setAcceptableValues(Arrays.asList(DatapointInterval.values()));
        intervalListBox.setEnabled(true);
        controlFormField.add(intervalListBox);

        timeOutput = new FormOutputText();
        timeOutput.addStyleName("flex");
        timeOutput.getElement().getStyle().setFontSize(0.8, EM);
        controlFormField.add(timeOutput);

        FormGroupActions controlFormActions = new FormGroupActions();
        controlFormGroup.setFormGroupActions(controlFormActions);

        FormButton previousButton = new FormButton();
        previousButton.setIcon("arrow-circle-left");
        previousButton.setText(environment.getMessages().previous());
        previousButton.addClickHandler(event -> refresh(calculateTimestamp(true)));
        controlFormActions.add(previousButton);

        nextButton = new FormButton();
        nextButton.setIcon("arrow-circle-right");
        nextButton.setText(environment.getMessages().next());
        nextButton.addClickHandler(event -> refresh(calculateTimestamp(false)));
        controlFormActions.add(nextButton);

        chart = ChartUtil.createLineChart(canvas.getContext2d());

        // TODO: Ugly, sometimes the chart is not ready (chart == undefined but !null in Java...) so we wait a bit
        Browser.getWindow().setTimeout(() -> refresh(timestamp), 50);
    }

    protected long calculateTimestamp(boolean subtract) {
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

    protected void destroyChart() {
        if (chart != null) {
            chart.destroy();
        }
        chart = null;
        intervalListBox = null;
        timeOutput = null;
        nextButton = null;
        clear();
    }

    abstract protected void queryDatapoints(DatapointInterval interval,
                                            long timestamp,
                                            Consumer<NumberDatapoint[]> consumer);
}