/*
 * Copyright 2025, OpenRemote Inc.
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
import type {Preview} from "@storybook/web-components";
import {setStorybookHelpersConfig} from "@wc-toolkit/storybook-helpers";
import {themes} from "storybook/theming";
import "./styles.css";

setStorybookHelpersConfig({ hideArgRef: true, categoryOrder: ["attributes", "cssProps", "cssParts", "events"] });

const style = globalThis.getComputedStyle(document.documentElement);
const preview: Preview = {
    parameters: {
        controls: {
            expanded: true,
            disableSaveFromUI: true,
            presetColors: [
                style.getPropertyValue("--or-color-primary"),
                style.getPropertyValue("--or-color-error"),
                style.getPropertyValue("--or-color-warning"),
                style.getPropertyValue("--or-color-success")
            ],
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i
            }
        },
        docs: {
            theme: themes.light,
            story: {
                inline: false
            },
            toc: {
                disable: false,
                headingSelector: "h2,h3"
            },
            source: {
                // TODO: Use a proper code formatter like Prettier
                transform: async source => source.replaceAll(/&quot;/g,'"')
            }
        },
        options: {
            storySort: {
                method: "alphabetical",
                order: ["Introduction"]
            }
        }
    }
};

export default preview;
