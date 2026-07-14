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

interface ReplayDatapoint {
    timestamp: number;
    value: any;
}

// Cache keyed by data array reference so re-renders return the same string object.
// Lit's property binding uses === to detect changes, so an identical reference
// prevents setting .value on the textarea (and its expensive internal string scan).
const textCache = new WeakMap<ReplayDatapoint[], string>();

function datapointsToText(data: ReplayDatapoint[] | undefined): string {
    if (!Array.isArray(data) || data.length === 0) return "";
    const cached = textCache.get(data);
    if (cached !== undefined) return cached;
    const parts = new Array<string>(data.length);
    for (let i = 0; i < data.length; i++) {
        parts[i] = `${data[i].timestamp}, ${data[i].value}`;
    }
    const text = parts.join("\n");
    textCache.set(data, text);
    return text;
}

function textToDatapoints(text: string): ReplayDatapoint[] {
    const result: ReplayDatapoint[] = [];
    const len = text.length;
    let lineStart = 0;

    while (lineStart < len) {
        let lineEnd = text.indexOf("\n", lineStart);
        if (lineEnd === -1) lineEnd = len;

        if (lineEnd > lineStart) {
            const commaIdx = text.indexOf(",", lineStart);
            if (commaIdx !== -1 && commaIdx < lineEnd) {
                const timestamp = Number(text.slice(lineStart, commaIdx).trim());
                if (!isNaN(timestamp)) {
                    const valueStr = text.slice(commaIdx + 1, lineEnd).trim();
                    const num = Number(valueStr);
                    const value: any = (valueStr !== "" && !isNaN(num)) ? num
                        : valueStr === "true" ? true
                        : valueStr === "false" ? false
                        : valueStr;
                    result.push({ timestamp, value });
                }
            }
        }

        lineStart = lineEnd + 1;
    }

    return result;
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
        const text = (event.target as HTMLInputElement).value;
        const datapoints = textToDatapoints(text);
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
            helper-text="Format: seconds, value — one entry per line"
            style="width: 100%;"
            .value="${textValue}"
            @change="${onChanged}"
        ></or-vaadin-text-area>
    `, deleteHandler);
};

export const replayDataRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: replayDataTester,
    renderer: replayDataRenderer
};
