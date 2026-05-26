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
import { EventEmitter } from "events";

// Inspired by or-rules flow editor Input class
export class DashboardKeyEmitter extends EventEmitter {

    constructor() {
        super();
        window.addEventListener("keydown", this.onkeydown);
    }

    /* ------------------------------------- */

    private onkeydown = (e: KeyboardEvent) => {
        if(e.key == 'Delete') {
            e.preventDefault();
            this.emit('delete', e);
        } else if(e.key == 'Escape') {
            e.preventDefault();
            this.emit('deselect', e);
        } else if(e.key == 's' && e.ctrlKey) {
            e.preventDefault();
            this.emit('save', e);
        }
    }
}
