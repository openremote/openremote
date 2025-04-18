const { exec } = require('child_process');

class OrCustomElementsManifestPlugin {
  apply(compiler) {
    compiler.hooks.run.tapAsync('OrCustomElementsManifestPlugin', (compilation, done) => {
      console.debug("Building custom elements manifest..")
      exec('cem analyze --litelement --globs "./src/**.ts"', (err, stdout, stderr) => {
        if (err) {
          console.error('Error generating custom elements manifest:', stderr);
          return done(err);
        }
        console.debug('Custom elements manifest generated.');
        done();
      });
    });
  }
}

module.exports = OrCustomElementsManifestPlugin;
