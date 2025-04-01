//public class ClusterNode {
//    // ..
//    MembershipService membershipService;
//
//    // Quando iniciado, um cluster node tenta contato com o node seed para se juntar ao cluster
//    public void start(Config config) {
//        this.membershipService = new MembershipService(config.getListenAddress());
//        membershipService.join(config.getSeedAddress());
//    }
//}