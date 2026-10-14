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
import type { LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { DateTimePicker } from "@vaadin/date-time-picker";
import { ValueFormatStyleRepresentation, type ValueFormat } from "@openremote/model";
import { getDatePickerI18n, getTimePickerI18n, syncWithLanguage } from "./date-time-i18n";
import type { OrVaadinComponent } from "./util";

@customElement("or-vaadin-date-time-picker")
export class OrVaadinDateTimePicker
  extends (DateTimePicker as new () => DateTimePicker & LitElement)
  implements OrVaadinComponent
{
  protected _stopLanguageSync?: () => void;

  /**
   * Returns the date as a local ISO string in the precision of the picker step, which is minutes without a step,
   * rounded down or, with roundUp, up to that precision, or undefined for an invalid date.
   */
  public static getLocalizedISOString(d?: Date, step?: number, roundUp = false) {
    if (!d || Number.isNaN(d.getTime())) {
      return undefined;
    }
    let format = "YYYY-MM-DDTHH:mm";
    let precision = 60000;
    if (step !== undefined && step % 60 !== 0) {
      format = step % 1 === 0 ? "YYYY-MM-DDTHH:mm:ss" : "YYYY-MM-DDTHH:mm:ss.sss";
      precision = step % 1 === 0 ? 1000 : 1;
    }
    const date = roundUp ? new Date(Math.ceil(d.getTime() / precision) * precision) : d;
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, format.length);
  }

  /**
   * Returns the step for the time precision a value format shows: milliseconds for fractional seconds, seconds for
   * seconds, or undefined for the default of minutes.
   */
  public static getStep(format?: ValueFormat): number | undefined {
    // Text in brackets is literal in moment formats, and LTS is the localized time with seconds
    const momentJsFormat = (format?.momentJsFormat ?? "").replace(/\[[^\]]*\]/gu, "");
    const momentTokens = momentJsFormat.replace(/LTS/gu, "");
    if (format?.fractionalSecondDigits || /[Sx]/u.test(momentTokens)) {
      return 0.001;
    }
    if (
      format?.second ||
      (format?.timeStyle && format.timeStyle !== ValueFormatStyleRepresentation.SHORT) ||
      momentJsFormat.includes("LTS") ||
      /[sX]/u.test(momentTokens)
    ) {
      return 1;
    }
    return undefined;
  }

  connectedCallback() {
    super.connectedCallback();
    this._stopLanguageSync = syncWithLanguage(() => {
      this.i18n = { ...getDatePickerI18n(), ...getTimePickerI18n() };
    });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._stopLanguageSync?.();
  }
}
