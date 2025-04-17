package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    int seedAddress;
    private List<Integer> upNodes = new ArrayList<>();
    private Map<Integer, Node> nodes = new HashMap<>();
    public Config(int seedAddress) {
        this.seedAddress = seedAddress;
    }

    public int getSeedAddress() {
        return seedAddress;
    }

    public void setSeedAddress(int seedAddress) {
        this.seedAddress = seedAddress;
    }

    public void setUpNodes(List<Integer> upNodes) {
        this.upNodes = upNodes;
    }

    public List<Integer> getUpNodes() {
        return upNodes;
    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }

}
