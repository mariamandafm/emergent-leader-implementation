package udp;

import java.util.ArrayList;
import java.util.List;

public class Membership {
    int seedAddress = 0;
    List<Member> liveMembers = new ArrayList<>();
    List<Member> failedMembers = new ArrayList<>();
    int version;

    public <T> Membership(int version, List<Member> liveMembers) {
    }

    public Membership addNewMember (int address) {
        var newMembership = new ArrayList<>(liveMembers);
        int age = yougestMemberAge() + 1;
        newMembership.add(new Member(address, age));
        return new Membership(version + 1, newMembership);
    }

    private int yougestMemberAge() {
        return liveMembers.stream().map(m -> m.getAge()).max(Integer::compare).orElse(0);
    }

    public List<Member> getLiveMembers() {
        return liveMembers;
    }

    public int getSeedAddress() {
        return seedAddress;
    }
}
