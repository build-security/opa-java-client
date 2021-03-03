package security.build.pdp.client;

public class PdpRequest {
    PdpRequestInput input;

    public PdpRequest(PdpRequestInput input) {
        this.input = input;
    }

    public PdpRequest() {
        this.input = new PdpRequestInput();
    }

    public PdpRequestInput getInput() {
        return input;
    }
}
