import type {Preview} from "@storybook/web-components";
import {setStorybookHelpersConfig} from "@wc-toolkit/storybook-helpers";
import rest from "@openremote/rest";
import './styles.css';

setStorybookHelpersConfig({ hideArgRef: true });

const preview: Preview = {
    parameters: {
        controls: {
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i
            }
        },
        docs: {
            story: {
                inline: false
            },
            toc: {
                disable: false,
                headingSelector: "h2,h3"
            },
            source: {
                // TODO: Use a proper code formatter like Prettier
                transform: async (source) => source.replaceAll(/&quot;/g,'"')
            }
        },
        options: {
            storySort: {
                method: "alphabetical",
                order: ["Introduction", "How to Install"]
            }
        }
    }
};

export default preview;
