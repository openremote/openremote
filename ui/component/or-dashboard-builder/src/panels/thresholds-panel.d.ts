import { CSSResult, LitElement, PropertyValues, TemplateResult } from "lit";
export declare class ThresholdChangeEvent extends CustomEvent<[number, string][]> {
    static readonly NAME = "threshold-change";
    constructor(thresholds: [number, string][]);
}
export declare class TextColorsChangeEvent extends CustomEvent<[string, string][]> {
    static readonly NAME = "text-colors-change";
    constructor(textColors: [string, string][]);
}
export declare class BoolColorsChangeEvent extends CustomEvent<{
    type: string;
    false: string;
    true: string;
}> {
    static readonly NAME = "bool-colors-change";
    constructor(boolColors: {
        type: string;
        false: string;
        true: string;
    });
}
export declare class ThresholdsPanel extends LitElement {
    protected thresholds: [number, string][];
    protected textColors: [string, string][];
    protected boolColors: {
        type: string;
        false: string;
        true: string;
    };
    protected readonly min?: number;
    protected readonly max?: number;
    protected readonly valueType: string;
    static get styles(): CSSResult[];
    protected updated(changedProps: PropertyValues): void;
    protected render(): TemplateResult;
    protected removeThreshold(threshold: [any, string]): void;
    protected addThreshold(threshold: [any, string]): void;
    protected addNewThreshold(): void;
}
