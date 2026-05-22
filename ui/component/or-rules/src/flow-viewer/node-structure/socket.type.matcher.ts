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
import {NodeDataType} from "@openremote/model";

export class SocketTypeMatcher {

    public static match(a: NodeDataType, b: NodeDataType) {
        return a === NodeDataType.ANY ||
            b === NodeDataType.ANY ||
            SocketTypeMatcher.matches.find((t) => t.type === a)!.matches.includes(b);
    }

    private static readonly matches: { type: NodeDataType, matches: NodeDataType[] }[] = [
        {
            type: NodeDataType.NUMBER,
            matches: [
                NodeDataType.NUMBER,
                NodeDataType.STRING,
                NodeDataType.NUMBER_ARRAY
            ]
        },
        {
            type: NodeDataType.STRING,
            matches: [
                NodeDataType.STRING,
            ]
        },
        {
            type: NodeDataType.TRIGGER,
            matches: [
                NodeDataType.TRIGGER,
            ]
        },
        {
            type: NodeDataType.BOOLEAN,
            matches: [
                NodeDataType.BOOLEAN,
                NodeDataType.STRING,
            ]
        },
        {
            type: NodeDataType.COLOR,
            matches: [
                NodeDataType.COLOR,
                NodeDataType.STRING,
            ]
        },
        {
            type: NodeDataType.NUMBER_ARRAY,
            matches: [
                NodeDataType.NUMBER
            ]
        }
    ];
}
