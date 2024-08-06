import { Dashboard } from "@openremote/model";
export declare enum DashboardSizeOption {
    DESKTOP = 0,
    MOBILE = 1
}
export declare class DashboardService {
    static create(dashboard?: Dashboard, size?: DashboardSizeOption, realm?: string, post?: boolean): Promise<Dashboard>;
    static delete(id: string, realm?: string): Promise<void>;
    private static getDefaultColumns;
    private static getDefaultDisplayName;
    private static getDefaultScreenPresets;
    private static getDefaultMaxScreenWidth;
}
