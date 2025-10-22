import {StorybookConfig} from "storybook-web-components-rsbuild";

// @ts-ignore
const storybookConfig: StorybookConfig = {
    framework: "storybook-web-components-rsbuild",
    /*staticDirs: ["../images"],*/
    stories: [
        "../../../component/**/*.stories.@(js|jsx|mjs|ts|tsx)",
        "../docs/**/*.mdx"
    ],
    addons: [
        "@storybook/addon-docs",
        "@storybook/addon-a11y"
    ],
    core: {
        disableTelemetry: true
    },
    rsbuildFinal: (config, { configType }) => {
        config.output ??= {};
        config.output.assetPrefix = "/storybook/";
        return config;
    }
};

export default storybookConfig;
