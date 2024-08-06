var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import manager from "@openremote/core";
import { i18next } from "@openremote/or-translate";
export var DashboardSizeOption;
(function (DashboardSizeOption) {
    DashboardSizeOption[DashboardSizeOption["DESKTOP"] = 0] = "DESKTOP";
    DashboardSizeOption[DashboardSizeOption["MOBILE"] = 1] = "MOBILE";
})(DashboardSizeOption || (DashboardSizeOption = {}));
export class DashboardService {
    static create(dashboard, size = DashboardSizeOption.DESKTOP, realm = manager.displayRealm, post = true) {
        var _a;
        return __awaiter(this, void 0, void 0, function* () {
            const randomId = () => (Math.random() + 1).toString(36).substring(2);
            if (!dashboard) {
                dashboard = {
                    realm: realm,
                    displayName: this.getDefaultDisplayName(size),
                    template: {
                        id: randomId(),
                        columns: this.getDefaultColumns(size),
                        maxScreenWidth: this.getDefaultMaxScreenWidth(size),
                        refreshInterval: "OFF" /* DashboardRefreshInterval.OFF */,
                        screenPresets: this.getDefaultScreenPresets(size),
                    }
                };
            }
            else {
                dashboard.id = randomId();
                if (dashboard.template) {
                    dashboard.template.id = randomId();
                    (_a = dashboard.template.widgets) === null || _a === void 0 ? void 0 : _a.forEach(w => w.id = randomId());
                }
            }
            if (post) {
                return (yield manager.rest.api.DashboardResource.create(dashboard)).data;
            }
            else {
                return dashboard;
            }
        });
    }
    static delete(id, realm = manager.displayRealm) {
        return __awaiter(this, void 0, void 0, function* () {
            return (yield manager.rest.api.DashboardResource.delete(realm, id)).data;
        });
    }
    /* -------------------------------------------------------- */
    static getDefaultColumns(preset) {
        switch (preset) {
            case DashboardSizeOption.MOBILE: {
                return 4;
            }
            case DashboardSizeOption.DESKTOP: {
                return 12;
            }
            default: {
                return 12;
            }
        }
    }
    static getDefaultDisplayName(preset) {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: {
                return i18next.t('dashboard.initialName');
            }
            case DashboardSizeOption.MOBILE: {
                return i18next.t('dashboard.initialName') + " (" + i18next.t('dashboard.size.mobile') + ")";
            }
        }
    }
    static getDefaultScreenPresets(preset) {
        switch (preset) {
            case DashboardSizeOption.MOBILE: {
                return [{
                        id: "mobile",
                        displayName: 'dashboard.size.mobile',
                        breakpoint: 640,
                        scalingPreset: "KEEP_LAYOUT" /* DashboardScalingPreset.KEEP_LAYOUT */
                    }];
            }
            default: { // DashboardSizeOption.DESKTOP since that is the default
                return [{
                        id: "mobile",
                        displayName: 'dashboard.size.mobile',
                        breakpoint: 640,
                        scalingPreset: "WRAP_TO_SINGLE_COLUMN" /* DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN */
                    }];
            }
        }
    }
    static getDefaultMaxScreenWidth(preset) {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: return 4000;
            case DashboardSizeOption.MOBILE: return 640;
        }
    }
}
//# sourceMappingURL=dashboard-service.js.map