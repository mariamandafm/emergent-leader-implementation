package udp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MembershipService {
    int selfAddress;
    Membership membership;

    public MembershipService(int selfAddress) {
        this.selfAddress = selfAddress;
        this.membership = new Membership(0, new ArrayList<>());
    }

    public void join(int seedAddress){
        var maxJoinAttempts = 5;

        for(int i = 0; i < maxJoinAttempts; i++){
            try {
                joinAttempt(seedAddress);
                return;
            } catch (Exception e) {
                //logger.info("Join attempt " + i + "from " + selfAddress + " to" + seedAddress + " failed. Retrying");
                System.out.println("Join attempt " + i + "from " + selfAddress + " to" + seedAddress + " failed. Retrying");
            }
        }
        //throw new JoinFailedException("Unable to join the cluster after " + maxJoinAttempts + " attempts");
    }

    private void joinAttempt(int seedAddress){
        // É o seed node. Idade 1.
        if (selfAddress == seedAddress) {
            System.out.println("Entrando como seed");
            int membershipVersion = 1;
            int age = 1;
            updateMembership(new Membership(membershipVersion, Arrays.asList(new Member(selfAddress, age))));

            start();
            return;
        }
        System.out.println("Entrando como membro");
        // Caso não seja o seed node pede pra entrar
        //long id = this.messageId++;
        //var future = new CompletableFuture<JoinResponse>();
        // Cria o pedido para entrar
        //var message = new JoinRequest(id, selfAddress);
        // pendingRequests.put(id, future);
        // Pedido enviado para o seed
//        network.send(seedAddress, message);

        //var joinResponse = Uninterruptibles.getUninterruptibly(future, 5, TimeUnit.SECONDS);
        // De acordo com resposta atualiza membership
        //int membershipVersion = membership.version;
        //int age = membership.liveMembers.size();
        handleNewJoin(selfAddress);
        //updateMembership(this.membership);
        start();
    }

    private void start() {
        // heartBeatScheduler.start();
        // failuteDetector.start();
        // startSplitBrainChecker();
        // logger.info(selfAddress + " joined the cluster. Membership=" + membership);
        System.out.println(selfAddress + " joined the cluster. Membership=" + membership.getLiveMembers());
    }
//    public void handleJoinRequest(JoinRequest joinRequest) {
//        //handlePossibleRejoin(joinRequest);
//        handleNewJoin(joinRequest);
//    }

    private void handleNewJoin(int selfAddress) {
        List<Member> existingMembers = membership.getLiveMembers();
        updateMembership(membership.addNewMember(selfAddress));
        System.out.println(this.membership);
        //var resultsCollector = broadcastMembershipUpdate(existingMembers);
        //var joinResponse = new JoinResponse(joinRequest.messageId, selfAddress, membership);

        // Quando recebe todas as acks dos membros envia uma resposta para o node que está querendo entrar
        //var resultsCollector.whenComplete((response, exception) -> {
        //    System.out.println("Sending join response from " + selfAddress + " to " + joinRequest.from);
        //    network.send(joinRequest.from, joinResponse);
        //});
    }

    private void updateMembership(Membership membership) {
        this.membership = membership;
    }
}
