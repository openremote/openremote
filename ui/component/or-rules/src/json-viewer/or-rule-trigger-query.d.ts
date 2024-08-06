import { GeoJSONPoint, RuleCondition, SunPositionTriggerPosition } from "@openremote/model";
import { LitElement, PropertyValues } from "lit";
import { TimeTriggerType } from "../index";
interface TimeTrigger {
    key: TimeTriggerType | SunPositionTriggerPosition;
    value: string;
}
export declare class OrRuleTriggerQuery extends LitElement {
    static get styles(): import("lit").CSSResult;
    condition: RuleCondition;
    readonly: boolean;
    protected selectedTrigger: TimeTrigger;
    protected triggerOptions: TimeTrigger[];
    constructor();
    updated(changedProperties: PropertyValues): void;
    initMap(): void;
    renderDialogHTML(point: GeoJSONPoint | undefined): void;
    render(): import("lit-html").TemplateResult<1>;
    setTrigger(trigger: TimeTrigger): void;
    setTime(time: string): void;
    setOffset(offset: number): void;
    setLocation(point: GeoJSONPoint): void;
    getSunPositions(): SunPositionTriggerPosition[];
    triggerToString(position: TimeTriggerType | SunPositionTriggerPosition): string;
}
export {};
