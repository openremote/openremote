import { jsxTypesPlugin } from "@wc-toolkit/jsx-types";

export default {
    globs: ['src/**.ts'],
    litelement: true,
    plugins: [
        jsxTypesPlugin({fileName: "custom-elements-jsx.d.ts", componentTypePath: (_name, _tag, path) => {
            return path.toString().replace("src", "lib").replace(".ts", ".d.ts");
        }})
    ]
}
