package udp;

public class EmergentLeader {
    public static void main(String[] args) {

        Config config = new Config(9001);

        Gateway gateway = new Gateway(config);
        gateway.start();

        Node node1 = new Node(config,9001);
        Node node2 = new Node(config,9002);
        node1.start();
        node2.start();
    }
}
