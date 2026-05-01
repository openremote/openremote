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
import {Dashboard, DashboardRefreshInterval, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import manager, {Util} from "@openremote/core";
import {i18next} from "@openremote/or-translate";

export enum DashboardSizeOption {
    DESKTOP, MOBILE
}

export class DashboardService {

    public static async create(dashboard?: Dashboard, size: DashboardSizeOption = DashboardSizeOption.DESKTOP, realm: string = manager.displayRealm, post = true): Promise<Dashboard> {
        if(!dashboard) {
            dashboard = {
                realm: realm,
                displayName: this.getDefaultDisplayName(size),
                template: {
                    id: Util.generateUniqueUUID(),
                    columns: this.getDefaultColumns(size),
                    maxScreenWidth: this.getDefaultMaxScreenWidth(size),
                    refreshInterval: DashboardRefreshInterval.OFF,
                    screenPresets: this.getDefaultScreenPresets(size)
                }
            } as Dashboard;
        } else {
            dashboard.id = undefined;
            if(dashboard.template) {
                dashboard.template.id = Util.generateUniqueUUID();
                dashboard.template.widgets?.forEach(w => w.id = Util.generateUniqueUUID());
            }
        }

        if(post) {
            return (await manager.rest.api.DashboardResource.create(dashboard)).data;
        } else {
            return dashboard;
        }
    }

    public static async delete(id: string, realm: string = manager.displayRealm) {
        return (await manager.rest.api.DashboardResource.delete(realm, id)).data;
    }


    /* -------------------------------------------------------- */

    private static getDefaultColumns(preset: DashboardSizeOption): number {
        switch (preset) {
            case DashboardSizeOption.MOBILE: { return 4; }
            case DashboardSizeOption.DESKTOP: { return 12; }
            default: { return 12; }
        }
    }

    private static getDefaultDisplayName(preset: DashboardSizeOption): string {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: { return i18next.t('dashboard.initialName'); }
            case DashboardSizeOption.MOBILE: { return i18next.t('dashboard.initialName') + " (" + i18next.t('dashboard.size.mobile') + ")"; }
        }
    }

    private static getDefaultScreenPresets(preset: DashboardSizeOption): DashboardScreenPreset[] {
        switch (preset) {
            case DashboardSizeOption.MOBILE: {
                return [{
                    id: "mobile",
                    displayName: 'dashboard.size.mobile',
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }];
            }
            default: { // DashboardSizeOption.DESKTOP since that is the default
                return [{
                    id: "mobile",
                    displayName: 'dashboard.size.mobile',
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN
                }];
            }
        }
    }

    private static getDefaultMaxScreenWidth(preset: DashboardSizeOption): number {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: return 4000;
            case DashboardSizeOption.MOBILE: return 640;
        }
    }
}
