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
import type {
  AbstractNotificationMessageUnion,
  EmailNotificationMessage,
  ManagerAppRealmNotificationConfig,
  PushNotificationMessage,
} from "@openremote/model";

/** A message that carries its own content, as opposed to a localized one holding one of these per language. */
export type NotificationMessage = PushNotificationMessage | EmailNotificationMessage;

/** The messages of a localized notification, keyed by language. */
export type NotificationMessages = { [language: string]: NotificationMessage };

/** Notification config per realm, as the manager app config holds it. */
export type NotificationConfigs = { [realm: string]: ManagerAppRealmNotificationConfig };

/** The notification config of `realm`, falling back to the `default` entry that stands in for every realm. */
export function getRealmNotificationConfig(
  configs?: NotificationConfigs,
  realm?: string
): ManagerAppRealmNotificationConfig | undefined {
  return (realm ? configs?.[realm] : undefined) || configs?.default;
}

/**
 * Every language a localized message can be shown for: those the realm supports and those the message holds,
 * which can differ as the supported languages change independently of the messages already sent.
 */
export function getNotificationLanguages(
  config?: ManagerAppRealmNotificationConfig,
  message?: AbstractNotificationMessageUnion
): string[] {
  const held = message?.type === "localized" ? Object.keys(message.languages || {}) : [];
  return [...new Set([...(config?.languages || []), ...held])];
}

/** Whether a message has anything to show; a language that is supported but not translated has not. */
function isTranslated(message?: NotificationMessage): boolean {
  if (!message) {
    return false;
  }
  return message.type === "email"
    ? !!(message.subject || message.html || message.text)
    : !!(message.title || message.body);
}

/**
 * Resolves what to show for a stored message, and for a localized one the language shown and every language it
 * holds. A localized message keeps its content per language under `languages`, so show `language` (the viewer's)
 * when that language is translated, and otherwise the message's `defaultLanguage`, which is what a target
 * without a locale of its own receives.
 */
export function resolveNotificationMessage(
  message?: AbstractNotificationMessageUnion,
  language?: string
): { message?: NotificationMessage; language?: string; languages?: NotificationMessages } {
  if (message?.type !== "localized") {
    return { message };
  }
  const languages = (message.languages || {}) as NotificationMessages;
  const codes = Object.keys(languages);
  // Fall through to any translated language, then to an untranslated one, so a language is always named
  const resolved =
    [language, message.defaultLanguage, ...codes].find((code) => code && isTranslated(languages[code])) ||
    (message.defaultLanguage && languages[message.defaultLanguage] ? message.defaultLanguage : codes[0]);

  return { message: resolved ? languages[resolved] : undefined, language: resolved, languages };
}
