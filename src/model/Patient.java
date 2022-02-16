package model;

import java.util.Date;
import java.util.Set;

public class Patient {
  private int age, preferredCapacity, variability;
  private String name, gender;
  private Date registrationDate, admissionDate, dischargeDate, maxAdmission;
  private String treatment;
  private Set<String> preferredRoomProperties;
  private Set<String> neededRoomProperties;

  private int penalty;
  private Room assignedRoom;

  public Patient(String n, int a, String g, String reg, String adm, String dis, int var,
                 String max, String cap, String t) {
    name = n;
    age = a;
    gender = g;
    variability = var;
    if(!cap.isEmpty()) preferredCapacity = Integer.parseInt(cap);
    else preferredCapacity = -1;
    treatment = t;
  }

  public void setPreferredRoomProperties(Set<String> preferredRoomProperties) {
    this.preferredRoomProperties = preferredRoomProperties;
  }

  public void setNeededRoomProperties(Set<String> neededRoomProperties) {
    this.neededRoomProperties = neededRoomProperties;
  }

  public String getGender(){
    return gender;
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
}
