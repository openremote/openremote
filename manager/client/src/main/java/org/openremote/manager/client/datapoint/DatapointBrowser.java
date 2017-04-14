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
package org.openremote.manager.client.datapoint;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.interop.chartjs.Chart;
import org.openremote.manager.client.interop.chartjs.ChartUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.Constants;
import org.openremote.model.Consumer;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;

import java.util.Arrays;
import java.util.Date;

import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.PX;

public abstract class DatapointBrowser extends FlowPanel {

    final ManagerMessages messages;
    final int width;
    final int height;

    DatapointInterval interval;
    long timestamp;

    Chart chart;
    FormValueListBox<DatapointInterval> intervalListBox;
    FormOutputText timeOutput;
    FormButton nextButton;

    public DatapointBrowser(ManagerMessages messages, int width, int height) {
        this(messages, width, height, DatapointInterval.HOUR, System.currentTimeMillis());
    }

    public DatapointBrowser(ManagerMessages messages, int width, int height, DatapointInterval interval, long timestamp) {
        this.messages = messages;
        this.width = width;
        this.height = height;
        this.interval = interval;
        this.timestamp = timestamp;

        setStyleName("layout vertical center or-DatapointBrowser");

        addAttachHandler(event -> {
            if (event.isAttached()) {
                createChart();
            } else {
                destroyChart();
            }
        });
    }

    public void refresh(long timestamp) {
        if (chart == null) {
            return;
        }
        refresh(timestamp, intervalListBox.getValue());
    }

    public void refresh(long timestamp, DatapointInterval interval) {
        if (chart == null) {
            return;
        }

        this.timestamp = timestamp;
        this.interval = interval;

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

        Canvas canvas = Canvas.createIfSupported();
        if (canvas == null) {
            add(new Label(messages.canvasNotSupported()));
            return;
        }

        canvas.getCanvasElement().setWidth(width);
        canvas.getCanvasElement().setHeight(height);
        FlowPanel canvasContainer = new FlowPanel();
        canvasContainer.setWidth(width + "px");
        canvasContainer.setHeight(height + "px");
        canvasContainer.add(canvas);
        add(canvasContainer);

        Form controlForm = new Form();
        controlForm.getElement().getStyle().setWidth(width, PX);
        controlForm.getElement().getStyle().setMarginTop(0.4, EM);
        add(controlForm);

        FormGroup controlFormGroup = new FormGroup();
        controlForm.add(controlFormGroup);

        FormLabel controlFormLabel = new FormLabel(messages.showChartAggregatedFor());
        controlFormGroup.addFormLabel(controlFormLabel);

        FormField controlFormField = new FormField();
        controlFormGroup.addFormField(controlFormField);
        intervalListBox = new FormValueListBox<>(
            new AbstractRenderer<DatapointInterval>() {
                @Override
                public String render(DatapointInterval interval) {
                    return messages.datapointInterval(interval.name());
                }
            }
        );
        intervalListBox.addValueChangeHandler(event -> refresh(timestamp));
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
        previousButton.setText(messages.previous());
        previousButton.addClickHandler(event -> refresh(calculateTimestamp(true)));
        controlFormActions.add(previousButton);

        nextButton = new FormButton();
        nextButton.setIcon("arrow-circle-right");
        nextButton.setText(messages.next());
        nextButton.addClickHandler(event -> refresh(calculateTimestamp(false)));
        controlFormActions.add(nextButton);

        chart = ChartUtil.createLineChart(canvas.getContext2d());

        refresh(timestamp);
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