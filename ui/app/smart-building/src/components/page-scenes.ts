import {css, html, LitElement, property, PropertyValues} from "lit-element";
import {connect} from "pwa-helpers/connect-mixin";
import {store} from "../store";
import {PageElement} from "./page-element";
import { localize } from "../localize-mixin";
import { i18next } from "../i18next";

class PageScenes extends connect(store)(localize(i18next)(PageElement)) {

}

window.customElements.define("page-scenes", PageScenes);
