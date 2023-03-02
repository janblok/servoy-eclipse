// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('@chiragrupani/karma-chromium-edge-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        // you can add configuration options for Jasmine here
        // the possible options are listed at https://jasmine.github.io/api/edge/Configuration.html
        // for example, you can disable the random execution with `random: false`
        // or set a specific seed with `seed: 4321`
      },
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },
    jasmineHtmlReporter: {
      suppressAll: true // removes the duplicated traces
    },
    coverageReporter: {
      dir: require('path').join(__dirname, '../../../target/coverage/servoy-public'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' }
      ]
    },
    reporters: ['progress', 'kjhtml','junit', 'coverage'],
    junitReporter: {
        outputFile: '../../../../target/LIB-browser-karma.xml'
    },
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadless','Chrome', 'Edge'],
    singleRun: false,
    restartOnFileChange: true,
    customLaunchers: {
      headlessChrome: {
          base: "ChromeHeadless",
          flags: [
              "--no-sandbox",
              "--js-flags=--max-old-space-size=8196",
              "--disable-dev-shm-usage"
          ],
      },
    },
    captureTimeout: 180000,
    browserDisconnectTolerance: 7,
    browserDisconnectTimeout : 100000,
    browserNoActivityTimeout : 100000
  });
};