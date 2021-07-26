import {LitElement, html, css} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "./or-navigation-item";
import { FlattenedNodesObserver } from "@polymer/polymer/lib/utils/flattened-nodes-observer";
import {OrNavigationItem} from "./or-navigation-item";

@customElement("or-bottom-navigation")
export class OrBottomNavigation extends LitElement {

    static _iconsCss: HTMLLinkElement | null = null;
    static iconsUrl = "https://fonts.googleapis.com/icon?family=Material+Icons";

    protected _observer?: FlattenedNodesObserver;

    protected _temp: number[] = [1,2,3,4];

    @property({type: Array})
    protected _virtualItems: OrNavigationItem[] = [];

    @query("slot")
    private _itemsSlot!: HTMLSlotElement;

    constructor() {
        super();

        if (OrBottomNavigation._iconsCss === null) {
            let elem = document.createElement("link");
            elem.href = OrBottomNavigation.iconsUrl;
            elem.rel = "stylesheet";
            OrBottomNavigation._iconsCss = elem;
            window.document.head.appendChild(OrBottomNavigation._iconsCss);
        }
    }

    static styles = css`
        .mdc-bottom-navigation {
          position: fixed;
          bottom: 0px;
          height: 56px;
          background-color: var(--mdc-theme-background, #fff);
          width: 100%;
          box-shadow: 0px 5px 5px -3px rgba(0, 0, 0, 0.2), 0px 8px 10px 1px rgba(0, 0, 0, 0.14), 0px 3px 14px 2px rgba(0, 0, 0, 0.12);
          overflow: hidden;
          z-index: 8;
        }
        .mdc-bottom-navigation__list {
          display: flex;
          justify-content: center;
          height: 100%;
        }
        .mdc-bottom-navigation__list-item {
          flex: 1 1 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: flex-start;
          padding: 0 12px;
          min-width: 60px;
          max-width: 168px;
          box-sizing: border-box;
          color: var(--mdc-theme-text-secondary-on-background, rgba(0, 0, 0, 0.54));
          -webkit-user-select: none;
             -moz-user-select: none;
              -ms-user-select: none;
                  user-select: none;
        }
        .mdc-bottom-navigation__list-item__icon {
          padding-top: 8px;
          pointer-events: none;
          transition-property: padding-top, color;
          transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
          transition-duration: 100ms;
        }
        .mdc-bottom-navigation__list-item__icon path {
          fill: var(--mdc-theme-text-secondary-on-background, rgba(0, 0, 0, 0.54));
        }
        .mdc-bottom-navigation__list-item__text {
          margin-top: auto;
          padding-bottom: 10px;
          font-size: 0.75rem;
          pointer-events: none;
          transition-property: font-size, color;
          transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
          transition-duration: 100ms;
        }
        .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__icon {
          padding-top: 6px;
        }
        .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__text {
          font-size: 0.875rem;
        }
        .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__icon, .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__text {
          color: var(--mdc-theme-primary, #6200ee);
        }
        .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__icon path, .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__text path {
          fill: var(--mdc-theme-primary, #6200ee);
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item {
          min-width: 56px;
          max-width: 96px;
          transition-property: min-width, max-width;
          transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
          transition-duration: 100ms;
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item .mdc-bottom-navigation__list-item__icon {
          padding-top: 16px;
          transition-property: padding-top;
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item .mdc-bottom-navigation__list-item__text {
          position: absolute;
          line-height: 10px;
          bottom: 0;
          opacity: 0;
          transition-property: opacity, font-size;
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item--activated {
          min-width: 96px;
          max-width: 168px;
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__icon {
          padding-top: 8px;
          transition-property: padding-top;
        }
        .mdc-bottom-navigation--shifting .mdc-bottom-navigation__list-item--activated .mdc-bottom-navigation__list-item__text {
          white-space: nowrap;
          opacity: 1;
        }
    `;

    connectedCallback(): void {
        super.connectedCallback();
        // var lists = document.querySelectorAll('.mdc-bottom-navigation__list');
        // var activatedClass = 'mdc-bottom-navigation__list-item--activated';
        // for (var i = 0, list; list = lists[i]; i++) {
        //     list.addEventListener('click', function(event) {
        //         var el = event.target;
        //         while (!el.classList.contains('mdc-bottom-navigation__list-item') && el) {
        //             el = el.parentNode;
        //         }
        //         if (el) {
        //             var selectRegex = /.*(demo-card-\d).*/;
        //             var activatedItem = document.querySelector('.' + event.target.parentElement.parentElement.parentElement.className.replace(selectRegex, '$1') + ' .' + activatedClass);
        //             if (activatedItem) {
        //                 activatedItem.classList.remove(activatedClass);
        //             }
        //             event.target.classList.add(activatedClass);
        //         }
        //     });
        // }
    }

    firstUpdated() {
        this._observer = new FlattenedNodesObserver(this._itemsSlot!, (info: any) => {
            this._onAdd(info.addedNodes);
            this._onRemove(info.removedNodes);
        });
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
    }

    protected render() {
        return html`
        ${OrBottomNavigation.iconsUrl ? html `<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">` : null}
        <div class="mdc-bottom-navigation mdc-bottom-navigation--shifting">
          <nav class="mdc-bottom-navigation__list">
                <slot></slot>
                <div>
                    ${this._virtualItems.map(navItem => OrBottomNavigation._virtualTemplate(navItem))}
                </div>
          </nav>
        </div>        
        `;
    }

    private _onAdd(nodes: Element[]) {
        nodes.forEach((node) => {
           if (node instanceof OrNavigationItem) {
               this._virtualItems.push(node);
               this.requestUpdate();
           }
        });
    }

    private _onRemove(nodes: Element[]) {
        nodes.forEach((node) => {
            if (node instanceof OrNavigationItem) {
                let index = this._virtualItems.indexOf(node);
                if (index >= 0) {
                    this._virtualItems.slice(index, 1);
                    this.requestUpdate();
                }
            }
        });
    }

    protected static _virtualTemplate(navItem: OrNavigationItem) {
        return html`
            <span class="mdc-bottom-navigation__list-item">
                ${navItem.icon ? html`<span class="material-icons mdc-bottom-navigation__list-item__icon">${navItem.icon}</span>` : null}
                ${navItem.text ? html`<span class="material-icons mdc-bottom-navigation__list-item__text">${navItem.text}</span>` : null}              
            </span>
        `;
    }
}
