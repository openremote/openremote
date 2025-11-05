import {customElement} from "lit/decorators.js";
import {unsafeCSS } from "lit";
import {ComboBox} from "@vaadin/combo-box";

const lumoCss = require("@vaadin/vaadin-lumo-styles/lumo.css");

@customElement("or-vaadin-combobox")
export class OrVaadinCombobox extends ComboBox<any> {

    static get styles() {
        console.log(lumoCss);
        return [unsafeCSS(lumoCss)]; // TODO: Storybook works fine (RSBuild), but importing lumo.css in the Manager (RSPack) doesn't work. (due to @import?)
    }
}
