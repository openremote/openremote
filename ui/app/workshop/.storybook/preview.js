import {setCustomElementsManifest} from '@storybook/web-components';
import customElements from '@openremote/docs/custom-elements.json';
import { setWcStorybookHelpersConfig } from "wc-storybook-helpers";
import { withActions } from '@storybook/addon-actions/decorator';
import DefaultPage from "../docs/default.mdx";

// SOURCES;
// https://github.com/storybookjs/storybook/blob/next/code/addons/docs/web-components/README.md
// https://github.com/open-wc/custom-elements-manifest/tree/master/packages/analyzer
// https://github.com/webcomponents/custom-elements-manifest

setWcStorybookHelpersConfig({
    hideArgRef: true,
    /*typeRef: "expandedType",*/
    renderDefaultValues: true
});

setCustomElementsManifest(customElements);

/** @type { import('@storybook/web-components').Preview } */
const preview = {
    parameters: {
        actions: { argTypesRegex: '^on[A-Z].*' },
        controls: {
            expanded: true,
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i,
            },
            sort: "alpha"
        },
        docs: {
            page: DefaultPage,
            toc: {
                disable: false,
                contentsSelector: '.sbdocs-content',
                headingSelector: 'h1,h2',
                unsafeTocbotOptions: {
                    orderedList: true,
                },
            },
            story: {
                inline: false
            }
        },
        options: {
            storySort: (a, b) => a.title === b.title ? 0 : a.title.localeCompare(b.id, undefined, { numeric: true })
        }
        /*layout: "centered"*/
    },
    decorators: [withActions],
    tags: ['autodocs']
};

export default preview;
