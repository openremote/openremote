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

export const countBadgeStyle = css`
  or-vaadin-badge {
    --vaadin-badge-background: var(--shades-contrast-10, #3a463a0d);
    border-radius: calc(var(--lumo-border-radius-m) + 2px);
    font-size: 12px;
    color: var(--lumo-secondary-text-color);
    padding: 0 calc(var(--lumo-space-m) - 4px);
  }
`;
