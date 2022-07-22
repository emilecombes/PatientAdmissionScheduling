package model;

import java.util.Set;

public class Room {
  private final int id, capacity;
  private final String name, genderPolicy, department;
  private final Set<String> features;
  private int departmentId;

  public Room(int id, String name, int cap, String gender, String dep, Set<String> features) {
    this.id = id;
    this.name = name;
    this.capacity = cap;
    this.genderPolicy = gender;
    this.department = dep;
    this.features = features;
  }

  public void setDepartmentId(int id) {
    departmentId = id;
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

  public int getDepartmentId() {
    return departmentId;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean canHostGender(String gender) {
    if (genderPolicy.equals("Any") || genderPolicy.equals("SameGender")) return true;
    if (genderPolicy.equals("MaleOnly")) return gender.equals("Male");
    if (genderPolicy.equals("FemaleOnly")) return gender.equals("Female");
    return false;
  }

  public boolean hasGenderPolicy(String pol) {
    return genderPolicy.equals(pol);
  }

  public boolean hasFeature(String f) {
    return features.contains(f);
  }

  public boolean hasAllFeatures(Set<String> f) {
    return features.containsAll(f);
  }
}