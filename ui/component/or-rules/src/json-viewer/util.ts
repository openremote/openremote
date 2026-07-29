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
interface ValidatableField extends HTMLElement {
  checkValidity(): boolean;
  validate?(): boolean;
  reportValidity?(): boolean;
}

function isValidatableField(elem: Element): elem is ValidatableField {
  return elem.tagName !== "FORM" && typeof (elem as Partial<ValidatableField>).checkValidity === "function";
}

/**
 * Whether every field rendered inside `root` holds a valid value.
 *
 * Fields are collected by capability rather than by tag name; matching on a specific tag silently degrades to "no
 * fields found, so nothing to reject" once a form swaps component family, which is how the rule action dialogs
 * ended up either permanently invalid or never validated at all.
 *
 * @param root The form component's shadow root, or any subtree holding the fields.
 * @param report Also mark the offending fields, so the user can see which one blocks them.
 */
export function isFormValid(root: ParentNode | null | undefined, report = false): boolean {
  const fields = root ? Array.from(root.querySelectorAll("*")).filter(isValidatableField) : [];

  // A form that has not rendered yet exposes no fields; treat that as invalid rather than letting it be committed.
  if (fields.length === 0) {
    return false;
  }

  // Deliberately not short-circuiting, so reporting marks every offending field rather than only the first.
  return fields.reduce((valid, field) => {
    const fieldValid = report
      ? (field.validate?.() ?? field.reportValidity?.() ?? field.checkValidity())
      : field.checkValidity();
    return fieldValid && valid;
  }, true);
}
