import resolve from 'rollup-plugin-node-resolve';
import babel from 'rollup-plugin-babel';
import common from 'rollup-plugin-commonjs';
import json from 'rollup-plugin-json';

export default {
    input: ['dist/index.js'],
    output: {
        file: 'dist/bundle.js',
        format: 'es',
        sourcemap: true
    },
    plugins: [
        common(),
        resolve({
            browser: true,
            dedupe: [ ]
        }),
        babel(),
        json()
    ],
    external:  [
        "@openremote/or-icon/dist/mdi-icons"
    ]
};