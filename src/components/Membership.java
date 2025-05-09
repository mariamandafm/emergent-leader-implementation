package components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Membership {
    int seedAddress = 0;
    List<Member> liveMembers = new ArrayList<>();
    List<Member> failedMembers = new ArrayList<>();
    int version;

    public Membership(int version, List<Member> liveMembers) {
        this.version = version;
        this.liveMembers = new ArrayList<>(liveMembers); // Defensivo copy
        this.failedMembers = new ArrayList<>();
    }

    public Membership addNewMember (int address) {
        var newMembership = new ArrayList<>(liveMembers);
        int age = oldestMemberAge() + 1;
        newMembership.add(new Member(address, age));
        return new Membership(version + 1, newMembership);
    }

    public Membership removeMember(int address) {
        var newMembership = new ArrayList<>(liveMembers);
        newMembership.removeIf(member -> member.getPort() == address);
        return new Membership(version + 1, newMembership);
    }

    private int oldestMemberAge() {
        return liveMembers.stream().map(m -> m.getAge()).max(Integer::compare).orElse(0);
    }

    public Optional<Member> getSecondOldestMember() {
        List<Member> test = liveMembers.stream().sorted(Comparator.comparingInt(Member::getAge)).toList();
        return liveMembers.stream()
                .sorted(Comparator.comparingInt(Member::getAge))
                .skip(1) // Pula o mais velho
                .findFirst();
    }

    public List<Member> getLiveMembers() {
        return liveMembers;
    }

    public int getSeedAddress() {
        return seedAddress;
    }

    public List<Integer> getUpNodesAddress(){
        return liveMembers.stream().map(Member::getPort).collect(Collectors.toList());
    }

    public boolean contains(int address) {
        return liveMembers.stream().anyMatch(m -> m.getPort() == address);
    }

    public String getSerializedMembership(){
        String members =  liveMembers.stream()
                .map(member -> member.getPort() + ":" + member.getAge())
                .collect(Collectors.joining(","));

        return members + ";" + version;
    }

    public void deserializeMembershipAndUpdate(String data, String version){
        List<Member> members = new ArrayList<>();
        String[] membersAndAge = data.split(",");

        for (String member : membersAndAge){
            String[] parts = member.split(":");
            int port = Integer.parseInt(parts[0]);
            int age = Integer.parseInt(parts[1]);
            members.add(new Member(port, age));
        }
        this.liveMembers = members;
        this.version = Integer.parseInt(version);
    }

    public void setSeedAddress(int seedAddress) {
        this.seedAddress = seedAddress;
    }
}
