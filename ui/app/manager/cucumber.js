module.exports = {
    default: `
    --require ../../../test/frontend_test/tests/step_definitions/*.js
    --require ../../../test/frontend_test/tests/support/*.js
    --format summary 
    --format progress-bar 
    --format @cucumber/pretty-formatter
    --format-options ${JSON.stringify({ snippetInterface: 'async-await' })}
    --publish-quiet  
    `
}