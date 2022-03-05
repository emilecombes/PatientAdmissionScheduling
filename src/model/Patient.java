package model;

import java.util.HashMap;
import java.util.Set;

public class Patient {
  private final int age, preferredCapacity, variability;
  private final String name, gender, treatment, neededSpecialism;
  private final int registration, admission, discharge, maxAdmission;
  private Set<String> preferredRoomProperties;
  private Set<String> neededRoomProperties;
  private HashMap<Integer, Integer> assignedRooms;

  public Patient(String n, int a, String g, int reg, int adm, int dis, int var, int max, int cap,
                 String t, String spec) {
    name = n;
    age = a;
    gender = g;
    variability = var;
    preferredCapacity = cap;
    treatment = t;
    registration = reg;
    admission = adm;
    discharge = dis;
    maxAdmission = max;
    assignedRooms = new HashMap<>();
    neededSpecialism = spec;
  }

  public void setPreferredRoomProperties(Set<String> preferredRoomProperties) {
    this.preferredRoomProperties = preferredRoomProperties;
  }

  public void setNeededRoomProperties(Set<String> neededRoomProperties) {
    this.neededRoomProperties = neededRoomProperties;
  }

  public String getGender() {
    return gender;
  }

  public String getName(){
    return name;
  }

  public int getPreferredCapacity() {
    return preferredCapacity;
  }

  public Set<String> getPreferredProperties() {
    return preferredRoomProperties;
  }

  public Set<String> getNeededProperties() {
    return neededRoomProperties;
  }

  public String getTreatment() {
    return treatment;
  }

  public String getNeededSpecialism(){
    return neededSpecialism;
  }

  public int getRegistrationDate(){
    return registration;
  }

  public int getAdmissionDate() {
    return admission;
  }

  public int getDischargeDate() {
    return discharge;
  }

  public int getDelay(){
    return 0;
  }

  public void assignRoom(int day, int room){
    assignedRooms.put(day, room);
  }

  public int getAssignedRoom(int day){
    return (assignedRooms.get(day) == null) ? -1 : assignedRooms.get(day);
  }

  public HashMap<Integer, Integer> getAssignedRooms(){
    return assignedRooms;
  }

  @Override
  public String toString() {
    return "Patient{" +
        "name=" + name +
        ", preferredCapacity=" + preferredCapacity +
        ", gender='" + gender + '\'' +
        ", treatment='" + treatment + '\'' +
        ", preferredRoomProperties=" + preferredRoomProperties +
        ", neededRoomProperties=" + neededRoomProperties +
        '}';
  }
}
