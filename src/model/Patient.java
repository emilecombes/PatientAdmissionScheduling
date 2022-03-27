package model;

import java.util.*;

public class Patient {
  private final String name, treatment, gender;
  private final int id, preferredCap;
  private final int originalAD, originalDD, stayLength, maxAdm;
  private final Set<String> preferredProps, neededProps;
  private final List<Integer> neededCare;

  private final LinkedHashMap<Integer, Integer> assignedRooms;
  private final Map<Integer, Integer> roomCosts;
  private int admission, discharge, delay;
  private boolean inPatient;
  private Set<Integer> feasibleRooms;
  private List<Integer> feasibleRoomList;

  public Patient(int id, String name, String gender, String treatment, int ad, int dd,
                 int ma, int cap, Set<String> np, Set<String> pp) {
    this.id = id;
    this.name = name;
    this.gender = gender;
    this.treatment = treatment;
    this.originalAD = ad;
    this.originalDD = dd;
    this.maxAdm = ma;
    this.stayLength = dd - ad;
    this.admission = ad;
    this.discharge = dd;
    this.delay = 0;
    this.preferredCap = cap;
    this.neededProps = np;
    this.preferredProps = pp;
    this.neededCare = new ArrayList<>();
    for (int i = 0; i < stayLength; i++) neededCare.add(1);
    assignedRooms = new LinkedHashMap<>();
    roomCosts = new HashMap<>();
  }

  public void setInitial() {
    inPatient = true;
  }

  public void setFeasibleRooms(Set<Integer> fr) {
    feasibleRooms = fr;
    feasibleRoomList = new ArrayList<>();
    feasibleRoomList.addAll(feasibleRooms);
  }

  public void setRoomCost(int room, int cost) {
    roomCosts.put(room, cost);
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

  public Set<String> getNeededProperties() {
    return neededProps;
  }

  public Set<String> getPreferredProperties() {
    return preferredProps;
  }

  public Set<Integer> getFeasibleRooms() {
    return feasibleRooms;
  }

  public int getRoomCost(int room) {
    return roomCosts.get(room);
  }

  public int getPreferredCap() {
    return preferredCap;
  }

  public int getNeededCare(int day) {
    return neededCare.get(day - admission);
  }

  public int getOriginalAD() {
    return originalAD;
  }

  public int getOriginalDD() {
    return originalDD;
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

  public int getRoom(int day) {
    return assignedRooms.get(day);
  }

  public int getRandomFeasibleRoom() {
    return feasibleRoomList.get((int) (Math.random() * feasibleRoomList.size()));
  }

  public String getStatus() {
    return inPatient ? "arrived" : "registered";
  }

  public boolean isInitial() {
    return inPatient;
  }

  public void assignRoom(int room, int day) {
    assignedRooms.put(day, room);
  }

  public void cancelRoom(int day) {
    assignedRooms.remove(day);
  }

  public void shiftAdmission(int days) {
    admission += days;
    discharge += days;
    delay += days;
  }

}