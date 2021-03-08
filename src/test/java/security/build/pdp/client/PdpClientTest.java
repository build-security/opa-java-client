package security.build.pdp.client;

import com.fasterxml.jackson.databind.JsonNode;
import net.jodah.failsafe.FailsafeException;
import okhttp3.*;
import okio.BufferedSource;
import okio.Okio;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class PdpClientTest {

    private static PdpClient staticPdpClient;

    private OkHttpClient mockHttpClient;
    private Call mockCall;
    private Response mockResponse;

    @BeforeAll
    public static void setup() {
        // Use the same PdpClient for all test with the same retry template.
        staticPdpClient = new PdpClient.Builder()
                .retryMaxAttempts(2)
                .retryBackoffMilliseconds(50)
                .build();
    }

    @BeforeEach
    public void beforeEach() {
        // Use a different mock OkHttpClient and Call for each test because mocking is different in each test.
        this.mockHttpClient = mock(OkHttpClient.class);
        this.mockCall = mock(Call.class);

        when(this.mockHttpClient.newCall(any(Request.class))).thenReturn(this.mockCall);

        staticPdpClient.setMockHttpClient(this.mockHttpClient);

        // Use a different mock Response for each test, because Response body is consumable only once.
        String mockResponseBody = "{\"a\":\"1\",\"b\":\"2\"}";

        this.mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://random.org").build())
                .protocol(Protocol.HTTP_1_0)
                .message(new String("message"))
                .code(200)
                .body(new ResponseBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return staticPdpClient.JSON;
                    }

                    @Override
                    public long contentLength() {
                        return mockResponseBody.getBytes().length;
                    }

                    @NotNull
                    @Override
                    public BufferedSource source() {
                        return Okio.buffer(Okio.source(new ByteArrayInputStream(mockResponseBody.getBytes())));
                    }
                })
                .build();
    }

    @Test()
    void getPdpEndpoint() throws Throwable {
        class testcase {
            public PdpClient client;
            public String expectedPdpEndpoint;

            testcase(PdpClient client, String expected) {
                this.client = client;
                this.expectedPdpEndpoint = expected;
            }
        }

        testcase cases[] = new testcase[]{
                new testcase(
                        new PdpClient(),
                        new String("http://localhost:8181/authz")
                ),
                new testcase(
                        new PdpClient.Builder()
                            .hostname("somehost")
                            .build(),
                        new String("http://somehost:8181/authz")
                ),
                new testcase(
                        new PdpClient.Builder()
                                .hostname("http://somehost-with-http")
                                .build(),
                        new String("http://somehost-with-http:8181/authz")
                ),
                new testcase(
                        new PdpClient.Builder()
                                .hostname("https://somehost-with-https")
                                .build(),
                        new String("https://somehost-with-https:8181/authz")
                ),
                new testcase(
                        new PdpClient.Builder()
                                .port(8182)
                                .build(),
                        new String("http://localhost:8182/authz")
                ),
                new testcase(
                        new PdpClient.Builder()
                                .policyPath("/somepath")
                                .build(),
                        new String("http://localhost:8181/somepath")
                ),
                new testcase(
                        new PdpClient.Builder()
                                .policyPath("somepath_without_leading_slash")
                                .build(),
                        new String("http://localhost:8181/somepath_without_leading_slash")
                ),
        };

        for (testcase tc: cases) {
            String endpoint = tc.client.getPdpEndpoint();

            Assertions.assertEquals(tc.expectedPdpEndpoint, endpoint);
        }
    }

    @Test()
    void getJsonResponse_pdpRequest_statusOk_noRetry() throws Throwable {
        when(this.mockCall.execute()).thenReturn(this.mockResponse);

        PdpRequest request = new PdpRequest();
        JsonNode node = staticPdpClient.getJsonResponse(request);

        // Assert that there was no retry on a successful attempt.
        verify(this.mockCall, times(1)).execute();

        // Assert that the returned JSON node is correct.
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_pdpRequest_serverError_retry() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException()).thenReturn(this.mockResponse);

        PdpRequest request = new PdpRequest();
        JsonNode node = staticPdpClient.getJsonResponse(request);

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();

        // Assert that the returned JSON node is correct
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_pdpRequest_serverError_retriesExhausted() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        PdpRequest request = new PdpRequest();
        try {
            staticPdpClient.getJsonResponse(request);
        } catch (FailsafeException exception) {
            //we are expecting a 5xx exception to be thrown
        }

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();
    }

    @Test()
    void getJsonResponse_map_statusOk_noRetry() throws Throwable {
        when(this.mockCall.execute()).thenReturn(this.mockResponse);

        Map<String, Object> input = new HashMap<String, Object>();
        JsonNode node = staticPdpClient.getJsonResponse(input);

        // Assert that there was no retry on a successful attempt.
        verify(this.mockCall, times(1)).execute();

        // Assert that the returned JSON node is correct.
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_map_serverError_retry() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException()).thenReturn(this.mockResponse);

        Map<String, Object> input = new HashMap<String, Object>();
        JsonNode node = staticPdpClient.getJsonResponse(input);

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();

        // Assert that the returned JSON node is correct
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_map_serverError_retriesExhausted() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        Map<String, Object> input = new HashMap<String, Object>();
        try {
            JsonNode node = staticPdpClient.getJsonResponse(input);
        } catch (FailsafeException exception) {
            //we are expecting a 5xx exception to be thrown
        }

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();
    }

    @Test()
    void getMappedResponse_pdpRequest_statusOk_noRetry() throws Throwable {
        when(this.mockCall.execute()).thenReturn(this.mockResponse);

        PdpRequest request = new PdpRequest();
        Map<String, Object> response = staticPdpClient.getMappedResponse(request);

        // Assert that there was no retry on a successful attempt.
        verify(this.mockCall, times(1)).execute();

        // Assert that the returned JSON node is correct.
        Assertions.assertEquals("1", response.get("a"));
        Assertions.assertEquals("2", response.get("b"));
    }

    @Test()
    void getMappedResponse_pdpRequest_serverError_retry() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException()).thenReturn(this.mockResponse);

        PdpRequest request = new PdpRequest();
        Map<String, Object> response = staticPdpClient.getMappedResponse(request);

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();

        // Assert that the returned JSON node is correct
        Assertions.assertEquals("1", response.get("a"));
        Assertions.assertEquals("2", response.get("b"));
    }

    @Test()
    void getMappedResponse_pdpRequest_serverError_retriesExhausted() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        PdpRequest request = new PdpRequest();
        try {
            staticPdpClient.getMappedResponse(request);
        } catch (FailsafeException exception) {
            // We are expecting a 5xx exception to be thrown.
        }

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();
    }

    @Test()
    void getMappedResponse_map_statusOk_noRetry() throws Throwable {
        when(this.mockCall.execute()).thenReturn(this.mockResponse);

        Map<String, Object> input = new HashMap<String, Object>();
        Map<String, Object> response = staticPdpClient.getMappedResponse(input);

        // Assert that there was no retry on a successful attempt.
        verify(this.mockCall, times(1)).execute();

        // Assert that the returned JSON node is correct.
        Assertions.assertEquals("1", response.get("a"));
        Assertions.assertEquals("2", response.get("b"));
    }

    @Test()
    void getMappedResponse_map_serverError_retry() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException()).thenReturn(this.mockResponse);

        Map<String, Object> input = new HashMap<String, Object>();
        Map<String, Object> response = staticPdpClient.getMappedResponse(input);

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();

        // Assert that the returned JSON node is correct
        Assertions.assertEquals("1", response.get("a"));
        Assertions.assertEquals("2", response.get("b"));
    }

    @Test()
    void getMappedResponse_map_serverError_retriesExhausted() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        Map<String, Object> input = new HashMap<String, Object>();
        try {
            Map<String, Object> response = staticPdpClient.getMappedResponse(input);
        } catch (FailsafeException exception) {
            // We are expecting a 5xx exception to be thrown.
        }

        // Assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();
    }
}

