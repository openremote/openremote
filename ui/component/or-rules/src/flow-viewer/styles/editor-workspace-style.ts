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
import { css } from "lit";

export const EditorWorkspaceStyle = css`
:host{
    background: whitesmoke;
    position: relative;
    display: block;
    overflow: hidden;
    box-shadow: rgba(0, 0, 0, 0.2) 0 0 4px inset;
    height: 100%;
}

.view-options{
    position: absolute;
    left: 0;
    top: 0;
    display: flex;
    flex-direction: row;
}

.button{
    padding: 10px;
    margin: 10px;
    cursor:pointer;
    background: rgba(0,0,0,0.02);
}

or-mwc-input[type=button]
{
    margin: 10px;
    color: inherit;
}

.button:hover{
    background: rgba(0,0,0,0.04);
}

.button:active{
    background: rgba(0,0,0,0.06);
}

svg, connection-container {
    pointer-events: none;
    position: absolute;
    display: block;
    right: 0;
    top: 0;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    stroke-width: 4px;
    stroke: rgb(80,80,80);
}
`;
