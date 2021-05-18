# opa-java-client

<p align="center"><img src="Logo-build.png" class="center" alt="build-logo" width="30%"/></p>

## Abstract
[build.security](https://docs.build.security/) provides simple development and management of the organization's authorization policy.
opa-java-client is a Java middleware intended for performing authorizing requests against build.security pdp/[OPA](https://www.openpolicyagent.org/).
## Data Flow
<p align="center"> <img src="Data%20flow.png" alt="drawing" width="60%"/></p>

## Usage
Before you start we recommend completing the onboarding tutorial.

---
**Important note**

To simplify the setup process, the following example uses a local [build.security pdp instance](https://docs.build.security/policy-decision-points-pdp/pdp-deployments/standalone-docker-1).
If you are already familiar with how to run your PDP (Policy Decision Point), You can also run a pdp on you environment (Dev/Prod, etc).

In that case, don't forget to change the **hostname** and the **port** in your code.

---

### Simple usage
Make a new client

```java
PdpClient client = new PdpClient.Builder()
			.hostname("localhost")
			.port(8181).policyPath("/authz/allow")
			.retryMaxAttempts(5)
			.build();
 
Map<String, Object> input = new HashMap<String, Object>();

// put your json input here
input.put("username", "myname");

// get a decision from the PDP
JsonNode response = client.getJsonResponse(input);
```

### Optional configuration

 1. `hostname`: The hostname of the Policy Decision Point (PDP). **Default is localhost**
 2. `port`: The port at which the OPA service is running. **Default is 8181**
 3. `policyPath`: Full path to the policy (including the rule) that decides whether requests should be authorized. **Default is '/v1/data/authz/allow'**
 4. `retryMaxAttempts` - Integer. the maximum number of retry attempts in case a failure occurs. **Default is 2**.
 5. `pdp.enable`: Boolean. Whether or not to consult with the policy engine for the specific request. **Default is true**
 6. `readTimeoutMilliseconds` - Integer. Read timeout for requests in milliseconds. **Default is 5000**
 7. `connectionTimeoutMilliseconds` - Integer. Connection timeout in milliseconds. **Default is 5000**
 8. `retryBackoffMilliseconds` - Integer. The number of milliseconds to wait between two consecutive retry attempts. **Default is 250** 

The following options can be configured, either explicitly using `PdpClient.Builder` methods, or via environment variables.

Configuration values defined explicitly using Java methods are prioritized over values available in environment variables.
## Try it out

Run your PDP (OPA) instance (assuming it runs on localhost:8181) and your java server.  
* Please make sure to [define some pdp policy rules](https://docs.build.security/policies/creating-a-new-policy).
### PDP Request example

This is what the input received by the PDP would look like:

```
{
   "input":{
      "request":{
         "scheme":"http",
         "method":"GET",
         "path":"websecurity",
         "query":{
            
         },
         "headers":{
            "host":"localhost:8080",
            "user-agent":"curl/7.64.1",
            "accept":"*/*"
         }
      },
      "resources":{
         "requirements":[
            "websecurity"
         ],
         "attributes":{
            
         }
      },
      "source":{
         "ipAddress":"172.19.0.1",
         "port":0
      },
      "destination":{
         "ipAddress":"172.19.0.2",
         "port":0
      }
   }
}
```

If everything works well you should receive the following response:

```
{
    "result": {
        "allow": true
    }
}
```