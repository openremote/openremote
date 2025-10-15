import type {Preview} from "@storybook/web-components";
import { setStorybookHelpersConfig, type Options } from "@wc-toolkit/storybook-helpers";

import './styles.css';


const preview: Preview = {
    parameters: {
        controls: {
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i
            }
        },
        docs: {
            toc: {
                disable: false,
                headingSelector: "h2,h3"
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
