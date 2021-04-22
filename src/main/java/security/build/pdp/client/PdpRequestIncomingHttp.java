package security.build.pdp.client;

import java.util.HashMap;
import java.util.Map;

public class PdpRequestIncomingHttp {
    String scheme;
    String method;
    String path;
    Map<String, String[]> query;
    Map<String, String> headers;

    public PdpRequestIncomingHttp(String scheme, String method, String path, Map<String, String[]> query, Map<String, String> headers) {
        this.scheme = scheme;
        this.method = method;
        this.path = path;
        this.query = query;
        this.headers = headers;
    }

    public PdpRequestIncomingHttp() {
        this.query = new HashMap<String, String[]>();
        this.headers = new HashMap<String, String>();
    }

    public String getScheme() { return scheme; }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String[]> getQuery() { return query; }

    public Map<String, String> getHeaders() {
        return headers;
    }
}