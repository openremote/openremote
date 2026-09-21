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
export interface OrRuleForm {
  checkValidity(): boolean;
}

/**
 * Returns whether every element within {@link root} that can validate itself reports being valid.
 * Elements without a `checkValidity` function are skipped, so this covers the Vaadin fields as well
 * as any nested component implementing {@link OrRuleForm}, wherever they sit in the template.
 */
export function isFormValid(root: ParentNode | null | undefined): boolean {
  if (!root) {
    return false;
  }
  const elements = Array.from(root.querySelectorAll("*")) as Partial<HTMLInputElement>[];
  return elements.every((elem) => typeof elem.checkValidity !== "function" || elem.checkValidity());
}
