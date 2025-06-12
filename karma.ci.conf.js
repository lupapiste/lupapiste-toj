var baseConfig = require('./karma.conf.js');

module.exports = function (config) {
  baseConfig(config)
  config.set({
    plugins: ['karma-cljs-test', 'karma-chrome-launcher', 'karma-junit-reporter'],
    reporters: ['progress', 'junit'],
    junitReporter: {
      outputDir: './',
      outputFile: 'karma-test-results.xml',
      useBrowserName: false,
      suite: ''
    },
    browsers: ['ChromeHeadless_no_sandbox'],
    customLaunchers: {
      ChromeHeadless_no_sandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    }
  });
};
