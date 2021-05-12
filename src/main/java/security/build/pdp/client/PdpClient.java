package security.build.pdp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implements a configurable HTTP client that request authorization decisions from a Policy Decision Point based on the
 * the input provided.
 */
public class PdpClient implements Serializable {

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static int DefaultPort = 8181;
    public static String DefaultHostname = "localhost";
    public static String DefaultSchema = "http";
    public static String DefaultPolicyPath = "v1/data/authz";
    public static int DefaultReadTimeoutMilliseconds = 5000;
    public static int DefaultConnectionTimeoutMilliseconds = 5000;
    public static int DefaultRetryMaxAttempts = 2;
    public static int DefaultRetryBackoffMilliseconds = 250;

    public static String EnvPort = "PDP_PORT";
    public static String EnvHostname = "PDP_HOSTNAME";
    public static String EnvPolicyPath = "PDP_POLICY_PATH";
    public static String EnvReadTimeoutMilliseconds = "PDP_READ_TIMEOUT_MILLISECONDS";
    public static String EnvConnectionTimeoutMilliseconds = "PDP_CONNECTION_TIMEOUT_MILLISECONDS";
    public static String EnvRetryMaxAttempts = "PDP_RETRY_MAX_ATTEMPTS";
    public static String EnvRetryBackoffMilliseconds = "PDP_RETRY_BACKOFF_MILLISECONDS";

    /**
     * Allows constructing a new PdpClient object with desired configuration.
     */
    public static class Builder {
        private int port = PdpClient.DefaultPort;
        private String hostname = PdpClient.DefaultHostname;
        private String schema = PdpClient.DefaultSchema;
        private String policyPath = PdpClient.DefaultPolicyPath;
        private int readTimeoutMilliseconds = PdpClient.DefaultReadTimeoutMilliseconds;
        private int connectionTimeoutMilliseconds = PdpClient.DefaultConnectionTimeoutMilliseconds;
        private int retryMaxAttempts = PdpClient.DefaultRetryMaxAttempts;
        private int retryBackoffMilliseconds = PdpClient.DefaultRetryBackoffMilliseconds;

        public Builder() {
        }

        public Builder port(int port) {
            this.port = port;

            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;

            return this;
        }

        public Builder policyPath(String policyPath) {
            this.policyPath = "v1/data" + policyPath;

            return this;
        }

        public Builder readTimeoutMilliseconds(int readTimeoutMilliseconds) {
            this.readTimeoutMilliseconds = readTimeoutMilliseconds;

            return this;
        }

        public Builder connectionTimeoutMilliseconds(int connectionTimeoutMilliseconds) {
            this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;

            return this;
        }

        public Builder retryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;

            return this;
        }

        public Builder retryBackoffMilliseconds(int retryBackoffMilliseconds) {
            this.retryBackoffMilliseconds = retryBackoffMilliseconds;

            return this;
        }

