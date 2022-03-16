module.exports = {
    default: `
    --require tests/step_definitions/*.js
    --require tests/support/*.js
    --format json:reports/report.json 
    --format message:reports/report.ndjson
    --format html:reports/report.html
    --format summary 
    --format progress-bar 
    --format @cucumber/pretty-formatter
    --format-options ${JSON.stringify({ snippetInterface: 'async-await' })}
    --publish-quiet  
    `
}