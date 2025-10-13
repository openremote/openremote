import {StorybookConfig} from "storybook-web-components-rsbuild";

// @ts-ignore
const storybookConfig: StorybookConfig = {
    framework: "storybook-web-components-rsbuild",
    staticDirs: ["../images"],
    stories: [
        "../../../component/**/*.stories.@(js|jsx|mjs|ts|tsx)",
        "../docs/**/*.mdx"
    ],
    core: {
        disableTelemetry: true
    },
    rsbuildFinal: config => {
        // Customize the final Rsbuild config here
        return config;
    }
};

export default storybookConfig;
