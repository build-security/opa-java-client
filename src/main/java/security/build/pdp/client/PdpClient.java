package security.build.pdp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PdpClient implements Serializable {

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static int DefaultPort = 8181;
    public static String DefaultHostname = "localhost";
    public static String DefaultSchema = "http";
    public static String DefaultPolicyPath = "/authz";
    public static int DefaultReadTimeoutMilliseconds = 5000;
    public static int DefaultConnectionTimeoutMilliseconds = 5000;
    public static int DefaultRetryMaxAttempts = 2;
    public static int DefaultRetryBackoffMilliseconds = 250;

    public static String EnvPort = "PDP_PORT";
    public static String EnvHostname = "PDP_HOSTNAME";
    public static String EnvSchema = "PDP_SCHEMA";
    public static String EnvPolicyPath = "PDP_POLICY_PATH";
    public static String EnvReadTimeoutMilliseconds = "PDP_READ_TIMEOUT_MILLISECONDS";
    public static String EnvConnectionTimeoutMilliseconds = "PDP_CONNECTION_TIMEOUT_MILLISECONDS";
    public static String EnvRetryMaxAttempts = "PDP_RETRY_MAX_ATTEMPTS";
    public static String EnvRetryBackoffMilliseconds = "PDP_RETRY_BACKOFF_MILLISECONDS";

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
            this.policyPath = policyPath;

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

            client.configureHttpClient();

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

    private OkHttpClient client;

    private ObjectMapper mapper;

    public PdpClient() {
        this.loadConfigurationFromEnvironment();

        this.mapper = new ObjectMapper();
    }

    private void configureHttpClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(this.connectionTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .readTimeout(this.readTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .build();

        // TODO(yashtewari): Configure retries.
    }

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

        String schema = env.get(EnvSchema);
        if (schema != null) {
            this.schema = schema;
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

        this.configureHttpClient();
    }

    public String getPdpEndpoint() throws URISyntaxException {
        URI uri = new URI(this.schema, null, this.hostname, this.port, this.policyPath, null, null);

        return uri.toString();
    }

    private ResponseBody evaluate(Object requestObject) throws Throwable {
        byte[] json = this.mapper.writeValueAsBytes(requestObject);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(getPdpEndpoint())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body();
        }
    }

    public JsonNode getJsonResponse(Map<String, Object> input) throws Throwable {
        ResponseBody body = evaluate(input);

        return this.mapper.readTree(body.bytes());
    }

    public JsonNode getJsonResponse(PdpRequest request) throws  Throwable {
        ResponseBody body = evaluate(request);

        return this.mapper.readTree(body.bytes());
    }

    public Map<String, Object> getMappedResponse(Map<String, Object> input) throws Throwable {
        ResponseBody body = evaluate(input);

        return this.mapper.readValue(body.bytes(), new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> getMappedResponse(PdpRequest request) throws Throwable {
        ResponseBody body = evaluate(request);

        return this.mapper.readValue(body.bytes(), new TypeReference<Map<String, Object>>() {});
    }
}