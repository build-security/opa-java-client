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

import security.build.pdp.client.PdpRequest;

import java.io.*;

import static org.mockito.Mockito.*;

class PdpClientTest {

    private static PdpClient PdpClient;

    private OkHttpClient mockClient;
    private Call mockCall;
    private Response mockResponse;

    @BeforeAll
    public static void setup() {
        // Use the same PdpClient for all test with the same retry template.
        PdpClient = new PdpClient.Builder()
                .retryMaxAttempts(2)
                .retryBackoffMilliseconds(50)
                .build();
    }

    @BeforeEach
    public void beforeEach() {
        // Use a different mock OkHttpClient and Call for each test because mocking is different in each test.
        this.mockClient = mock(OkHttpClient.class);
        this.mockCall = mock(Call.class);

        when(this.mockClient.newCall(any(Request.class))).thenReturn(this.mockCall);

        PdpClient.setMockHttpClient(this.mockClient);

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
                        return PdpClient.JSON;
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
    void getJsonResponse_statusOk_noRetry() throws Throwable {
        when(this.mockCall.execute()).thenReturn(this.mockResponse);

        PdpRequest request = new PdpRequest();
        JsonNode node = PdpClient.getJsonResponse(request);

        // Assert that there was no retry on a successful attempt.
        verify(this.mockCall, times(1)).execute();

        // Assert that the returned JSON node is correct.
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_serverError_retry() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException()).thenReturn(this.mockResponse);



        PdpRequest request = new PdpRequest();
        JsonNode node = PdpClient.getJsonResponse(request);

        //assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();

        //assert that the returned JSON node is correct
        Assertions.assertEquals("1", node.get("a").asText());
        Assertions.assertEquals("2", node.get("b").asText());
    }

    @Test()
    void getJsonResponse_serverError_retriesExhausted() throws Throwable {
        when(this.mockCall.execute())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        PdpRequest request = new PdpRequest();
        try {
            PdpClient.getJsonResponse(request);
        } catch (FailsafeException exception) {
            //we are expecting a 5xx exception to be thrown
        }

        //assert that there were exactly 2 attempts
        verify(this.mockCall, times(2)).execute();
    }
}

