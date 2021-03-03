package security.build.pdp.client;

public class PdpConnectionTuple {
    String ipAddress;
    int port;

    public PdpConnectionTuple(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public PdpConnectionTuple() {
        this.ipAddress = new String();
        this.port = 0;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }
}
