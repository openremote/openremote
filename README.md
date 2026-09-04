# OpenRemote

[![CI/CD](https://github.com/openremote/openremote/actions/workflows/ci_cd.yml/badge.svg?branch=master&event=push)](https://github.com/openremote/openremote/actions/workflows/ci_cd.yml?query=event%3Apush+branch%3Amaster)
[![Open Source? Yes!](https://badgen.net/badge/Open%20Source%20%3F/Yes%21/blue?icon=github)](https://github.com/Naereen/badges/)
<!-- ![tests](https://github.com/openremote/openremote/workflows/tests/badge.svg) -->

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://docs.openremote.io) **·** [Forum](https://forum.openremote.io) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

Welcome to OpenRemote; an intuitive user-friendly 100% open source IoT platform. You can build a complete IoT device management solution including: device management and auto provisioning, customisation of asset types, automation via when-then, flow, javascript and groovy rules, data analytics, connectivity via several protocol agents and manager APIs (e.g. MQTT broker, HTTP/REST, WS), Multi-tenancy (realms), Users and roles management, Edge gateway, Front-end UI web components and consoles, and an Insights dashboard builder.

As the code base is 100% open source, applications are limitless. Here's an architecture overview:

<img src="https://openremote.io/wp-content/uploads/2023/09/OpenRemote_Architecture-scaled.jpg" width="900">

## Quickstart

You can quickly try the online demo with restricted access, login credentials are `smartcity:smartcity`:

[Online demo](https://demo.openremote.app/manager/?realm=smartcity)

The quickest way to get your own environment with full access is to make use of our docker images (both `amd64` and `arm64` are supported).

1. Make sure you have [Docker Desktop](https://www.docker.com/products/docker-desktop) installed (v18+).
2. Download the docker compose file:
   [OpenRemote Stack](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose.yml) (Right click 'Save link as...')
3. In a terminal `cd` to where you just saved the compose file and then run:

```
docker compose pull
docker compose -p openremote up
```

If all goes well then you should now be able to access the OpenRemote Manager UI at [https://127.0.0.1](https://127.0.01). You will need to accept the self-signed
certificate, see [here](https://www.technipages.com/google-chrome-bypass-your-connection-is-not-private-message) for details how to do this in Chrome (similar for other browsers).

### Login credentials

Username: admin  
Password: secret

### Changing host and/or port

The URL you use to access the system is important, the default is configured as `https://127.0.0.1` if you are using a VM then you will need to set the `OR_HOSTNAME` environment variable, so if for example you will be accessing using `https://192.168.1.1` then use the following startup command:

BASH:

```shell
OR_HOSTNAME=192.168.1.1 docker-compose -p openremote up -d
```

or

CMD:

```shell
cmd /C "set OR_HOSTNAME=192.168.1.1 && docker-compose -p openremote up -d"
```

## What next

Try creating assets, agents, rules, users, realms, etc. using the Manager UI, please refer to the [documentation](https://docs.openremote.io) for more information, some things to try:

- [Manager UI Guide](https://docs.openremote.io/docs/user-guide/manager-ui/) - Learn more about the User Interface
- [Creating an HTTP Agent tutorial](https://docs.openremote.io/docs/tutorials/open-weather-api-using-http-agent) - Connect to an online weather service
- [Custom Deployment](https://docs.openremote.io/docs/user-guide/deploying/custom-deployment) - Style the Manager to your brand
- [Setting up an IDE](https://docs.openremote.io/docs/developer-guide/setting-up-an-ide) - Set up your development environment
- [Working on the UI](https://docs.openremote.io/docs/developer-guide/working-on-ui-and-apps) - Create a web application compatible with OpenRemote
- [Creating a custom project](https://docs.openremote.io/docs/developer-guide/creating-a-custom-project) - Create a project with custom protocols, asset types and setup code

## Where's the data stored?

Persistent data is stored in a PostgreSQL DB which is stored in the `openremote_postgresql-data` docker volume which is durably stored independently of the running containers (see all with `docker volume ls`).
Note that historical attribute data is purged daily based on value of `OR_DATA_POINTS_MAX_AGE_DAYS`; this value can also be overridden for individual attributes by using the `dataPointsMaxAgeDays` configuration item.
See the [Developer Guide](https://docs.openremote.io/docs/developer-guide/useful-commands-and-queries/#backuprestore-openremote-db) for details on making backups of the database.

## OpenTelemetry tracing

The manager distribution and container image include OpenTelemetry Java agent. The image entrypoint adds
`-javaagent:/opt/opentelemetry/opentelemetry-javaagent.jar` only when the standard
`OTEL_JAVAAGENT_ENABLED` environment variable is `true`; tracing is therefore opt-in and the agent is not loaded for
existing deployments.

No application source instrumentation is used. The agent and its automatic instrumentations are configured with
standard OpenTelemetry environment variables.

### Docker Compose

Set these values in the deployment environment or `.env` file used by `docker-compose.yml` or
`profile/deploy.yml`:

```dotenv
OTEL_JAVAAGENT_ENABLED=true
OTEL_SERVICE_NAME=openremote-manager
OTEL_TRACES_EXPORTER=otlp
OTEL_EXPORTER_OTLP_ENDPOINT=http://alloy-otel:4318
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED=true
```

The `alloy-otel` service must be reachable from the manager container's Compose network. The Compose profile sets
`OTEL_METRICS_EXPORTER=none` and `OTEL_LOGS_EXPORTER=none` by default so this integration exports traces only and
does not duplicate the existing Prometheus metrics or export application logs. These remain standard OpenTelemetry
settings and can be overridden in the deployment environment.

Do not configure Tempo credentials on the manager. The manager sends OTLP to its local Alloy instance, and Alloy
handles authenticated forwarding to Tempo.

### Kubernetes

Add the following to the environment-specific manager Helm values file, replacing the example service DNS name and
namespace with the Alloy service used by the cluster:

```yaml
or:
  otel:
    enabled: true
    serviceName: openremote-manager
    endpoint: http://alloy-otel.observability.svc.cluster.local:4318
    protocol: http/protobuf
```

This uses Alloy's OTLP/HTTP receiver on port `4318`. The service name, namespace, sampling, resource attributes, and
all other customer- or environment-specific settings belong in the deployment values, not in the common manager
image or chart defaults. Additional OpenTelemetry Java agent settings, such as sampling, can be supplied through
`or.env`.

### Validated automatic visibility

An integration run against PostgreSQL and Keycloak, exporting over OTLP/HTTP to Alloy, produced the following useful
automatic telemetry:

- Undertow HTTP server spans enriched with RESTEasy/JAX-RS routes, including manager API and authentication proxy
  requests.
- Apache HTTP client and `HttpURLConnection` client spans for manager-to-Keycloak calls.
- JDBC and Hibernate spans for PostgreSQL operations, including transaction spans and sanitized SQL statements.
- Trace-context propagation across supported executor and concurrency APIs. No executor task spans are expected;
  executor instrumentation connects asynchronous work to its parent trace.

Query the appropriate Tempo data source in Grafana for `service.name = openremote-manager` to verify that the same
span types arrive through the environment's Alloy and Tempo pipeline.

The MQTT to Artemis to attribute-processing path is outside this first version; there is no manual span creation or MQTT trace propagation.

### Data safety

The default agent instrumentation does not capture HTTP request or response bodies, and HTTP header and servlet
request-parameter capture are opt-in. Do not enable those capture settings without a separate data review. Database
statement sanitization is enabled by default and must remain enabled. The traces still contain operational metadata
such as route and URL information, database operation and table names, remote addresses, exception details, and
messaging destinations. Do not put credentials or sensitive values in URLs, destination names, exception messages,
resource attributes, or custom OpenTelemetry configuration.

Validate the actual trace data with representative non-production requests before enabling tracing in production.
Specifically inspect span names, resource attributes, span attributes, and events for authorization headers, cookies,
tokens, passwords, request bodies, OpenRemote attribute values, personal data, and un-sanitized SQL parameters.

During the integration run, synthetic username, password, asset-name, and attribute-value markers and the exact bearer
token used for an API request were absent from Alloy's detailed trace output. No authorization, cookie, request-body,
or database-parameter attributes were emitted. SQL statement text was present, but bound values were represented by
`?` placeholders.

### Runtime validation

Use the following checks in an environment with PostgreSQL, Keycloak, Alloy, Tempo, and Grafana available:

1. Start the manager without `OTEL_JAVAAGENT_ENABLED`, confirm the Java agent banner is absent, and exercise normal
   login, API, and database-backed operations.
2. Enable the environment variables above, repeat those operations, and confirm the manager remains healthy.
3. In Grafana Explore, select the environment's Tempo data source and query for
   `{ resource.service.name = "openremote-manager" }`. Record which of the expected span types are present.
4. Inspect representative spans using the data-safety checklist above.
5. Temporarily set `OTEL_EXPORTER_OTLP_ENDPOINT` to an unused address, restart the manager, and confirm startup,
   health checks, API calls, and database operations still succeed. Export failures should be reported by the agent
   without terminating or blocking the manager.

To disable tracing, unset the tracing variables or set `OTEL_JAVAAGENT_ENABLED=false`, then restart the manager.

## Contributing to OpenRemote

For information and how to set up a development environment, see the [Developer Guide](https://docs.openremote.io/docs/category/developer-guide).

We work with Java, Groovy, TypeScript, Gradle, Docker, and a wide range of APIs and protocol implementations.

We follow the [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow) workflow with tags and releases for published versions of our components; when working on the codebase create descriptive branch names (e.g. `feature/cool_feature_x`, `hotfix/flux_capacitor`, `issue/123`, etc.).

When your changes are complete then create a Pull Request ensuring that your branch is up-to-date with the source branch and that code changes are covered by tests and that the full test suite passes.

## Discuss OpenRemote

Join us on the [community forum](https://forum.openremote.io/).
