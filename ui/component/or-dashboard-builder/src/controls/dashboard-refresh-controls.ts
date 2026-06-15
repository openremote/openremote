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
import {html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {DashboardRefreshInterval} from "@openremote/model";
import {createMenuBarItem, MenuBarItem} from "@openremote/or-vaadin-components/or-vaadin-menu-bar";

export function intervalToMillis(interval: DashboardRefreshInterval): number | undefined {
    switch (interval) {
        case DashboardRefreshInterval.OFF:
            return undefined;
        case DashboardRefreshInterval.ONE_MIN:
            return (60 * 1000);
        case DashboardRefreshInterval.FIVE_MIN:
            return (5 * 60 * 1000);
        case DashboardRefreshInterval.QUARTER:
            return (15 * 60 * 1000);
        case DashboardRefreshInterval.ONE_HOUR:
            return (60 * 60 * 1000);
        default:
            return undefined;
    }
}

export class IntervalSelectEvent extends CustomEvent<DashboardRefreshInterval> {

    public static readonly NAME = "interval-select";

    constructor(interval: DashboardRefreshInterval) {
        super(IntervalSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: interval
        });
    }
}

@customElement("dashboard-refresh-controls")
export class DashboardRefreshControls extends LitElement {

    @property()
    protected interval: DashboardRefreshInterval = DashboardRefreshInterval.OFF;

    @property()
    protected readonly = true;

    // TODO: Replace this with Object.values(), after generated typescript enums support this. ('const' variable issue in model.ts)
    protected intervalOptions = [DashboardRefreshInterval.OFF, DashboardRefreshInterval.ONE_MIN, DashboardRefreshInterval.FIVE_MIN, DashboardRefreshInterval.QUARTER, DashboardRefreshInterval.ONE_HOUR]

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if (changedProps.has("interval") && this.interval !== undefined) {
            this.dispatchEvent(new IntervalSelectEvent(this.interval));
        }
    }

    protected render(): TemplateResult {
        const intervalOptions: string[] = this.getRefreshOptions();
        const menuItems: MenuBarItem[] = [{
            component: createMenuBarItem(when(this.interval === DashboardRefreshInterval.OFF,
                    () => html`<or-icon icon="pause"></or-icon>`,
                    () => html`<or-translate value=${this.getIntervalString(this.interval)}></or-translate>`)
            ),
            children: intervalOptions.map(o => ({interval: o, component: createMenuBarItem(html`<or-translate value=${o}></or-translate>`)}))
        }]
        return html`
            <or-vaadin-menu-bar .items=${menuItems} ?disabled=${this.readonly}
                                @item-selected=${(ev: CustomEvent)=> this.onIntervalSelect(intervalOptions, ev.detail.value.interval as string)}
            ></or-vaadin-menu-bar>
        `;
    }

    protected onIntervalSelect(stringOptions: string[], value: string) {
        this.interval = this.intervalOptions[stringOptions.indexOf(value as DashboardRefreshInterval)];
    }

    protected getIntervalString(interval: DashboardRefreshInterval): string {
        return (`dashboard.interval.${interval.toLowerCase()}`);
    }

    protected getRefreshOptions(): string[] {
        return this.intervalOptions.map(interval => this.getIntervalString(interval));
    }
}
