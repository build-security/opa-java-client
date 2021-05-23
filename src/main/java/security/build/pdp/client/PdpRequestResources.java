package security.build.pdp.client;

import java.util.HashMap;
import java.util.Map;

public class PdpRequestResources {
    String[] permissions;
    Map<String, String> attributes;

    public PdpRequestResources(String[] permissions, Map<String, String> attributes) {
        this.permissions = permissions;
        this.attributes = attributes;
    }

    public PdpRequestResources() {
        this.permissions = new String[0];
        this.attributes = new HashMap<String, String>();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String[] getPermissions() {
        return permissions;
    }
}
