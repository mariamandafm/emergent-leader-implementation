package components;

import protocols.Protocol;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MembershipService {
    int selfAddress;
    public Membership membership;
    //DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeat = new ConcurrentHashMap<>();

    private Thread failureDetectorThread;
    private boolean failureDetectorRunning;

    private Protocol protocol;


    public MembershipService(int selfAddress, Protocol protocol) {
        this.selfAddress = selfAddress;
        this.membership = new Membership(0, new ArrayList<>());
        this.protocol = protocol;
    }

    public boolean join(int seedAddress, Config config){
        String joinResult = joinAttempt(seedAddress, config);
        if (joinResult.equals("accepted")) {
            start();
            return true;
        }
        return false;
    }

    private String joinAttempt(int seedAddress, Config config) {
        if (selfAddress == seedAddress) {
            System.out.println("[" + selfAddress + "] Entrando como seed");
            updateMembership(new Membership(1, Arrays.asList(new Member(selfAddress, 1))));
            membership.setSeedAddress(selfAddress);
            return "accepted";
        }

        try {
            System.out.println("Enviando join request para seed " + seedAddress);
            sendJoinRequest(config.getSeedAddress());

            return "accepted";
        } catch (Exception e) {
            System.out.println("Erro no join attempt: " + e.getMessage());
            return "error;join_failed";
        }
    }

    private void sendJoinRequest(int seedAddress) {
        try{
            InetAddress inetAddress = InetAddress.getByName("localhost");

            String message = "join_request;" + selfAddress;
            protocol.send(message, inetAddress, seedAddress);
        } catch (Exception e){
            System.out.println("[Node " + seedAddress + "] Could not send join request.");
        }
    }

    private void start() {
        startFailureDetector();
    }

    public void handleNewJoin(int joinAddress) {
        if (membership.contains(joinAddress)) {
            System.out.println("Node " + joinAddress + " já está no cluster. Ignorando.");
            return;
        }
        updateMembership(membership.addNewMember(joinAddress));
        List<Member> existingMembers = membership.getLiveMembers();

        System.out.println("[ " + selfAddress + " ] Enviando broadcast de join request");
        List<Integer> members = new ArrayList<>(membership.getUpNodesAddress());
        broadcastMembershipUpdate(members, joinAddress);
    }

    // Seed node envia uma mensagem para todos os nodes quando um novo membro entra no cluster
    // Os nodes envian um ack para confirmar o recebimento.
    private boolean broadcastMembershipUpdate(List<Integer> existingMembers, int joinAddress) {
        existingMembers.add(9000);

        existingMembers.remove(Integer.valueOf(selfAddress));

        if (joinAddress > 0){
            existingMembers.remove(Integer.valueOf(joinAddress));
        }
        var members = existingMembers.size();
        if (members == 0){
            System.out.println("Apenas o seed está no cluster, não há necessidade de broadcast");
            return true;
        } else {
//            var collector = 0;
//            var acks = 0;
            for (Integer member : existingMembers){
                sendMembershipUpdate(member);
            }
//            while (collector < members){
//                String responseMessage = protocol.waitForMessage(
//                        msg -> msg.contains("ack"), 3000
//                );
//
//                if (responseMessage != null) {
//                    StringTokenizer tokenizer = new StringTokenizer(responseMessage, ";");
//                    String response = tokenizer.nextToken();
//                    collector++;
//                    if (response.contains("ack")) {
//                        acks++;
//                    }
//                } else {
//                    System.out.println("Timeout esperando ack de algum membro");
//                    collector++;
//                }
//            }
//
//            if (acks == members) {
//                System.out.println("Ack recebido de todos os membros");
//
//            }
            return true;
        }

        //return false;
    }

    private void sendMembershipUpdate(int memberAddress) {
        String membershipMessage = membership.getSerializedMembership();
        try{
            InetAddress inetAddress = InetAddress.getByName("localhost");
            String message = "membership_update;" + membershipMessage;
            protocol.send(message, inetAddress, memberAddress);
        } catch (Exception e){
            System.out.println("Não foi possível mandar atualização do membership");
        }
    }

    private void updateMembership(Membership membership) {
        System.out.println("[" + selfAddress + "] Atualizando membership");
        this.membership = membership;
    }

    public void receiveHeartbeat(int port) {
        lastHeartbeat.put(port, System.currentTimeMillis());
    }

    private void startFailureDetector() {
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
                            lastHeartbeat.remove(nodePort);
                            membership = membership.removeMember(nodePort);
                            broadcastMembershipUpdate(membership.getUpNodesAddress(), 0);
                        }
                    }
                } else {
                    int seedPort = membership.getSeedAddress();
                    if (!lastHeartbeat.isEmpty() & (now - lastHeartbeat.getOrDefault(seedPort, 0L) > 40000)) {
                        System.out.println("[FailureDetector" + selfAddress + "] Seed node " + seedPort + " caiu.");
                        lastHeartbeat.remove(seedPort);
                        Optional<Member> oldestMember = membership.getSecondOldestMember();
                        if (oldestMember.isPresent() && oldestMember.get().getPort() == selfAddress) {
                            System.out.println("[LeaderElection] Node " + selfAddress + " é o mais velho. Assumindo como novo seed.");
                            isLeader = true;
                            takeLeadership(seedPort);
                        }
                    }
                }
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ignored) {}
            }
        });
        failureDetectorThread.start();
    }

    private void takeLeadership(int seedPort){
        System.out.println("TOMANDO LIDERANÇA");
        membership = membership.removeMember(seedPort);
        membership.seedAddress = selfAddress; // Define-se como novo seed


        broadcastMembershipUpdate(membership.getUpNodesAddress(), 0);
    }

    public void stopFailureDetector(){
        System.out.println("Parando failure detector na thread" + selfAddress);
        this.failureDetectorRunning = false;
        failureDetectorThread.interrupt();
    }
}
