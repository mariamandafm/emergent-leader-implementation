package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

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
        existingMembers.remove(Integer.valueOf(joinAddress));
        var members = existingMembers.size();
        if (members == 0){
            System.out.println("Only seed in the cluster, no need for broacast");
            return true;
        } else {
            var collector = 0;
            var acks = 0;
            System.out.println("Membros existentes");
            for (Integer member : existingMembers){
                sendJoinUpdate(member);
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

    private void sendJoinUpdate(int memberAddress) {
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
}
