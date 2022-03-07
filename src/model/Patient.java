package model;

import java.util.Collections;
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

  public String getName() {
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

  public String getNeededSpecialism() {
    return neededSpecialism;
  }

  public boolean isMale(){
    return gender.equals("Male");
  }

  public int getRegistrationDate() {
    return registration;
  }

  public int getAdmissionDate() {
    return admission;
  }

  public int getActualAdmission() {
    return admission + getDelay();
  }

  public int getDischargeDate() {
    return discharge;
  }

  public int getActualDischarge() {
    return discharge + getDelay();
  }

  public int getDelay() {
    if (assignedRooms.size() == 0) return 0;
    int adm = admission;
    while (adm <= Collections.max(assignedRooms.keySet())) {
      if (assignedRooms.get(adm) != -1) return adm - admission;
      adm++;
    }
    return -1;
  }

  public int getRestingLOT(int day) {
    int start = Math.max(admission + getDelay(), day);
    int stop = Math.max(discharge + getDelay(), day);
    return stop - start;
  }

  public void assignRoom(int day, int room) {
    assignedRooms.put(day, room);
  }

  public void cancelRoom(int day){
    assignedRooms.remove(day);
  }

  public int getAssignedRoom(int day) {
    return (assignedRooms.get(day) == null) ? -1 : assignedRooms.get(day);
  }

  public int getLastRoom() {
    return assignedRooms.get(Collections.max(assignedRooms.keySet()));
  }

  public HashMap<Integer, Integer> getAssignedRooms() {
    return assignedRooms;
  }

  public int[] getInDays() {
    int[] inDays = new int[discharge - admission];
    for (int i = 0; i < inDays.length; i++) {
      inDays[i] = i + admission + getDelay();
    }
    return inDays;
  }

  public int getTransfers(){
    int currRoom = (getAssignedRoom(-1) != -1) ? getAssignedRoom(-1) : getAssignedRoom(admission);
    int t = 0;
    for(int i = admission; i < discharge; i++){
      if(assignedRooms.get(i) != currRoom) {
        t++;
        currRoom = assignedRooms.get(i);
      }
    }
    return t;
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
