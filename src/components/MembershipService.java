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
        var maxJoinAttempts = 5;

        String joinResult = joinAttempt(seedAddress, config);
        if (joinResult.equals("accepted")) {
            System.out.println(selfAddress + " Aceito. Iniciando servidor.");
            start(config);
            return true;
        }
        return false;
        //throw new JoinFailedException("Unable to join the cluster after " + maxJoinAttempts + " attempts");
    }

    private String joinAttempt(int seedAddress, Config config) {
        if (selfAddress == seedAddress) {
            System.out.println("[" + selfAddress + "] Entrando como seed");
            updateMembership(new Membership(1, Arrays.asList(new Member(selfAddress, 1))));
            membership.setSeedAddress(selfAddress);
            config.setUpNodes(membership.getUpNodesAddress());
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
            //String message = "GET /join-request?"+ selfAddress + " HTTP/1.1\r\n";

            String message = "join_request;" + selfAddress;
            protocol.send(message, inetAddress, seedAddress);
        } catch (Exception e){
            System.out.println("[Node " + seedAddress + "] Could not send join request.");
        }
    }

    private void start(Config config) {
        //startFailureDetector(config);
    }

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
//
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
            for (Integer member : existingMembers){
                sendMembershipUpdate(member);
            }
            while (collector < members){
                String responseMessage = protocol.waitForMessage(
                        msg -> msg.startsWith("ack;"), 3000
                );

                if (responseMessage != null) {
                    StringTokenizer tokenizer = new StringTokenizer(responseMessage, ";");
                    String response = tokenizer.nextToken();
                    collector++;
                    if ("ack".equals(response)) {
                        acks++;
                    }
                } else {
                    System.out.println("Timeout esperando ack de algum membro");
                    collector++;
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
            String message = "membership_update;" + membershipMessage;
            protocol.send(message, inetAddress, memberAddress);
        } catch (Exception e){
            System.out.println("Could not send membership update");
        }
    }

    private void updateMembership(Membership membership) {
        System.out.println("[" + selfAddress + "] Atualizando membership");
        this.membership = membership;
    }

    public void receiveHeartbeat(int port) {
        lastHeartbeat.put(port, System.currentTimeMillis());
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
                    System.out.println(selfAddress + " " + membership.liveMembers);
                    // Nós comuns monitoram apenas o seed
                    int seedPort = config.getSeedAddress();
                    if (now - lastHeartbeat.getOrDefault(seedPort, 0L) > 30000) {
                        System.out.println("[FailureDetector" + selfAddress + "] Seed node " + seedPort + " caiu.");
                        lastHeartbeat.remove(config.getSeedAddress());
                        // Verifica se este node é o mais velho
                        Optional<Member> oldestMember = membership.getSecondOldestMember();
                        if (oldestMember.isPresent() && oldestMember.get().getPort() == selfAddress) {
                            System.out.println("[LeaderElection] Node " + selfAddress + " é o mais velho. Assumindo como novo seed.");
                            isLeader = true;
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
        System.out.println("TOMANDO LIDERANÇA");
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
