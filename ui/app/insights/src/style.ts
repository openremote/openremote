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
import { DefaultColor3 } from "@openremote/core";
import { css, unsafeCSS } from "lit";

// language=CSS
export const style = css`

    @media only screen and (max-width: 768px){
        .hideMobile {
            display: none !important;
        }
        .fullWidthOnMobile {
            flex: 1 !important;
        }
    }
    @media only screen and (min-width: 769px){
        .showMobile {
            display: none !important;
        }
    }
    
    /* Header related styling */
    #fullscreen-header-wrapper {
        min-height: 36px;
        padding: 6px;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    #fullscreen-header-title {
        font-size: 18px;
        font-weight: bold;
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
    }
    #fullscreen-header-title > or-mwc-input {
        margin-right: 4px;
        --or-icon-fill: ${unsafeCSS(DefaultColor3)};
    }
    #fullscreen-header-actions {
        flex: 1 1 auto;
        text-align: right;
    }
    #fullscreen-header-actions-content {
        display: flex;
        flex-direction: row;
        align-items: center;
        float: right;
    }
    #dashboard-error-text {
        display: flex;
        height: 100%;
        justify-content: center;
        align-items: center;
    }
    
    
    /* Drawer related styling */
    #drawer-custom-scrim {
        position: absolute;
        width: 100%;
        height: 100%;
        z-index: 1;
        animation: bgColorFade 0.3s ease-in-out 0s forwards;
    }
    @keyframes bgColorFade {
        0% {
            background-color: rgba(0, 0, 0, 0);
        }
        100% {
            background-color: rgba(0, 0, 0, 0.32);
        }
    }
    .drawer-scrim-fadeout {
        animation: bgColorFadeOut 0.2s ease-in-out 0s forwards !important;
    }
    @keyframes bgColorFadeOut {
        0% {
            background-color: rgba(0, 0, 0, 0.32);
        }
        100% {
            background-color: rgba(0, 0, 0, 0);
        }
    }
`
