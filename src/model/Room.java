package model;

import java.awt.color.ICC_ColorSpace;
import java.util.Set;

public class Room {
  private final int capacity;
  private final String name;
  private final String genderPolicy;
  private Set<String> roomFeatures;
  private Department department;
  private String currentPolicy;

  public Room(String n, int cap, String gender) {
    capacity = cap;
    name = n;
    genderPolicy = gender;
  }

  public void setDepartment(Department department) {
    this.department = department;
    department.addRoom(this);
  }

  public void setRoomFeatures(Set<String> roomFeatures) {
    this.roomFeatures = roomFeatures;
  }

  public void setCurrentPolicy(String pol){
    currentPolicy = pol;
  }

  public String getCurrentPolicy(){
    return currentPolicy;
  }

  public String getName(){
    return name;
  }

  public int getCapacity(){
    return capacity;
  }

  public String getGenderPolicy(){
    return genderPolicy;
  }

  public int getGenderPenalty(String g) {
    if (genderPolicy.equals("Any") ||
        genderPolicy.equals("MaleOnly") && g.equals("Male") ||
        genderPolicy.equals("FemaleOnly") && g.equals("Female") ||
        genderPolicy.equals("SameGender") && currentPolicy.equals(g))
      return 0;
    else if(genderPolicy.equals("SameGender"))
      return 1;
    else
      return -1;
  }

  public int getCapacityPenalty(int preference) {
    return (preference == capacity) ? 0 : 1;
  }

  public int getPreferredPropertiesPenalty(Set<String> properties) {
    int penalty = 0;
    for (String p : properties)
      if (!roomFeatures.contains(p))
        penalty++;
    return penalty;
  }

  public int getNeededPropertiesPenalty(Set<String> properties) {
    for (String p : properties)
      if (!roomFeatures.contains(p))
        return -1;
    return 0;
  }

  public int getTreatmentPenalty(String specialism){
    if(department.hasMainSpecialism(specialism)) return 0;
    if(department.hasAuxSpecialism(specialism)) return 1;
    return -1;
  }

  public boolean canHost(Patient patient){
    return getGenderPenalty(patient.getGender()) != -1
        && getNeededPropertiesPenalty(patient.getNeededProperties()) != -1
        && getTreatmentPenalty(patient.getNeededSpecialism()) != -1;
  }

  @Override
  public String toString() {
    return "Room{" +
        "capacity=" + capacity +
        ", name='" + name + '\'' +
        ", genderPolicy='" + genderPolicy + '\'' +
        ", roomFeatures=" + roomFeatures +
        ", department=" + department +
        '}';
  }
}
