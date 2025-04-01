//import java.util.ArrayList;
//import java.util.List;
//
//public class Membership {
//    // ...
//    List<Member> liveMembers = new ArrayList<>();
//    List<Member> failedMembers = new ArrayList<>();
//    int version;
//
////    public Membership(int i, ArrayList<Member> newMembership, List<Member> failedMembers) {
////    }
//
//
//    public Membership addNewMember (InetAddressAndPort address) {
//        var newMembership = new ArrayList<>(liveMembers);
//        int age = yougestMemberAge() + 1;
//        newMembership.add(new Member(address, age, MemberStatus.JOINED));
//        return new Membership(version + 1, newMembership, failedMembers);
//    }
//
//    private int yougestMemberAge() {
//        return liveMembers.stream().map(m -> m.age).max(Integer::compare).orElse(0);
//    }
//
//    public boolean isFailed(InetAddressAndPort address) {
//        return failedMembers.stream().anyMatch(m -> m.address.equals(address));
//    }
//}
