import {ConfirmDialog} from "@vaadin/confirm-dialog";
import {OrVaadinComponent} from "./util";
import {css, TemplateResult, type LitElement, render} from "lit";
import {customElement} from "lit/decorators.js";

type WithLit<T> = T & typeof LitElement;

/**
 * Helper function for rendering the `<or-vaadin-confirm-dialog>` dynamically, to "show a dialog on command".
 * This saves initial render time for components, and prevents state handling on the consumer side.
 * `showOkCancelDialog()` appends (and updates) dialogs to the host element when called,
 * and automatically removes the element upon closing the dialog.
 * It also conveniently adds HTML attributes like `cancel-button-visible` when a slotted element is present.
 * @param host - Host element (often shadow root) to append the dialog container as a child.
 * @param dialog - A {@link TemplateResult} to render. It is required to contain a `<or-vaadin-confirm-dialog>` tag.
 */
export function showOkCancelDialog(host: DocumentFragment, dialog: TemplateResult) {
    const container = document.createElement("div") as HTMLDivElement;
    container.id = `dialog-${crypto.randomUUID()}`;
    render(dialog, container); // Render Lit template inside the container

    const dialogElem = container.querySelector("or-vaadin-confirm-dialog") as OrVaadinConfirmDialog | undefined;
    if(dialogElem) {
        dialogElem.opened = true;
        dialogElem.addEventListener("closed", () => container.remove());
        if(dialogElem.querySelector("[slot='cancel-button']")) {
            dialogElem.cancelButtonVisible = true;
        }
        if(dialogElem.querySelector("[slot='reject-button']")) {
            dialogElem.rejectButtonVisible = true;
        }
    }
    host.appendChild(container);
}

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
