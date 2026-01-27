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

export const PickerStyle = css`
input{
    border: 0;
    height: max-content;
}
textarea {
    min-width: 150px;
    min-height: 37px;
} 
input[type=number] {
    padding: 11px 10px;
}
textarea, input[type=text], select{
    font-family: inherit;
    padding: 10px;
    border-radius: var(--roundness);    
    width: fit-content;
    border: none;
}
.attribute-label{
    padding: 5px;
    background: rgba(0,0,0,0.05);
    text-align: center;
    border-radius: var(--roundness);
    font-size: 110%;
    color: rgb(76, 76, 76);
    font-weight: 400;
}
`;
