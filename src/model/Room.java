package model;

import java.util.Set;

public class Room {
  private int capacity;
  private String name, genderPolicy;
  private Set<String> roomFeatures;
  private Department department;

  public Room(String n, int cap, String gender){
    capacity = cap;
    name = n;
    genderPolicy = gender;
  }

  public void setDepartment(Department department) {
    this.department = department;
  }

  public void setRoomFeatures(Set<String> roomFeatures) {
    this.roomFeatures = roomFeatures;
  }
}
