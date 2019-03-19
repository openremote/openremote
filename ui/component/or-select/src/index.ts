import {html, LitElement, property, PropertyValues} from 'lit-element';
import {MDCSelect} from '@material/select';

class OrSelect extends LitElement {
    @property({type: String})
    label: string = '';

    protected render() {

        return html`
               <div class="mdc-select">
                  <i class="mdc-select__dropdown-icon"></i>
                  <slot></slot>
                  <label class="mdc-floating-label mdc-floating-label--float-above">${this.label}</label>
                  <div class="mdc-line-ripple"></div>
                </div>
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
    }

    constructor() {
        super();
    }


}

window.customElements.define('or-select', OrSelect);
