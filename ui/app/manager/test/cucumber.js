module.exports = {
    default: `
    --require ./test/support/*.js
    --require ./test/step_definitions/*.js
    --format summary 
    --format progress-bar 
    --format @cucumber/pretty-formatter
    --format-options ${JSON.stringify({ snippetInterface: 'async-await' })}
    --publish-quiet  
    `,
    use: {
        trace: 'retain-on-failure',
        workers: 2,
        launchOptions: {
            // force GPU hardware acceleration (even in headless mode)
            // without hardware acceleration, tests will be much slower
            args: ["--use-gl=egl"]
        }
    }
}
