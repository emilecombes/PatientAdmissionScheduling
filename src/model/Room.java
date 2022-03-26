package model;

import java.util.Set;

public class Room {
  private final int id, capacity;
  private final String name, genderPolicy, department;
  private final Set<String> features;

  public Room(int id, String name, int cap, String gender, String dep, Set<String> features) {
    this.id = id;
    this.name = name;
    this.capacity = cap;
    this.genderPolicy = gender;
    this.department = dep;
    this.features = features;
  }

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public String getDepartment() {
    return department;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean canHostGender(String gender) {
    return switch (genderPolicy) {
      case "Any", "SameGender" -> true;
      case "MaleOnly" -> gender.equals("Male");
      case "FemaleOnly" -> gender.equals("Female");
      default -> false;
    };
  }

  public boolean hasFeature(String f) {
    return features.contains(f);
  }

  public boolean hasAllFeatures(Set<String> f) {
    return features.containsAll(f);
  }
}