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
        if(config.mode === "production") {
            config.output ??= {};
            config.output.assetPrefix = "/storybook/";
        }
        return config;
    }
};

export default storybookConfig;
