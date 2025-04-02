package udp;

import java.util.List;

public class Config {
    int seedAddress;
    private List<Integer> upNodes;

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
}
