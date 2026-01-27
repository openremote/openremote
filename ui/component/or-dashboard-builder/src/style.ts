/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { css, unsafeCSS } from "lit";
import {DefaultColor1, DefaultColor2, DefaultColor4, DefaultColor5} from "@openremote/core";


// language=CSS
export const style = css`

    :host {       
        --internal-or-asset-tree-header-color: var(--or-asset-tree-header-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));     
        --internal-or-asset-tree-header-text-color: var(--or-asset-tree-header-text-color, var(--or-app-color7, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-tree-header-menu-background-color: var(--or-asset-tree-header-menu-background-color, var(--internal-or-asset-tree-header-text-color));
        --internal-or-asset-tree-header-menu-text-color: var(--or-asset-tree-header-menu-text-color, inherit);
        --internal-or-asset-tree-header-height: var(--or-asset-tree-header-height, 48px);
        --internal-or-asset-tree-background-color: var(--or-asset-tree-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-tree-text-color: var(--or-asset-tree-text-color, inherit);
        --internal-or-asset-tree-item-height: var(--or-asset-tree-item-height, 24px);
        --internal-or-asset-tree-item-padding: var(--or-asset-tree-item-padding, 10px);
        --internal-or-asset-tree-selected-background-color: var(--or-asset-tree-selected-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-tree-selected-color: var(--or-asset-tree-selected-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-asset-tree-button-color: var(--or-asset-tree-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-asset-tree-line-color: var(--or-asset-tree-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
            
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-tree-background-color);
    }

    @media only screen and (max-width: 640px){
        .hideMobile {
            display: none !important;
        }
    }
    @media only screen and (min-width: 641px){
        .showMobile {
            display: none !important;
        }
    }
    
    #container {
        display: flex;
        width: 100%;
        height: 100%;
    }
        
    #menu-header {
        background-color: var(--internal-or-asset-tree-header-color);
        display: flex;
        align-items: center;
        width: 100%;
        height: var(--internal-or-asset-tree-header-height);
        border-bottom: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        z-index: 3;
        line-height: var(--internal-or-asset-tree-header-height);
        color: var(--internal-or-asset-tree-header-text-color);
        --or-icon-fill: var(--internal-or-asset-tree-header-text-color);
    }

    #title-container {
        flex: 1;
        flex-direction: row;
        text-transform: capitalize;
        padding-left: 14px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
    
    #title {
        font-weight: 500;
        font-size: 16px;
    }

    .expandableHeader {
        display: flex;
        align-items: center;
        padding: 12px;
        background: none;
        border-top: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        border-right: none;
        border-bottom: none;
        border-left: none;
        border-radius: 0;
        width: 100%;
        cursor: pointer;
        font-weight: 700;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        flex: 0 0 auto;
    }
    .expandableHeader > or-icon {
        --or-icon-height: 20px;
        --or-icon-width: 20px;
    }
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        /*margin-bottom: 20px;*/
        flex: 0 0 auto;
    }
`
