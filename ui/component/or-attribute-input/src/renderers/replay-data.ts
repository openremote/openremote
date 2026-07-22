/*
 * Copyright 2026, OpenRemote Inc.
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
import { html } from "lit";
import { i18next } from "@openremote/or-translate";
import {
    JsonFormsStateContext,
    getTemplateWrapper,
    JsonFormsRendererRegistryEntry,
    RankedTester,
    rankWith,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    schemaMatches,
} from "@openremote/or-json-forms";
import "@openremote/or-vaadin-components/or-vaadin-text-area";
import { SimulatorReplayDatapoint } from "@openremote/model";

// Cache keyed by data array reference so re-renders return the same string object.
// Lit's property binding uses === to detect changes, so an identical reference
// prevents setting .value on the textarea (and its expensive internal string scan).
const textCache = new WeakMap<SimulatorReplayDatapoint[], string>();

function datapointsToText(data: SimulatorReplayDatapoint[] | undefined): string {
    if (!Array.isArray(data) || data.length === 0) return "";
    const cached = textCache.get(data);
    if (cached !== undefined) return cached;
    const parts = new Array<string>(data.length);
    for (let i = 0; i < data.length; i++) {
        // JSON-encode the value so non-primitives and typed strings ("5", "true")
        // survive the text round trip. Numbers/booleans render bare, strings quoted.
        parts[i] = `${data[i].timestamp}, ${JSON.stringify(data[i].value)}`;
    }
    const text = parts.join("\n");
    textCache.set(data, text);
    return text;
}

interface ParseResult {
    datapoints: SimulatorReplayDatapoint[];
    /** 1-based line number of the first invalid line, or null when all lines parsed. */
    invalidLine: number | null;
}

function textToDatapoints(text: string): ParseResult {
    const datapoints: SimulatorReplayDatapoint[] = [];
    const len = text.length;
    let lineStart = 0;
    let line = 0;

    while (lineStart < len) {
        let lineEnd = text.indexOf("\n", lineStart);
        if (lineEnd === -1) lineEnd = len;
        line++;

        if (lineEnd > lineStart) {
            const commaIdx = text.indexOf(",", lineStart);
            if (commaIdx !== -1 && commaIdx < lineEnd) {
                const timestampStr = text.slice(lineStart, commaIdx).trim();
                const timestamp = Number(timestampStr);
                // Explicit empty check: Number("") is 0, not NaN.
                if (timestampStr === "" || !Number.isFinite(timestamp)) {
                    return { datapoints, invalidLine: line };
                }
                const valueStr = text.slice(commaIdx + 1, lineEnd).trim();
                const num = Number(valueStr);
                let value: any;
                if (valueStr !== "" && !isNaN(num)) {
                    value = num;
                } else {
                    // JSON covers booleans, quoted strings, objects/arrays;
                    // anything else is treated leniently as a bare string.
                    try {
                        value = JSON.parse(valueStr);
                    } catch {
                        value = valueStr;
                    }
                }
                datapoints.push({ timestamp, value });
            } else if (text.slice(lineStart, lineEnd).trim() !== "") {
                return { datapoints, invalidLine: line };
            }
        }

        lineStart = lineEnd + 1;
    }

    return { datapoints, invalidLine: null };
}

const replayDataTester: RankedTester = rankWith(
    6,
    schemaMatches((schema) => schema.format === "simulator-replay-data")
);

const replayDataRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const onChanged = (event: Event) => {
        const textArea = event.target as HTMLInputElement & { invalid: boolean; errorMessage: string };
        const { datapoints, invalidLine } = textToDatapoints(textArea.value);
        if (invalidLine !== null) {
            // Don't commit: the data stays untouched and, since no state changes,
            // the textarea keeps the user's draft instead of dropping bad lines.
            // Requires manual-validation, otherwise Vaadin's focusout auto-validation
            // (which runs after this change handler) resets invalid to false.
            textArea.errorMessage = i18next.t("simulatorReplayInvalidLine", { line: invalidLine });
            textArea.invalid = true;
            return;
        }
        textArea.invalid = false;
        props.handleChange(props.path, datapoints.length > 0 ? datapoints : undefined);
    };

    const textValue = datapointsToText(props.data);

    let deleteHandler: undefined | (() => void);
    if (!props.required && props.path) {
        deleteHandler = () => {
            props.handleChange(props.path, undefined);
        };
    }

    return getTemplateWrapper(html`
        <or-vaadin-text-area
            label="${props.label}"
            helper-text="${i18next.t("simulatorReplayFormatHelper")}"
            style="width: 100%;"
            min-rows="5"
            manualresize
            manual-validation
            .value="${textValue}"
            @change="${onChanged}"
        ></or-vaadin-text-area>
    `, deleteHandler);
};

export const replayDataRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: replayDataTester,
    renderer: replayDataRenderer
};
