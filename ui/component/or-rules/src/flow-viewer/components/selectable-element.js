var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement } from "lit";
import { property } from "lit/decorators.js";
import { input } from "./flow-editor";
import { i18next, translate } from "@openremote/or-translate";
export class SelectableElement extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.isSelected = false;
        this.onSelected = (e) => {
            if (e === this) {
                this.isSelected = true;
            }
        };
        this.onDeselected = (e) => {
            if (e === this) {
                this.isSelected = false;
            }
        };
        this.handleSelection = (event) => {
            if (event.buttons === 1) {
                input.handleSelection(this);
                event.stopPropagation();
            }
            else if (event.buttons === 2) {
                input.handleSelection(this, true);
            }
        };
    }
    get selected() {
        return this.isSelected;
    }
    get handle() {
        return this.selectableHandle;
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this.selected) {
            input.selected.splice(input.selected.indexOf(this), 1);
        }
        this.isSelected = false;
        input.removeListener("selected", this.onSelected);
        input.removeListener("deselected", this.onDeselected);
        input.selectables.splice(input.selectables.indexOf(this), 1);
    }
    setHandle(element) {
        if (this.selectableHandle) {
            this.selectableHandle.removeEventListener("mousedown", this.handleSelection);
        }
        element.addEventListener("mousedown", this.handleSelection);
        this.selectableHandle = element;
    }
    firstUpdated() {
        this.setHandle(this);
        input.selectables.push(this);
        input.addListener("selected", this.onSelected);
        input.addListener("deselected", this.onDeselected);
    }
}
__decorate([
    property({ type: Boolean })
], SelectableElement.prototype, "isSelected", void 0);
__decorate([
    property({ attribute: false })
], SelectableElement.prototype, "selectableHandle", void 0);
//# sourceMappingURL=selectable-element.js.map