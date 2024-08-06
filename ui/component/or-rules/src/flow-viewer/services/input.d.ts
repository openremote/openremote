/// <reference types="node" />
import { EventEmitter } from "events";
import { SelectableElement } from "../components/selectable-element";
export declare class Input extends EventEmitter {
    selected: SelectableElement[];
    selectables: SelectableElement[];
    private keysCurrentlyHeld;
    constructor();
    select(element: SelectableElement, forceMultipleSelection?: boolean): void;
    deselect(element: SelectableElement): void;
    handleSelection(element: SelectableElement, neverDeselect?: boolean): void;
    clearSelection(ignoreMultiselect?: boolean): void;
    isHeld(key: string): boolean;
    get multiSelectedEnabled(): boolean;
    private onkeydown;
    private onkeyup;
}
