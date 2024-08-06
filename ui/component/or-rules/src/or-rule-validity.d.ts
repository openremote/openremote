import { LitElement, PropertyValues, TemplateResult } from "lit";
import { CalendarEvent, RulesetUnion } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import { OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { ByWeekday, RRule } from 'rrule';
declare const OrRuleValidity_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleValidity extends OrRuleValidity_base {
    ruleset?: RulesetUnion;
    protected dialog?: OrMwcDialog;
    protected _validity?: CalendarEvent;
    protected _rrule?: RRule;
    protected _dialog?: OrMwcDialog;
    constructor();
    static styles: import("lit").CSSResult;
    protected updated(changedProps: PropertyValues): void;
    getWeekDay(weekday: string): ByWeekday | undefined;
    isAllDay(): boolean | undefined;
    protected setRRuleValue(value: any, key: string): void;
    timeLabel(): string | undefined;
    setValidityType(value: any): void;
    getValidityType(): "validityAlways" | "validityRecurrence" | "validityPeriod";
    protected render(): TemplateResult<1>;
    protected showDialog(): void;
    protected getDialogContent(): TemplateResult;
}
export {};
