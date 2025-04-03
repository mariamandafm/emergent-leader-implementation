package udp;

import java.util.ArrayList;
import java.util.List;

public class Config {
    int seedAddress;
    private List<Integer> upNodes = new ArrayList<>();

    public Config(int seedAddress) {
        this.seedAddress = seedAddress;

    }

    public int getSeedAddress() {
        return seedAddress;
    }

    public void setSeedAddress(int seedAddress) {
        this.seedAddress = seedAddress;
    }

    public void addUpNode(int port) {
        upNodes.add(port);
    }

    public void setUpNodes(List<Integer> upNodes) {
        this.upNodes = upNodes;
    }

    public List<Integer> getUpNodes() {
        return upNodes;
    }
}
