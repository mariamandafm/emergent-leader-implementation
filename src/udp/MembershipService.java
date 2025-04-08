package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MembershipService {
    int selfAddress;
    Membership membership;
    DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeat = new ConcurrentHashMap<>();

    private Thread failureDetectorThread;
    private boolean failureDetectorRunning;


    public MembershipService(int selfAddress, DatagramSocket socket) {
        this.selfAddress = selfAddress;
        this.membership = new Membership(0, new ArrayList<>());
        this.socket = socket;
    }

    public boolean join(int seedAddress, Config config){
        var maxJoinAttempts = 5;

        for(int i = 0; i < maxJoinAttempts; i++){
            try {
                String joinResult = joinAttempt(seedAddress, config);
                if (joinResult.equals("accepted")) {
                    start(config);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("Join attempt " + i + "from " + selfAddress + " to" + seedAddress + " failed. Retrying");
            }
        }
        return false;
        //throw new JoinFailedException("Unable to join the cluster after " + maxJoinAttempts + " attempts");
    }

    private String joinAttempt(int seedAddress, Config config){
        // É o seed node.
        if (selfAddress == seedAddress) {
            System.out.println("Entrando como seed");
            int membershipVersion = 1;
            int age = 1;
            updateMembership(new Membership(membershipVersion, Arrays.asList(new Member(selfAddress, age))));
            config.setUpNodes(membership.getUpNodesAddress());

            start(config);
            return "accepted";
        }
        try {
            System.out.println("Enviando join request pro seed");
            //DatagramSocket clientSocket = new DatagramSocket();
            byte[] receivemessage = new byte[1024];
            // Envia join request para o seed
            sendJoinRequest(seedAddress);
            // Espera resposta do seed
            DatagramPacket receivepacket = new DatagramPacket(receivemessage, receivemessage.length);
            socket.receive(receivepacket);
            String responseMessage = new String(receivepacket.getData(), 0, receivepacket.getLength());

            StringTokenizer tokenizer = new StringTokenizer(responseMessage, ";");
            String responseStatus = tokenizer.nextToken();
            String responseMembership = tokenizer.nextToken();
            String responseMembershipVersion = tokenizer.nextToken();
            membership.deserializeMembershipAndUpdate(responseMembership, responseMembershipVersion);
            return responseStatus;
        } catch (Exception e) {
            System.out.println("[Node " + seedAddress + "] Could not join cluster");
        }
        return "Erro";
    }

    private void sendJoinRequest(int seedAddress) {
        try{
            InetAddress inetAddress = InetAddress.getByName("localhost");
            byte[] sendMessage;

            String message = "join_request;" + selfAddress;
            sendMessage = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, seedAddress);
            socket.send(sendPacket);
        } catch (Exception e){
            System.out.println("[Node " + seedAddress + "] Could not send join request.");
        }
    }

    private void start(Config config) {
        startFailureDetector(config);
        System.out.println(selfAddress + " joined the cluster. Membership=" + membership.getLiveMembers());
    }

    // TODO
//    public void handleJoinRequest(JoinRequest joinRequest) {
//        //handlePossibleRejoin(joinRequest);
//        handleNewJoin(joinRequest);
//    }

    public void handleNewJoin(int joinAddress, Config config) {
        if (membership.contains(joinAddress)) {
            System.out.println("Node " + joinAddress + " já está no cluster. Ignorando.");
            return;
        }
        updateMembership(membership.addNewMember(joinAddress));
        List<Member> existingMembers = membership.getLiveMembers();

        System.out.println("[ " + selfAddress + " ] Enviando broadcast de join request");
        config.setUpNodes(membership.getUpNodesAddress());
        // Envia uma cópia da lista de nodes ativos
        List<Integer> members = new ArrayList<>(membership.getUpNodesAddress());
        broadcastMembershipUpdate(members, joinAddress);
    }

    // Seed node envia uma mensagem para todos os nodes quando um novo membro entra no cluster
    // Os nodes envian um ack para confirmar o recebimento.
    private boolean broadcastMembershipUpdate(List<Integer> existingMembers, int joinAddress) {

        //remove o seed e o novo node, pois eles não precisam receber o novo
        existingMembers.remove(Integer.valueOf(selfAddress));

        // para casos em que há o join
        if (joinAddress > 0){
            existingMembers.remove(Integer.valueOf(joinAddress));
        }
        var members = existingMembers.size();
        if (members == 0){
            System.out.println("Only seed in the cluster, no need for broacast");
            return true;
        } else {
            var collector = 0;
            var acks = 0;
            System.out.println("Membros existentes");
            for (Integer member : existingMembers){
                sendMembershipUpdate(member);
            }
            while (collector < members){
                try{
                    byte[] receivemessage = new byte[1024];
                    DatagramPacket receivepacket = new DatagramPacket(receivemessage, receivemessage.length);
                    socket.receive(receivepacket);

                    String responseMessage = new String(receivepacket.getData(), 0, receivepacket.getLength());
                    StringTokenizer tokenizer = new StringTokenizer(responseMessage, ";");
                    String response = tokenizer.nextToken();
                    collector++;
                    if (Objects.equals(response, "ack")) {
                        acks++;
                    }

                } catch (Exception e) {
                    System.out.println("Could not receive update response from nodes");
                }
            }

            if (acks == members) {
                System.out.println("Ack received from all members");
                return true;
            }
        }

        return false;
    }

    private void sendMembershipUpdate(int memberAddress) {
        String membershipMessage = membership.getSerializedMembership();
        try{
            InetAddress inetAddress = InetAddress.getByName("localhost");
            byte[] sendMessage;
            String message = "membership_update;" + membershipMessage;
            sendMessage = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, memberAddress);
            socket.send(sendPacket);
        } catch (Exception e){
            System.out.println("Could not send membership update");
        }
    }

    private void updateMembership(Membership membership) {
        System.out.println("Atualizando membership");
        System.out.println(membership.version);
        System.out.println(membership.liveMembers);
        this.membership = membership;
    }

    public void receiveHeartbeat(int port) {
        lastHeartbeat.put(port, System.currentTimeMillis());
        System.out.println(lastHeartbeat);
    }

    private void startFailureDetector(Config config) {
        this.failureDetectorRunning = true;
        failureDetectorThread = new Thread(() -> {
            boolean isLeader = selfAddress == membership.getSeedAddress();

            while (failureDetectorRunning) {
                long now = System.currentTimeMillis();

                if (isLeader) {
                    // Líder (seed node) verifica todos os nodes
                    for (Integer nodePort : new HashSet<>(lastHeartbeat.keySet())) {
                        if (now - lastHeartbeat.getOrDefault(nodePort, 0L) > 20000) {
                            System.out.println("[FailureDetector " + selfAddress +  "] Node " + nodePort + " não respondeu. Removendo do cluster.");
                            config.getUpNodes().remove(nodePort);
                            lastHeartbeat.remove(nodePort);
                            membership = membership.removeMember(nodePort);
                            broadcastMembershipUpdate(membership.getUpNodesAddress(), 0);
                            config.setUpNodes(membership.getUpNodesAddress());
                        }
                    }
                } else {
                    // Nós comuns monitoram apenas o seed
                    int seedPort = config.getSeedAddress();
                    if (now - lastHeartbeat.getOrDefault(seedPort, 0L) > 20000) {
                        System.out.println("[FailureDetector" + selfAddress + "] Seed node " + seedPort + " caiu.");
                        lastHeartbeat.remove(config.getSeedAddress());
                        // Verifica se este node é o mais velho
                        Optional<Member> secondOldest = membership.getSecondOldestMember();

                        if (secondOldest.isPresent() && secondOldest.get().getPort() == selfAddress) {
                            System.out.println("[LeaderElection] Node " + selfAddress + " é o mais velho. Assumindo como novo seed.");
                            isLeader = true; // Agora pode remover nodes
                            takeLeadership(config);
                        }
                    }
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {}
            }
        });
        failureDetectorThread.start();
    }

    private void takeLeadership(Config config){
        membership = membership.removeMember(config.getSeedAddress());
        membership.seedAddress = selfAddress; // Define-se como novo seed

        //atualizar config
        config.setUpNodes(membership.getUpNodesAddress());
        config.setSeedAddress(selfAddress);

        broadcastMembershipUpdate(membership.getUpNodesAddress(), 0);
    }

    public void stopFailureDetector(){
        System.out.println("Parando failure detector na thread" + selfAddress);
        this.failureDetectorRunning = false;
        failureDetectorThread.interrupt();
    }
}
