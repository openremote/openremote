module.exports = {
    default: `
    --require ./test/support/*.js
    --require ./test/step_definitions/*.js
    --format summary 
    --format progress-bar 
    --format @cucumber/pretty-formatter
    --format-options ${JSON.stringify({ snippetInterface: 'async-await' })}
    --publish-quiet  
    `
}
