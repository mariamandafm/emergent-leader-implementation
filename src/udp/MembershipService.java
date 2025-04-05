package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class MembershipService {
    int selfAddress;
    Membership membership;
    DatagramSocket socket;

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

            start();
            return "accepted";
        }
        try {
            System.out.println("Enviando join request pro seed");
            //DatagramSocket clientSocket = new DatagramSocket();
            byte[] receivemessage = new byte[1024];
            // Envia join request para o seed
            sendJoinRequest(socket, seedAddress);
            // Espera resposta do seed
            DatagramPacket receivepacket = new DatagramPacket(receivemessage, receivemessage.length);
            socket.receive(receivepacket);
            String responseMessage = new String(receivepacket.getData(), 0, receivepacket.getLength());
            //clientSocket.close();
            // Processa resposta
            StringTokenizer tokenizer = new StringTokenizer(responseMessage, ";");
            String response = tokenizer.nextToken();

            return response;
        } catch (Exception e) {
            System.out.println("[Node " + seedAddress + "] Could not join cluster");
        }
        return "Erro";
    }

    private void sendJoinRequest(DatagramSocket clientSocket, int seedAddress) {
        try{
            InetAddress inetAddress = InetAddress.getByName("localhost");
            byte[] sendMessage;

            String message = "join_request;" + selfAddress;
            sendMessage = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, seedAddress);
            clientSocket.send(sendPacket);
        } catch (Exception e){
            System.out.println("[Node " + seedAddress + "] Could not send join request.");
        }

    }

    private void sendJoinUpdate(DatagramSocket clientSocket, int seedAddress) {
        // ...
    }

    private void start() {
        System.out.println(selfAddress + " joined the cluster. Membership=" + membership.getLiveMembers());
    }
//    public void handleJoinRequest(JoinRequest joinRequest) {
//        //handlePossibleRejoin(joinRequest);
//        handleNewJoin(joinRequest);
//    }

    public void handleNewJoin(int joinAddress, Config config) {
        if (membership.contains(joinAddress)) {
            System.out.println("Node " + joinAddress + " já está no cluster. Ignorando.");
            return;
        }
        List<Member> existingMembers = membership.getLiveMembers();
        updateMembership(membership.addNewMember(joinAddress));
//        System.out.println("Membros: " + this.membership.liveMembers);

        config.setUpNodes(membership.getUpNodesAddress());
        broadcastMembershipUpdate(membership.getUpNodesAddress());
        //var resultsCollector = broadcastMembershipUpdate(existingMembers);
        //var joinResponse = new JoinResponse(joinRequest.messageId, selfAddress, membership);

        // Quando recebe todas as acks dos membros envia uma resposta para o node que está querendo entrar
        //var resultsCollector.whenComplete((response, exception) -> {
        //    System.out.println("Sending join response from " + selfAddress + " to " + joinRequest.from);
        //    network.send(joinRequest.from, joinResponse);
        //});
    }

    // Seed node envia uma mensagem para todos os nodes quando um novo membro entra no cluster
    // Os nodes envian um ack para confirmar o recebimento.
    private void broadcastMembershipUpdate(List<Integer> existingMembers){
        //...
        var members = existingMembers.size();
        var collector = 0;

        for (Integer member : existingMembers){

        }
    }



    private void updateMembership(Membership membership) {
        System.out.println("Atualizando membership");
        System.out.println(membership.version);
        System.out.println(membership.liveMembers);
        this.membership = membership;
    }
}
