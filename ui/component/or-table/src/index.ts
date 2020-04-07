import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult, unsafeCSS, query} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {MDCComponent} from "@material/base";
import {MDCDataTable } from "@material/data-table";

import {DefaultColor1, DefaultColor4, DefaultColor8} from "@openremote/core";

// language=CSS
const style = css`
    
    :host([hidden]) {
        display: none;
    }
    
    #wrapper {
        display: flex;
        align-items: center;
    }
    
    #component {
        max-width: 100%;
    }
`;

@customElement("or-table")
export class OrTable extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    // @property({type: Boolean})
    // public focused?: boolean;
    //
    // @property()
    // public value?: any;
    //
    // @property({type: String})
    // public type?: InputType;
    //
    // @property({type: Boolean})
    // public readonly: boolean = false;
    //
    // @property({type: Boolean})
    // public disabled: boolean = false;
    //
    // @property({type: Boolean})
    // public required: boolean = false;
    //
    // @property()
    // public max?: any;
    //
    // @property()
    // public min?: HTMLInputElement;
    //
    // @property({type: Number})
    // public step?: number;
    //
    // @property({type: Boolean})
    // public checked: boolean = false;
    //
    // @property({type: Number})
    // public maxLength?: number;
    //
    // @property({type: Number})
    // public minLength?: number;
    //
    // @property({type: Number})
    // public rows?: number;
    //
    // @property({type: Number})
    // public cols?: number;
    //
    // @property({type: Boolean})
    // public multiple: boolean = false;
    //
    // @property({type: String})
    // public pattern?: string;
    //
    // @property({type: String})
    // public placeHolder?: string;
    //
    // @property({type: Array})
    // public options?: string[] | [string, string][];
    //
    // @property({type: Boolean})
    // public autoSelect?: boolean;
    //
    // /* STYLING PROPERTIES BELOW */
    //
    // @property({type: String})
    // public icon?: string;
    //
    // @property({type: String})
    // public iconOn?: string;
    //
    // @property({type: String})
    // public iconTrailing?: string;
    //
    // @property({type: Boolean})
    // public dense: boolean = false;
    //
    // /* BUTTON STYLES START */
    //
    // @property({type: Boolean})
    // public raised: boolean = false;
    //
    // @property({type: Boolean})
    // public action: boolean = false;
    //
    // @property({type: Boolean})
    // public unElevated: boolean = false;
    //
    // @property({type: Boolean})
    // public outlined: boolean = false;
    //
    // @property({type: Boolean})
    // public rounded: boolean = false;
    //
    // /* BUTTON STYLES END */
    //
    // /* TEXT INPUT STYLES START */
    //
    // @property({type: Boolean})
    // public fullWidth: boolean = false;
    //
    // @property({type: String})
    // public helperText?: string;
    //
    // @property({type: Boolean})
    // public helperPersistent: boolean = false;
    //
    // @property({type: String})
    // public validationMessage = "";
    //
    // @property({type: Boolean})
    // public charCounter: boolean = false;
    //
    // @property({type: String})
    // public label?: string;

    /* TEXT INPUT STYLES END */

    protected _mdcComponent?: MDCComponent<any>;
    protected _mdcField?: MDCComponent<any>;
    protected _selectedIndex = -1;

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
        if (this._mdcField) {
            this._mdcField.destroy();
            this._mdcField = undefined;
        }
    }

    protected render() {

        return html`<span>tabel</span>`;
    }

}
