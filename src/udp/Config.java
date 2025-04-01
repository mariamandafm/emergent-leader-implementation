package udp;

import java.util.List;

public class Config {
    int seedAddress;

    public Config(int seedAddress) {
        this.seedAddress = seedAddress;
    }

    public int getSeedAddress() {
        return seedAddress;
    }

    public void setSeedAddress(int seedAddress) {
        this.seedAddress = seedAddress;
    }
}
