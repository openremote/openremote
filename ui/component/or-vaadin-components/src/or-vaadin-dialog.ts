/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {Dialog} from "@vaadin/dialog";
import {OrVaadinComponent} from "./util";
import {customElement} from "lit/decorators.js";
import {type LitElement, css} from "lit";

/**
 * Vaadin uses custom directives for rendering the dialog content.
 * https://lit.dev/docs/templates/custom-directives/
 * https://vaadin.com/docs/latest/components/dialog/
 */
export {
    dialogHeaderRenderer,
    DialogHeaderRendererDirective,
    dialogRenderer,
    DialogRendererDirective,
    dialogFooterRenderer,
    DialogFooterRendererDirective,
} from "@vaadin/dialog/lit";

type WithLit<T> = T & typeof LitElement;

/*export function getOkCancelDialog(id: string, title?: string | TemplateResult, content: string | TemplateResult = "areYouSure", footer: TemplateResult | string[] = ["cancel", "ok"], onClick?: (index: number, ev: Event) => void): TemplateResult {
    return html`
        <or-vaadin-dialog id=${id}
                ${content ? dialogHeaderRenderer(() => typeof title === 'string'
                        ? html`<or-translate value=${title}></or-translate>`
                        : title as TemplateResult
                ) : undefined}
                ${dialogRenderer(() => typeof content === 'string'
                        ? html`<or-translate value=${content}></or-translate>`
                        : content as TemplateResult
                )}
                ${dialogFooterRenderer(() => !Array.isArray(footer) ? footer : html`
                    ${when(footer.length > 1, () => html`
                        <or-vaadin-button @click=${(ev: Event) => onClick?.(0, ev)}>
                            <or-translate value=${footer[0]}></or-translate>
                        </or-vaadin-button>
                    `)}
                    ${when(footer.length > 2, () => footer
                            .filter((_, i) => i !== 0 && i !== (footer.length - 1))
                            .map((btn, i) => html`
                                <or-vaadin-button @click=${(ev: Event) => onClick?.(i, ev)}>
                                    <or-translate value=${btn}></or-translate>
                                </or-vaadin-button>
                            `)
                    )}
                    <or-vaadin-button theme="primary" @click=${(ev: Event) => onClick?.(footer.length - 1, ev)}>
                        <or-translate value=${footer[footer.length - 1]}></or-translate>
                    </or-vaadin-button>
                `)}
        ></or-vaadin-dialog>
    `
}*/

@customElement("or-vaadin-dialog")
export class OrVaadinDialog extends (Dialog as new () => Dialog & LitElement) implements OrVaadinComponent {

    static get styles() {
        return [
            (Dialog as WithLit<typeof Dialog>).styles,
            css`
                ::part(header),
                ::part(content) {
                    background-color: var(--lumo-contrast-5pct);
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
