//import com.sun.net.httpserver.Request;
//
//import java.time.Duration;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.stream.Collectors;
//
//public class MembershipService {
//    // ..
//    Membership membership;
//    InetAddressAndPort selfAddress;
//    Map<Long, CompletableFuture> pendingRequests = new HashMap<>();
//    int messageId;
//
////    splitbrainCheckTask = taskScheduler.scheduleWithFixedDelay(() -> {
////        searchOtherClusterGroups();
////    }, 1, 1, TimeUnit.SECONDS);
//
//    TimeoutBasedFailureDetector<InetAddressAndPort> failureDetector = new TimeoutBasedFailureDetector<InetAddressAndPort>(Duration.ofSeconds(2));
//
//    // Metodo para se juntar ao cluster a partir do seed node.
//    public void join(InetAddressAndPort seedAddress){
//        var maxJoinAttempts = 5;
//
//        for(int i = 0; i < maxJoinAttempts; i++){
//            try {
//                joinAttempt(seedAddress);
//                return;
//            } catch (Exception e) {
//                //logger.info("Join attempt " + i + "from " + selfAddress + " to" + seedAddress + " failed. Retrying");
//                System.out.println("Join attempt " + i + "from " + selfAddress + " to" + seedAddress + " failed. Retrying");
//            }
//        }
//        throw new JoinFailedException("Unable to join the cluster after " + maxJoinAttempts + " attempts");
//    }
//
//    // Tentativa de se juntar ao cluster
//    private joinAttempt(InetAddressAndPort seedAddress) throws ExecutionException, TimeoutException {
//        // É o seed node. Idade 1.
//        if (selfAddress.equals(seedAddress)) {
//            int membershipVersion = 1;
//            int age = 1;
//            updateMembership(new Membership(membershipVersion, Arrays.asList(new Member(selfAddress, age, MemberStatus.JOINED))));
//
//            start();
//            return;
//        }
//        // Caso não seja o seed node pede pra entrar
//        long id = this.messageId++;
//        var future = new CompletableFuture<JoinResponse>();
//        // Cria o pedido para entrar
//        var message = new JoinRequest(id, selfAddress);
//        pendingRequests.put(id, future);
//        // Pedido enviado para o seed
//        network.send(seedAddress, message);
//
//        var joinResponse = Uninterruptibles.getUninterruptibly(future, 5, TimeUnit.SECONDS);
//        // De acordo com resposta atualiza membership
//        updateMembership(joinResponse.getMembership());
//        start();
//    }
//
//    private void start() {
//        // heartBeatScheduler.start();
//        // failuteDetector.start();
//        // startSplitBrainChecker();
//        // logger.info(selfAddress + " joined the cluster. Membership=" + membership);
//        System.out.println(selfAddress + " joined the cluster. Membership=" + membership);
//    }
//
//    private void updateMembership(Membership membership) {
//        this.membership = membership;
//    }
//
//    public void handleJoinRequest(JoinRequest joinRequest) {
//        handlePossibleRejoin(joinRequest);
//        handleNewJoin(joinRequest);
//    }
//
//    private void handleNewJoin(JoinRequest joinRequest) {
//        List<Member> existingMembers = membership.getLiveMembers();
//        updateMembership(membership.addNewMember(joinRequest.from));
//
//        var resultsCollector = broadcastMembershipUpdate(existingMembers);
//        var joinResponse = new JoinResponse(joinRequest.messageId, selfAddress, membership);
//
//        // Quando recebe todas as acks dos membros envia uma resposta para o node que está querendo entrar
//        var resultsCollector.whenComplete((response, exception) -> {
//            System.out.println("Sending join response from " + selfAddress + " to " + joinRequest.from);
//            network.send(joinRequest.from, joinResponse);
//        });
//    }
//
//    // Seed node envia uma mensagem para todos os nodes quando um novo membro entra no cluster
//    // Os nodes envian um ack para confirmar o recebimento.
//    private ResultsCollector broadcastMembershipUpdate(List<Member> existingMembers){
//        var resultsCollector = sendMembershipUpdateTo(existingMembers);
//        resultsCollector.orTimeout(2, TimeUnit.SECONDS);
//        return resultsCollector;
//    }
//
//    private ResultsCollector sendMembershipUpdateTo(List<Member> existingMembers){
//        var otherMembers = otherMembers(existingMembers);
//        var collector = new ResultsCollector(otherMembers.size()); // Número de acks
//        if (otherMembers.size() == 0) { // Se não houver acks o trabalho está concluido
//            collector.complete();
//            return collector;
//        }
//
//        for (Member m : otherMembers) { // Caso existam acks
//            var id = this.messageId++;
//            var future = new CompletableFuture<Message>();
//            future.whenComplete((result, exception) -> {
//                if (exception == null) {
//                    collector.ackReceived();
//                }
//            });
//            pendingRequests.put(id, future);
//            network.send(m.address, new UpdateMembershipRequest(id, selfAddress, membership));
//        }
//        return collector;
//    }
//
//    private void handleResponse(Message message){
//        completePendingRequests(message);
//    }
//
//    private void completePendingRequests(Message message) {
//        var requestFuture = pendingRequests.get(message.messageId);
//        if (requestFuture != null) {
//            requestFuture.complete(message);
//        }
//    }
//
//    private void handlePossibleRejoin(JoinRequest joinRequest) {
//        // Checa se o node que quer entrar está na lista de nodes que falharam
//        if (membership.isFailed(joinRequest.from)) {
//            // Member rejoining
//            System.out.println(joinRequest.from + " rejoining the cluster. Removing it from failed list");
//            membership.removeFromFailedList(joinRequest.from);
//        }
//    }
//
//    // Node pede pelo versão mais atual do membership
//    private void handleHeartbeatMessage(HeartbeatMessage message) {
//        failureDetector.heartBeatReceived(message.from);
//        // Se for o seed node e o heartbeat enviado pelo node não veio com a versão mais atual do membership
//        if (isCoordinator() && (message.getMembershipVersion() < this.membership.getVersion())){
//            // Pega referência do node que enviou a mensagem e enviar um membership update
//            membership.getMember(message.from).ifPresent(member -> {
//                System.out.println("Membership version in "
//                        + selfAddress + "="
//                        + this.membership.version + " and in "
//                        + message.from + "="
//                        + message.getMembershipVersion());
//                System.out.println("Sending membership update from " + selfAddress + " to " + message.from);
//
//                sendMembershipUpdateTo(Arrays.asList(member));
//            });
//        }
//    }
//
//    private boolean isCoordinator() {
//        Member coordinator = membership.getCoordinator();
//        return coordinator.address.equals(selfAddress);
//    }
//
//    // Todos os clusters mandam heatbeats para os outros nodes.
//    // Todos os nodes possuem um failure detector para checar se estão faltando heatbeats.
//    // Apenas o coordenador marca os nodes como failed e comunica a atualização para os outros.
//    private void checkFailedMembers(List<Member> members) {
//        if (isCoordinator()) {
//            removeFailedMembers();
//        } else {
//            // Se o membro que falhou é o seed node,
//            // checar se o presente node é o proximo coordenador
//            claimLeadershipNeeded(members);
//        }
//    }
//
//    //
//    void removeFailedMembers() {
//        var failedMembers = checkAndGetFailedMembers(membership.getLiveMembers());
//        if (failedMembers.isEmpty())  {
//            return;
//        }
//        updateMembership(membership.failed(failedMembers));
//        sendMembershipUpdateTo(membership.getLiveMembers());
//    }
//
//    private void claimLeadershipNeeded(List<Member> members) {
//        var failedMembers = checkAndGetFailedMembers(members);
//        if (!failedMembers.isEmpty() && isOlderThenAll(failedMembers)) {
//            var newMembership = membership.failed(failedMembers);
//            updateMembership(newMembership);
//            sendMembershipUpdateTo(newMembership.getLiveMembers());
//        }
//    }
//
//    private boolean isOlderThenAll(List<Member> failedMembers) {
//        return failedMembers.stream().allMatch(m -> m.age < thisMember().age);
//    }
//
//    private List<Member> checkAndGetFailedMembers(List<Member> members) {
//        List<Member> failedMembers = members
//                .stream()
//                .filter(this::isFailed)
//                .map(member -> new Member(
//                            member.address,
//                            member.age,
//                            member.status))
//                .collect(Collectors.toList());
//
//        failedMembers.forEach(member -> {
//            failureDetector.remove(member.address);
//            System.out.println(selfAddress + " marking " + member.address + " as DOWN");
//        });
//        return failedMembers;
//    }
//
//    private boolean isFailed(Member member) {
//        return !member.address.equals(selfAddress)
//                && failureDetector.isMonitoring(member.address)
//                && !failureDetector.isAlive(member.address);
//    }
//
//    public void handleClientRequest(Request request) {
//        if (!hasMinimumRequiredSize()){
//            throw new Not EnoughMembersException("Requires minimum 3 members " + "to serve the request");
//        }
//    }
//
//    private boolean hasMinimumRequiredSize() {
//        return membership.getLiveMembers().size() > 3;
//    }
//
//    // O coordenador envia uma mensagem periodica checando se consegue se conectar com os nodes failed.
//    // Se uma conexão pode ser estabelecida, envia uma mensagem especial para fazer o split brain merge
//    private void searchOtherClusterGroups() {
//        if (membership.getFailedMembers().isEmpty()) {
//            return;
//        }
//        var allMembers = new ArrayList<Member>();
//        allMembers.addAll(membership.getLiveMembers());
//        allMembers.addAll(membership.getFailedMembers());
//        if (isCoordinator()) {
//            for (Member member : membership.getFailedMembers()) {
//                logger.info("Sending SplitBrainJoinRequest to "
//                        + member.address);
//                network.send(member.address,
//                        new SplitBrainJoinRequest(messageId++,
//                                this.selfAddress,
//                                membership.version,
//                                membership.getLiveMembers().size()));
//            }
//        }
//    }
//
//    private void handleSplitBrainJoinMessage(SplitBrainJoinRequest splitBrainJoinRequest) {
//        System.out.println(selfAddress + " Handling SplitBrainJoinRequest from "
//                + splitBrainJoinRequest.from);
//        if (!membership.isFailed(splitBrainJoinRequest.from)) {
//            return;
//        }
//        if (!isCoordinator()) {
//            return;
//        }
//        if(splitBrainJoinRequest.getMemberCount()
//                < membership.getLiveMembers().size()) {
//            //requesting node should join this cluster.
//            logger.info(selfAddress
//                    + " Requesting "
//                    + splitBrainJoinRequest.from
//                    + " to rejoin the cluster");
//            network.send(splitBrainJoinRequest.from,
//                    new SplitBrainMergeMessage(splitBrainJoinRequest.messageId,
//                            selfAddress));
//        } else {
//            //we need to join the other cluster
//            mergeWithOtherCluster(splitBrainJoinRequest.from);
//        }
//    }
//    private void mergeWithOtherCluster(InetAddressAndPort otherClusterCoordinator) {
//        askAllLiveMembersToMergeWith(otherClusterCoordinator);
//        //initiate merge on this node.
//        handleMerge(new MergeMessage(messageId++,
//                selfAddress, otherClusterCoordinator));
//    }
//    private void askAllLiveMembersToMergeWith(InetAddressAndPort mergeToAddress) {
//        List<Member> liveMembers = membership.getLiveMembers();
//        for (Member m : liveMembers) {
//            network.send(m.address,
//                    new MergeMessage(messageId++, selfAddress, mergeToAddress));
//        }
//    }
//
//    private void handleMerge(MergeMessage mergeMessage) {
//        logger.info(selfAddress + " Merging with " + mergeMessage.getMergeToAddress());
//        shutdown();
//        //join the cluster again through the other cluster's coordinator
//        taskScheduler.execute(() -> {
//            join(mergeMessage.getMergeToAddress());
//        });
//    }
//}
