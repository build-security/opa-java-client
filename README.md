# opa-java-client
## Abstract
opa-java-client is a Java middleware meant for authorizing API requests using a 3rd party policy engine (OPA) as the Policy Decision Point (PDP).
If you're not familiar with OPA, please [learn more](https://www.openpolicyagent.org/).
## Data Flow
![enter image description here](https://github.com/build-security/opa-express-middleware/blob/main/Data%20flow.png)
## Usage
### Prerequisites 
- Finish our "onboarding" tutorial
- Run a pdp instance
---
**Important note**

In the following example we used our aws managed pdp instance to ease your first setup, but if you feel comfortable you are recommended to use your own pdp instance instead.
In that case, don't forget to change the **hostname** and the **port** in your code.

---

### Simple usage
Make a new client

```java
PdpClient client = new PdpClient.Builder()
    .hostname("localhost")
    .port(8181)
    .policyPath("/mypolicy")
    .retryMaxAttempts(5);
```

Make an object that will be used as input to the PDP

```java
Map<String, Object> input = new HashMap<String, Object>();

input.put("username", "myname");
```

Get a decision from the PDP

```java
JsonNode response = client.getJsonResponse(input);
```

### Mandatory configuration

 1. `hostname`: The hostname of the Policy Decision Point (PDP)
 2. `port`: The port at which the OPA service is running
 3. `policyPath`: Full path to the policy (including the rule) that decides whether requests should be authorized

### Optional configuration
 1. `allowOnFailure`: Boolean. "Fail open" mechanism to allow access to the API in case the policy engine is not reachable. **Default is false**.
 2. `includeBody`: Boolean. Whether or not to pass the request body to the policy engine. **Default is true**.
 3. `includeHeaders`: Boolean. Whether or not to pass the request headers to the policy engine. **Default is true**
 4. `timeout`: Boolean. Amount of time to wait before request is abandoned and request is declared as failed. **Default is 1000ms**.
 5. `enable`: Boolean. Whether or not to consult with the policy engine for the specific request. **Default is true**
 6. `enrich`: Object. An object to attach to the request that is being sent to the policy engine. **Default is an empty object {}**

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

### PDP Request example

This is what the input received by the PDP would look like.

```
{
    "input": {
        "request": {
            "method": "GET",
            "query": {
                "querykey": "queryvalue"
            },
            "path": "/some/path",
            "scheme": "http",
            "host": "localhost",
            "body": {
                "bodykey": "bodyvalue"
            },
            "headers": {
                "content-type": "application/json",
                "user-agent": "PostmanRuntime/7.26.5",
                "accept": "*/*",
                "cache-control": "no-cache",
                "host": "localhost:3000",
                "accept-encoding": "gzip, deflate, br",
                "connection": "keep-alive",
                "content-length": "24"
            }
        },
        "source": {
            "port": 63405,
            "address": "::1"
        },
        "destination": {
            "port": 3000,
            "address": "::1"
        },
        "resources": {
            "attributes": {
                "region": "israel",
                "userId": "buildsec"
            },
            "permissions": [
                "user.read"
            ]
        },
        "serviceId": 1
    }
}
```
