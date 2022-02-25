package model;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

public class Patient {
  private final int age, preferredCapacity, variability;
  private final String name, gender, treatment;
  private GregorianCalendar registrationDate, admissionDate, dischargeDate, maxAdmission;
  private String room;
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
    registrationDate = readDate(reg);
    admissionDate = readDate(adm);
    dischargeDate = readDate(dis);
  }

  public GregorianCalendar readDate(String d){
    String[] date = d.split("-");
    return new GregorianCalendar(
        Integer.parseInt(date[0]),
        Integer.parseInt(date[1]),
        Integer.parseInt(date[2])
    );
  }

  public void setRoom(String r){
    room = r;
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

  public String getRoom(){
    return room;
  }

  public String getName(){
    return name;
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
