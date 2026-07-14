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

function datapointsToText(data: ReplayDatapoint[] | undefined): string {
    if (!Array.isArray(data) || data.length === 0) return "";
    return data.map(dp => `${dp.timestamp}, ${dp.value}`).join("\n");
}

function textToDatapoints(text: string): ReplayDatapoint[] {
    return text.split("\n")
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .flatMap(line => {
            const commaIdx = line.indexOf(",");
            if (commaIdx < 0) return [];
            const timestamp = Number(line.slice(0, commaIdx).trim());
            if (isNaN(timestamp)) return [];
            const valueStr = line.slice(commaIdx + 1).trim();
            let value: any = valueStr;
            const num = Number(valueStr);
            if (valueStr !== "" && !isNaN(num)) value = num;
            else if (valueStr === "true") value = true;
            else if (valueStr === "false") value = false;
            return [{ timestamp, value }];
        });
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
