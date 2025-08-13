import {Dashboard, DashboardRefreshInterval, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import manager from "@openremote/core";
import {i18next} from "@openremote/or-translate";

export enum DashboardSizeOption {
    DESKTOP, MOBILE
}

export class DashboardService {

    public static async create(dashboard?: Dashboard, size: DashboardSizeOption = DashboardSizeOption.DESKTOP, realm: string = manager.displayRealm, post = true): Promise<Dashboard> {
        const randomId = () => (Math.random() + 1).toString(36).substring(2);
        if(!dashboard) {
            dashboard = {
                realm: realm,
                displayName: this.getDefaultDisplayName(size),
                template: {
                    id: randomId(),
                    columns: this.getDefaultColumns(size),
                    maxScreenWidth: this.getDefaultMaxScreenWidth(size),
                    refreshInterval: DashboardRefreshInterval.OFF,
                    screenPresets: this.getDefaultScreenPresets(size),
                }
            } as Dashboard;
        } else {
            dashboard.id = undefined;
            if(dashboard.template) {
                dashboard.template.id = randomId();
                dashboard.template.widgets?.forEach(w => w.id = randomId());
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
