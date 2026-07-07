import {ConfirmDialog} from "@vaadin/confirm-dialog";
import {OrVaadinComponent} from "./util";
import {css, type LitElement} from "lit";
import {customElement} from "lit/decorators.js";

type WithLit<T> = T & typeof LitElement;

@customElement("or-vaadin-confirm-dialog")
export class OrVaadinConfirmDialog extends (ConfirmDialog as new () => ConfirmDialog & LitElement) implements OrVaadinComponent {

    static get styles() {
        return [
            (ConfirmDialog as WithLit<typeof ConfirmDialog>).styles,
            css`
                ::part(header),
                ::part(content) {
                    background-color: var(--lumo-contrast-5pct);
                    margin-inline-start: 0;
                }
                ::part(header),
                ::part(footer) {
                    padding: var(--lumo-space-l);
                }
                ::part(content) {
                    padding: 0 var(--lumo-space-l);
                }
            `
        ];
    }

    public open() {
        this.setAttribute("opened", "true");
    }

    public close() {
        this.toggleAttribute("opened", false);
    }
}
