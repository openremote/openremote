window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  const ui = SwaggerUIBundle({
    url: "/api/master/openapi.json",
    oauth2RedirectUrl: window.location.protocol + "//" + window.location.host + "/swagger/oauth2-redirect.html",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });
  ui.initOAuth({
    clientId: "openremote"
  })
  window.ui = ui;

  //</editor-fold>
};
