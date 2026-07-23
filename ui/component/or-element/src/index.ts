/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { css, CSSResult, CSSResultGroup, CSSResultOrNative, LitElement } from "lit";

/**
 * Shared styling for components rendered in shadow DOM (where the global theme CSS can't reach).
 * Applied automatically to components extending {@link OrElement}.
 */
export const globals: CSSResult = css`
    h4 {
        margin: 0;
        padding-bottom: var(--lumo-space-m);
        font-size: var(--lumo-font-size-l);
        font-weight: 600;
        line-height: 125.303%;
    }
`;

/**
 * Base class for OpenRemote Lit components: prepends the shared {@link globals} styling to the
 * component's own `static styles`, so it applies inside the shadow DOM without a per-component include.
 */
export class OrElement extends LitElement {
    protected static finalizeStyles(styles?: CSSResultGroup): CSSResultOrNative[] {
        return [globals, ...super.finalizeStyles(styles)];
    }
}
