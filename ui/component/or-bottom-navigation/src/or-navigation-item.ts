import {LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";

@customElement("or-navigation-item")
export class OrNavigationItem extends LitElement {

    @property({type: String})
    icon: string | null = null;

    @property({type: String})
    text: string | null = null;
}

