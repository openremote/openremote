import { EventEmitter } from "events";
import { project } from "../components/flow-editor";
export class Input extends EventEmitter {
    constructor() {
        super();
        this.selected = [];
        this.selectables = [];
        this.keysCurrentlyHeld = [];
        this.onkeydown = (e) => {
            if (this.keysCurrentlyHeld.includes(e.key)) {
                return;
            }
            this.keysCurrentlyHeld.push(e.key);
        };
        this.onkeyup = (e) => {
            const index = this.keysCurrentlyHeld.indexOf(e.key);
            if (index === -1) {
                return;
            }
            this.keysCurrentlyHeld.splice(index, 1);
        };
        window.addEventListener("keydown", this.onkeydown);
        window.addEventListener("keyup", this.onkeyup);
        window.addEventListener("blur", () => {
            this.clearSelection();
            this.keysCurrentlyHeld = [];
        });
        project.addListener("cleared", () => { this.clearSelection(); });
        this.setMaxListeners(1024);
    }
    select(element, forceMultipleSelection = false) {
        if (!this.multiSelectedEnabled && !forceMultipleSelection) {
            this.clearSelection();
        }
        if (this.selected.includes(element)) {
            return;
        }
        this.emit("selected", element);
        if (element.selected) {
            this.selected.push(element);
        }
    }
    deselect(element) {
        const index = this.selected.indexOf(element);
        if (index === -1) {
            console.warn("Attempt to deselect nonexistent node");
            return;
        }
        this.emit("deselected", element);
        if (!element.selected) {
            this.selected.splice(index, 1);
        }
    }
    handleSelection(element, neverDeselect = false) {
        if (!this.multiSelectedEnabled && this.selected.length > 1) {
            this.select(element);
        }
        else if (this.selected.includes(element) && !neverDeselect) {
            this.deselect(element);
        }
        else {
            this.select(element);
        }
    }
    clearSelection(ignoreMultiselect = false) {
        if (this.multiSelectedEnabled && !ignoreMultiselect) {
            return;
        }
        this.selected.forEach((e) => this.emit("deselected", e));
        this.selected = [];
        this.emit("selectioncleared");
    }
    isHeld(key) {
        return this.keysCurrentlyHeld.includes(key);
    }
    get multiSelectedEnabled() {
        return this.isHeld("Shift") || this.isHeld("Control");
    }
}
//# sourceMappingURL=input.js.map