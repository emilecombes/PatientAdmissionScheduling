package model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class Patient {
  private final int age, preferredCapacity, variability;
  private final String name, gender, treatment, neededSpecialism;
  private final int registration, admission, discharge, maxAdmission;
  private Set<String> preferredRoomProperties;
  private Set<String> neededRoomProperties;
  private HashMap<Integer, Integer> assignedRooms;
  private int delay;

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
    maxAdmission = (max > 19) ? 19 : max;
    assignedRooms = new HashMap<>();
    neededSpecialism = spec;
    delay = 0;
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

  public int getVariability() {
    return variability;
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

  public boolean isMale() {
    return gender.equals("Male");
  }

  public boolean isUrgent() {
    return registration == admission && registration == maxAdmission;
  }

  public int getRegistrationDate() {
    return registration;
  }

  public int getAdmissionDate() {
    return admission;
  }

  public int getActualAdmission() {
    return admission + delay;
  }

  public int getMaxAdmission(){
    return maxAdmission;
  }

  public int getDischargeDate() {
    return discharge;
  }

  public int getActualDischarge() {
    return discharge + delay;
  }

  public int getDelay() {
    return delay;
  }

  public void addDelay(int d){
    delay += d;
  }

  public int getRestingLOT(int day) {
    int start = Math.max(admission + getDelay(), day);
    int stop = Math.max(discharge + getDelay(), day);
    return stop - start;
  }

  public void assignRoom(int day, int room) {
    assignedRooms.put(day, room);
  }

  public void cancelRoom(int day) {
    assignedRooms.remove(day);
  }

  public boolean isAdmittedOn(int start, int end) {
    for (int i = start; i <= end; i++)
      if (getAssignedRoom(i) != -1) return true;
    return false;
  }

  public int getAssignedRoom(int day) {
    return (assignedRooms.get(day) == null) ? -1 : assignedRooms.get(day);
  }

  public int getLastRoom() {
    return getAssignedRoom(discharge + delay - 1);
  }

  public HashMap<Integer, Integer> getAssignedRooms() {
    return assignedRooms;
  }

  public int getTransfers() {
    int currRoom = (getAssignedRoom(-1) != -1) ? getAssignedRoom(-1) :
        getAssignedRoom(admission+delay);
    int t = 0;
    for (int i = admission+delay; i < discharge+delay; i++) {
      if (assignedRooms.get(i) != currRoom) {
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
        ", registration=" + registration +
        ", admission=" + admission +
        ", maxAdmission=" + maxAdmission +
        ", discharge=" + discharge +
        ", delay=" + delay +
        '}';
  }
}
