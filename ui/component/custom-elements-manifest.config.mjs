import { jsxTypesPlugin } from "@wc-toolkit/jsx-types";

export default {
    globs: ['src/**.ts'],
    litelement: true,
    plugins: [
        jsxTypesPlugin({fileName: "custom-elements-jsx.d.ts", componentTypePath: (_x, _y, path) => {
            return path.toString().replace("src", "lib").replace(".ts", ".d.ts");
        }})
    ]
}
