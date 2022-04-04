package model;

import java.util.*;

public class Patient {
  private final String name, treatment, gender;
  private final int id, preferredCap;
  private final int originalAD, stayLength, maxAdm;
  private final Set<String> preferredProps, neededProps;
  private final List<Integer> neededCare;

  private final LinkedHashMap<Integer, Integer> assignedRooms;
  private final Map<String, Map<Integer, Integer>> specificRoomCosts;
  private final Map<Integer, Integer> roomCosts;
  private int initialRoom;
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
    this.admission = ad;
    this.discharge = dd;
    this.maxAdm = ma;
    this.stayLength = dd - ad;
    this.delay = 0;
    this.preferredCap = cap;
    this.neededProps = np;
    this.preferredProps = pp;
    this.neededCare = new ArrayList<>();
    for (int i = 0; i < stayLength; i++) neededCare.add(1);
    assignedRooms = new LinkedHashMap<>();
    roomCosts = new HashMap<>();
    specificRoomCosts = new HashMap<>();
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

  public int getCurrentRoomCost() {
    return getRoomCost(getLastRoom());
  }

  public int getTotalRoomCost() {
    return roomCosts.get(getLastRoom());
  }

  public int getPreferredCap() {
    return preferredCap;
  }

  public int getStayLength() {
    return stayLength;
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

  public int getMaxDelay() {
    return maxAdm - admission;
  }

  public int getMaxAdvance() {
    return admission - originalAD;
  }

  public int getRandomShift() {
    return -getMaxAdvance() + (int) (Math.random() * (getMaxAdvance() + getMaxDelay()));
  }

  public int getRoom(int day) {
    return (assignedRooms.get(day) == null) ? -1 : assignedRooms.get(day);
  }

  public int getInitialRoom() {
    return initialRoom;
  }

  public int getLastRoom() {
    return getRoom(discharge - 1);
  }

  public int getNewRandomFeasibleRoom() {
    if (feasibleRoomList.size() <= 1) return -1;
    int room;
    do room = feasibleRoomList.get((int) (Math.random() * feasibleRoomList.size()));
    while (room == getLastRoom());
    return room;
  }

  public String getStatus() {
    return inPatient ? "arrived" : "registered";
  }

  public void setInitialRoom(int room) {
    inPatient = true;
    initialRoom = room;
    for (int i = admission; i < discharge; i++)
      assignRoom(room, i);
  }

  public void setFeasibleRooms(Set<Integer> fr) {
    feasibleRooms = fr;
    feasibleRoomList = new ArrayList<>();
    feasibleRoomList.addAll(feasibleRooms);
  }

  public void setRoomCost(String type, int room, int cost) {
    specificRoomCosts.computeIfAbsent(type, k -> new HashMap<>());
    specificRoomCosts.get(type).put(room, cost);
  }

  public void calculateTotalRoomCost() {
    for (int room : feasibleRoomList) {
      int roomCost = 0;
      for (String type : specificRoomCosts.keySet())
        if (type.equals("transfer")) roomCost += specificRoomCosts.get(type).get(room);
        else roomCost += specificRoomCosts.get(type).get(room) * getStayLength();
      roomCosts.put(room, roomCost);
    }
  }

  public boolean isInitial() {
    return inPatient;
  }

  public boolean isAdmissibleOn(int day) {
    return day > originalAD && day < maxAdm;
  }

  public boolean hasFeasibleRoom(int room) {
    return feasibleRooms.contains(room);
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