        public PdpClient build() {
            PdpClient client = new PdpClient();

            client.port = this.port;
            client.hostname = this.hostname;
            client.schema = this.schema;
            client.policyPath = this.policyPath;
            client.readTimeoutMilliseconds = this.readTimeoutMilliseconds;
            client.connectionTimeoutMilliseconds = this.connectionTimeoutMilliseconds;
            client.retryMaxAttempts = this.retryMaxAttempts;
            client.retryBackoffMilliseconds = this.retryBackoffMilliseconds;

            client.loadHttpClient();

            return client;
        }
    }

    private int port = DefaultPort;
    private String hostname = DefaultHostname;
    private String schema = DefaultSchema;
    private String policyPath = DefaultPolicyPath;
    private int readTimeoutMilliseconds = DefaultReadTimeoutMilliseconds;
    private int connectionTimeoutMilliseconds = DefaultConnectionTimeoutMilliseconds;
    private int retryMaxAttempts = DefaultRetryMaxAttempts;
    private int retryBackoffMilliseconds = DefaultRetryBackoffMilliseconds;

    private RetryPolicy<Object> retryPolicy;
    private OkHttpClient client;
    private ObjectMapper mapper;

    public PdpClient() {
        this.loadConfigurationFromEnvironment();

        this.mapper = new ObjectMapper();
    }

    private void loadHttpClient() {
        this.retryPolicy = new RetryPolicy<>()
                .handle(IOException.class)
                .withBackoff(this.retryBackoffMilliseconds, (this.retryBackoffMilliseconds*this.retryMaxAttempts)+1, ChronoUnit.MILLIS)
                .withMaxAttempts(this.retryMaxAttempts);

        this.client = new OkHttpClient.Builder()
                .connectTimeout(this.connectionTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .readTimeout(this.readTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false) // Disable OkHTTPs automatic retries -- they don't provide enough granularity and are harder to test.
                .build();
    }

    /**
     * Replaces the HTTP client with a mock client.
     *
     * This can only be used for mocking and testing, because the client is meant to be recreated and replaced whenever
     * configuration values change.
     *
     * @param client a mock HTTP client
     */
    public void setMockHttpClient(OkHttpClient client) {
        this.client = client;
    }

    // Properties.

    public int getPort() {
        return  this.port;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getSchema() {
        return this.schema;
    }

    public String getPolicyPath() {
        return this.policyPath;
    }

    public int getReadTimeoutMilliseconds() {
        return this.readTimeoutMilliseconds;
    }

    public int getConnectionTimeoutMilliseconds() {
        return this.connectionTimeoutMilliseconds;
    }

    public int getRetryMaxAttempts() {
        return this.retryMaxAttempts;
    }

    public int getRetryBackoffMilliseconds() {
        return this.retryBackoffMilliseconds;
    }

    /**
     * Loads configuration values from environment variables and recreates the HTTP client based on them.
     */
    public void loadConfigurationFromEnvironment() {
        Map<String, String> env = System.getenv();

        String port = env.get(EnvPort);
        if (port != null) {
            try {
                this.port = Integer.parseInt(port);
            } catch (NumberFormatException exception) {
            }
        }

        String hostname = env.get(EnvHostname);
        if (hostname != null) {
            this.hostname = hostname;
        }

        String policyPath = env.get(EnvPolicyPath);
        if (policyPath != null) {
            this.policyPath = policyPath;
        }

        String readTimeoutMilliseconds = env.get(EnvReadTimeoutMilliseconds);
        if (readTimeoutMilliseconds != null) {
            try {
                this.readTimeoutMilliseconds = Integer.parseInt(readTimeoutMilliseconds);
            } catch (NumberFormatException exception) {
            }
        }

        String connectionTimeoutMilliseconds = env.get(EnvConnectionTimeoutMilliseconds);
        if (connectionTimeoutMilliseconds != null) {
            try {
                this.connectionTimeoutMilliseconds = Integer.parseInt(connectionTimeoutMilliseconds);
            } catch (NumberFormatException exception) {
            }
        }

        String retryMaxAttempts = env.get(EnvRetryMaxAttempts);
        if (retryMaxAttempts != null) {
            try {
                this.retryMaxAttempts = Integer.parseInt(retryMaxAttempts);
            } catch (NumberFormatException exception) {
            }
        }

        String retryBackoffMilliseconds = env.get(EnvRetryBackoffMilliseconds);
        if (retryBackoffMilliseconds != null) {
            try {
                this.retryBackoffMilliseconds = Integer.parseInt(retryBackoffMilliseconds);
            } catch (NumberFormatException exception) {
            }
        }

        this.loadHttpClient();
    }

    /**
     * Returns a URL to the Policy Decision Point by constructing it as per configuration values.
     *
     * Handles expected configuration values:
     * - hostname can include the schema, or default to HTTP
     * - policyPath may or may not have a leading /
     *
     * @return a URL to the Policy Decision Point
     * @throws Throwable
     */
    public String getPdpEndpoint() throws Throwable {
        String schema = this.schema, hostname = this.hostname, policyPath = this.policyPath;

        String hostnameParts[] = this.hostname.split("://");

        if (hostnameParts.length > 2) {
            throw new MalformedURLException(String.format("Invalid schema/hostname: %s", this.hostname));
        } else if (hostnameParts.length == 2) {
            schema = hostnameParts[0];
            hostname = hostnameParts[1];
        }

        if (!policyPath.startsWith("/")) {
            policyPath = String.format("/%s", policyPath);
        }

        URL url = new URL(schema, hostname, this.port, policyPath);

        return url.toString();
    }

    /**
     * Executes the request to the Policy Decision Point and returns the response.
     *
     * @param requestObject the request to make to the Policy Decision Point
     * @return the response from the Policy Decision Point
     * @throws Throwable
     */
    public Response evaluateExecute(Object requestObject) throws Throwable {
        byte[] json = this.mapper.writeValueAsBytes(requestObject);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(getPdpEndpoint())
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        return response;
    }

    /**
     * Calls evaluateExecute with requestObject multiple times based on the retry policy, and returns the response.
     *
     * @param requestObject the request to make to the Policy Decision Point
     * @return the response from the Policy Decision Point
     * @throws Throwable
     */
    private Response evaluate(Object requestObject) throws Throwable {
        return Failsafe.with(this.retryPolicy).get(() -> evaluateExecute(requestObject));
    }

    /**
     * Returns the JSON object response from the Policy Decision Point, after making the request as per the
     * defined configuration values.
     *
     * @param input a Java native input object that is serialized for making the request to the Policy Decision Point.
     * @return the JSON object response from the Policy Decision Point.
     * @throws Throwable
     */
    public JsonNode getJsonResponse(Map<String, Object> input) throws Throwable {
        Response response = evaluate(input);

        return this.mapper.readTree(response.body().bytes());
    }

    /**
     * Returns the JSON object response from the Policy Decision Point, after making the request as per the
     * defined configuration values.
     *
     * @param request a PdpRequest object that is serialized for making the request to the Policy Decision Point.
     * @return the JSON object response from the Policy Decision Point.
     * @throws Throwable
     */
    public JsonNode getJsonResponse(PdpRequest request) throws  Throwable {
        Response response = evaluate(request);

        return this.mapper.readTree(response.body().bytes());
    }

    /**
     * Returns the response body from the Policy Decision Point, after making the request as per the
     * defined configuration values, and deserializing the JSON response.
     *
     * @param input a Java native input object that is serialized for making the request to the Policy Decision Point.
     * @return the Map representation of the response from the Policy Decision Point.
     * @throws Throwable
     */
    public Map<String, Object> getMappedResponse(Map<String, Object> input) throws Throwable {
        Response response = evaluate(input);

        return this.mapper.readValue(response.body().bytes(), new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Returns the response body from the Policy Decision Point, after making the request as per the
     * defined configuration values, and deserializing the JSON response.
     *
     * @param request a PdpRequest object that is serialized for making the request to the Policy Decision Point.
     * @return the Map representation of the response from the Policy Decision Point.
     * @throws Throwable
     */
    public Map<String, Object> getMappedResponse(PdpRequest request) throws Throwable {
        Response response = evaluate(request);

        return this.mapper.readValue(response.body().bytes(), new TypeReference<Map<String, Object>>() {});
    }
}