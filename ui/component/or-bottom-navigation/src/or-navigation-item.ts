import {customElement, LitElement, property} from "lit-element";

@customElement("or-navigation-item")
export class OrNavigationItem extends LitElement {

    @property({type: String})
    icon: string | null = null;

    @property({type: String})
    text: string | null = null;
}

