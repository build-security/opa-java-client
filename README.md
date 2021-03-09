# opa-java-client

This package implements a Plain Old Java Objects (POJOs) client for making policy evaluation requests to PDPs (Policy Decision Point) which are compatible with the Open Policy Agent API.

If you're not familiar with OPA, [learn more here](https://www.openpolicyagent.org/).

## Configuration

The following options can be configured, either explicitly using `PdpClient.Builder` methods, or via environment variables.

Java method | Environment variable | Description | Default
--- | --- | --- | ---
`port` | `PDP_PORT` | Port to connect to the PDP | `8181`
`hostname` | `PDP_HOSTNAME` | Hostname of the PDP | `"localhost"`
`policyPath` | `PDP_POLICY_PATH` | Path to the policy that makes the decision | `"/authz"`
`readTimeoutMilliseconds` | `PDP_READ_TIMEOUT_MILLISECONDS` | Duration to wait on the response stream, in milliseconds | `5000` 
`connectionTimeoutMilliseconds` | `PDP_CONNECTION_TIMEOUT_MILLISECONDS` | Duration to wait for connection to establish, in milliseconds | `5000`
`retryMaxAttempts` | `PDP_RETRY_MAX_ATTEMPTS` | Total attempts at connection before giving up | `2`
`retryBackoffMilliseconds` | `PDP_RETRY_BACKOFF_MILLISECONDS` | Duration to wait before each retry, doubled on each iteration | `250`

Configuration values defined explicitly using Java methods are prioritized over values available in environment variables.

## Usage

Make a new client

```
PdpClient client = new PdpClient.Builder()
    .hostname("localhost")
    .port(8181)
    .policyPath("/mypolicy")
    .retryMaxAttempts(5);
```

Make an object that will be used as input to the PDP

```
Map<String, Object> input = new HashMap<String, Object>();

input.put("username", "myname");
```

Get a decision from the PDP

```
JsonNode response = client.getJsonResponse(input);
```
