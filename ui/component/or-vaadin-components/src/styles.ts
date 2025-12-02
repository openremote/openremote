import {css, unsafeCSS} from "lit";

// @ts-ignore
import vaadinStyles from "@vaadin/vaadin-lumo-styles/lumo.css";

export default [unsafeCSS(vaadinStyles), css`
    span {
        color: purple;
    }
`];
