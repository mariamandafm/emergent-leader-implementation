package udp;

public class Member {
    private int age;
    private int port;

    public Member(int port, int age) {
        this.age = age;
        this.port = port;
    }

    public int getAge() {
        return age;
    }

    public int getPort() {
        return port;
    }
}
