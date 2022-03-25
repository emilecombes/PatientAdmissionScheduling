package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class Patient {
  private final String name, treatment, gender;
  private final int id, preferredCap;
  private final int admDate, disDate, stayLength, maxAdm;
  private final List<String> preferredProps, neededProps;
  private final List<Integer> neededCare;

  private boolean inPatient;
  private int admission, discharge, delay;
  private LinkedHashMap<Integer, Integer> assignedRooms;
  private List<Integer> roomCosts;
  private Set<Integer> feasibleRooms;

  public Patient(int id, String name, String gender, String treatment, int ad, int dd,
                 int ma, int cap, List<String> np, List<String> pp) {
    this.id = id;
    this.name = name;
    this.gender = gender;
    this.treatment = treatment;
    this.admDate = ad;
    this.disDate = dd;
    this.maxAdm = ma;
    this.stayLength = dd - ad;
    this.admission = ad;
    this.discharge = dd;
    this.delay = 0;
    this.preferredCap = cap;
    this.neededProps = np;
    this.preferredProps = pp;
    this.neededCare = new ArrayList<>();
    for(int i = 0; i < stayLength; i++) neededCare.add(1);
    assignedRooms = new LinkedHashMap<>();
  }

  // Getters & Setters
  public void setInitial(){
    inPatient = true;
  }

  public String getName() {
    return name;
  }

  public String getTreatment() {
    return treatment;
  }

  public String getGender() {
    return gender;
  }

  public int getId() {
    return id;
  }

  public int getPreferredCap() {
    return preferredCap;
  }

  public int getAdmDate() {
    return admDate;
  }

  public int getDisDate() {
    return disDate;
  }

  public int getStayLength() {
    return stayLength;
  }

  public int getMaxAdm() {
    return maxAdm;
  }

  public int getAdmission() {
    return admission;
  }

  public int getDischarge() {
    return discharge;
  }

  public int getDelay() {
    return delay;
  }

  public int getRoom(int day){
    return assignedRooms.get(day);
  }

  public boolean isInitial() { return inPatient; }

  // Public methods
  public void assignRoom(int room, int day){
    assignedRooms.put(day, room);
  }
}