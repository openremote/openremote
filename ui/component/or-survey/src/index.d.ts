import "@openremote/or-translate";
export interface OrComputeGridEventDetail {
}
export declare class OrComputeGridEvent extends CustomEvent<OrComputeGridEventDetail> {
    static readonly NAME = "or-asset-viewer-compute-grid-event";
    constructor();
}
export interface AnswerOption {
    value: string;
}
export interface SurveyAnswers {
    [key: string]: string | string[];
}
