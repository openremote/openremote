module.exports = {
    default: `
    --require tests/step_definitions/*.js
    --require tests/support/*.js
    --format summary 
    --format progress-bar 
    --format @cucumber/pretty-formatter
    --format-options ${JSON.stringify({ snippetInterface: 'async-await' })}
    --publish-quiet  
    `
}