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
package org.openremote.app.client.interop.chartjs;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

/**
 * TODO Use JSInterop instead
 */
public class ChartUtil {

//    static public JavaScriptObject convertLabels(ValueDatapoint[] numberDatapoints) {
//        JsArrayString array = (JsArrayString) JsArrayString.createArray();
//        for (int i = 0; i < numberDatapoints.length; i++) {
//            ValueDatapoint numberDatapoint = numberDatapoints[i];
//            array.set(i, numberDatapoint.getLabel());
//        }
//        return array;
//    }

    static public JavaScriptObject convertData(ValueDatapoint[] dps) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < dps.length; i++) {
            ValueDatapoint dp = dps[i];
            Value value = dp.getValue();
            JSONObject obj = new JSONObject();
            obj.put("x", new JSONNumber(dp.getTimestamp()));
            obj.put("y", value != null ? new JSONNumber(Values.getNumber(value).orElse(0d)) : null);
            array.set(i, obj);
        }
        return array.getJavaScriptObject();
    }

    public native static void update(Chart chart, JavaScriptObject data) /*-{
        chart.data.datasets[0].data = data;
        chart.update();
    }-*/;

    public native static Chart createLineChart(Context2d canvasContext) /*-{
        return new $wnd.Chart(canvasContext, {
            type: 'line',
            data: {
                datasets: [
                    {
                        backgroundColor: "rgba(193, 215, 47, 0.1)",
                        borderColor: "#c1d72f",
                        pointBorderColor: "#cccccc",
                        pointBackgroundColor: "#374c4f",
                        pointHoverBackgroundColor: "#374c4f",
                        pointHoverBorderColor: "#c1d72f",
                        pointBorderWidth: 2,
                        pointHoverRadius: 4,
                        pointHoverBorderWidth: 4,
                        pointRadius: 4,
                        pointHitRadius: 20,
                        spanGaps: true
                    }
                ]
            },
            options: {
                legend: {
                    display: false
                },
                tooltips: {
                    backgroundColor: "rgba(0,0,0,0.8)",
                    titleFontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                    titleFontColor: "#ffffff",
                    titleFontSize: 10,
                    titleMarginBottom: 4,
                    bodyFontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                    bodyFontColor: "#c1d72f",
                    bodyFontSize: 16,
                    displayColors: false,
                    xPadding: 10,
                    yPadding: 10,
                    footerFontSize: 0,
                    callbacks: {
                        label: function (tooltipItem, data) {
                            return tooltipItem.yLabel; // Removes the colon before the label
                        },
                        footer: function () {
                            return " "; // Hack the broken vertical alignment of body with footerFontSize: 0
                        }
                    }
                },
                scales: {
                    yAxes: [{
                        ticks: {
                            fontColor: "#000",
                            fontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                            fontSize: 11.5,
                            fontStyle: "normal",
                            beginAtZero: true
                        },
                        gridLines: {
                            color: "#cccccc"
                        }
                    }],
                    xAxes: [{
                        type: "time",
                        time: {
                            unit: "minute",
                            parser: "x"
                        },
                        ticks: {
                            autoSkip: true,
                            maxTicksLimit: 30,
                            fontColor: "#000",
                            fontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                            fontSize: 9,
                            fontStyle: "normal"
                        },
                        gridLines: {
                            color: "#cccccc"
                        }
                    }]
                }
            }
        });
    }-*/;
}
