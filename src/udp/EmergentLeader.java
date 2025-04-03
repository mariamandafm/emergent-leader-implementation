package udp;

public class EmergentLeader {
    public static void main(String[] args) throws InterruptedException {

        Config config = new Config(9001);

        Gateway gateway = new Gateway(config);
        gateway.start();

        Node node1 = new Node(config,9001);
        Node node2 = new Node(config,9002);
        Node node3 = new Node(config,9003);
        node1.start();
        //Thread.sleep(10000);
        node2.start();
        node3.start();
    }
}
