/*
 * Copyright 2025, OpenRemote Inc.
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
import {DefaultColor5} from "@openremote/core";
import {css, unsafeCSS} from "lit";

// language=CSS
export const invalidStyle = css`
    *:invalid {
        border-bottom: 2px solid var(--internal-or-rules-invalid-color);
    }
`;

// language=CSS
export const buttonStyle = css`
    .button-clear {
        background: none;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        visibility: hidden;
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .button-clear:hover {
        --or-icon-fill: var(--internal-or-rules-button-color);
    }

    .button-clear:focus {
        outline: 0;
    }

    .button-clear.hidden {
        visibility: hidden;
    }

    .plus-button {
        --or-icon-fill: var(--internal-or-rules-button-color);
    }
    
    .add-button {
        display: inline-block;
        font-weight: bold;
        font-size: 15px;
        line-height: 24px;
    }

    .add-buttons-container {
        display: flex;
        flex-direction: column;
        margin-top: 10px;
        margin-bottom: -10px;
        padding-top: 5px;
        border-top-width: 1px;
        border-top-style: solid;
        border-color: var(--internal-or-rules-line-color);
    }

    .add-buttons-container.hidden {
        border: none;
        margin: 0;
        padding: 0;
    }
    
    .add-buttons-container > button {
        display: inline-flex;
    }

    .add-buttons-container > button > or-icon {
        margin-right: 5px;
    }
`;
