import { LitElement } from "lit";
import {property} from "lit/decorators.js";
import { input } from "./flow-editor";
import { i18next, translate } from "@openremote/or-translate";

export class SelectableElement extends translate(i18next)(LitElement) {
    public get selected() {
        return this.isSelected;
    }

    public get handle() {
        return this.selectableHandle;
    }
    @property({ type: Boolean }) private isSelected = false;
    @property({ attribute: false }) private selectableHandle!: HTMLElement;

    public disconnectedCallback() {
        super.disconnectedCallback();
        if (this.selected) {
            input.selected.splice(input.selected.indexOf(this), 1);
        }
        this.isSelected = false;
        input.removeListener("selected", this.onSelected);
        input.removeListener("deselected", this.onDeselected);
        input.selectables.splice(input.selectables.indexOf(this), 1);
    }

    public setHandle(element: HTMLElement) {
        if (this.selectableHandle) {
            this.selectableHandle.removeEventListener("mousedown", this.handleSelection);
        }
        element.addEventListener("mousedown", this.handleSelection);
        this.selectableHandle = element;
    }

    protected firstUpdated() {
        this.setHandle(this);
        input.selectables.push(this);
        input.addListener("selected", this.onSelected);
        input.addListener("deselected", this.onDeselected);
    }

    private readonly onSelected = (e: HTMLElement) => {
        if (e === this) {
            this.isSelected = true;
        }
    }

    private readonly onDeselected = (e: HTMLElement) => {
        if (e === this) {
            this.isSelected = false;
        }
    }

    private readonly handleSelection = (event: MouseEvent) => {
        if (event.buttons === 1) {
            input.handleSelection(this);
            event.stopPropagation();
        } else if (event.buttons === 2) {
            input.handleSelection(this, true);
        }
    }
}
