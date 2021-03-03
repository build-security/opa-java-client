package security.build.pdp.client;

public class PdpRequestInput {
    PdpRequestIncomingHttp request;
    PdpRequestResources resources;
    PdpConnectionTuple source;
    PdpConnectionTuple destination;

    public PdpRequestInput(PdpRequestIncomingHttp request, PdpRequestResources resources, String source, String destination) {
        this.request = request;
        this.resources = resources;
        this.source = new PdpConnectionTuple(source, 0);
        this.destination = new PdpConnectionTuple(destination, 0);
    }

    public PdpRequestInput() {
        this.request = new PdpRequestIncomingHttp();
        this.resources = new PdpRequestResources();
    }

    public PdpConnectionTuple getSource() {
        return source;
    }

    public PdpConnectionTuple getDestination() {
        return destination;
    }

    public PdpRequestResources getResources() {
        return resources;
    }

    public PdpRequestIncomingHttp getRequest() {
        return request;
    }
}