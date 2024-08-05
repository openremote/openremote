import { OrMwcList } from "@openremote/or-mwc-components/or-mwc-list";
import { css } from "lit";
import { customElement } from "lit/decorators.js";

@customElement("asset-type-list")
export class AssettypeList extends OrMwcList {

    static get styles() {
        return [...super.styles, css`
            .mdc-list-item__meta or-icon {
                --or-icon-fill: gray;
            }
        `];
    }
}
