const config = {
    use: {
        trace: 'retain-on-failure',
        workers:1,
        launchOptions: {
            // force GPU hardware acceleration (even in headless mode)
            // without hardware acceleration, tests will be much slower
            args: ["--use-gl=desktop"]
          }
    },
};

module.exports = config;