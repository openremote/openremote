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
export interface BrowserLocale {
  locale: string;
  /** 2026-01-02 as shown in the date format of the locale */
  date: string;
  /** 10:30 as shown in the time format of the locale */
  time: string;
  /** 2026-01-03 as typed in the date format of the locale */
  typedDate: string;
  /** 14:15 as typed in the time format of the locale */
  typedTime: string;
  /** English short name of the first day of the week in the locale */
  firstWeekday: string;
}

export const browserLocales: BrowserLocale[] = [
  {
    locale: "en-US",
    date: "01/02/2026",
    time: "10:30 AM",
    typedDate: "1/3/2026",
    typedTime: "2:15 PM",
    firstWeekday: "Sun",
  },
  {
    locale: "nl-NL",
    date: "02-01-2026",
    time: "10:30",
    typedDate: "3-1-2026",
    typedTime: "14:15",
    firstWeekday: "Mon",
  },
  {
    locale: "de-DE",
    date: "02.01.2026",
    time: "10:30",
    typedDate: "3.1.2026",
    typedTime: "14:15",
    firstWeekday: "Mon",
  },
];
