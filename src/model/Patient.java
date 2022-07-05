package model;

import util.DateConverter;

import java.util.*;

public class Patient {
  private final String name, treatment, gender;
  private final int id, preferredCap;
  private final int originalAD, stayLength, maxAdm;
  private final Set<String> preferredProps, neededProps;
  private final List<Integer> neededCare;
  private final int totalNeededCare;

  private final LinkedHashMap<Integer, Integer> assignedRooms;
  private final Map<String, Map<Integer, Integer>> specificRoomCosts;
  private final Map<Integer, Integer> roomCosts;
  private int initialRoom, bed;
  private int admission, discharge, delay;
  private boolean inPatient;
  private Set<Integer> feasibleRooms;
  private List<Integer> feasibleRoomList;

  public Patient(int id, String name, String gender, String treatment, int ad, int dd, int ma,
                 int cap, Set<String> np, Set<String> pp) {
    this.id = id;
    this.name = name;
    this.gender = gender;
    this.treatment = treatment;
    this.originalAD = ad;
    this.admission = ad;
    this.discharge = dd;
    this.stayLength = dd - ad;
    this.maxAdm = (ma == -1) ? DateConverter.getTotalHorizon() - stayLength : ma;
    this.delay = 0;
    this.preferredCap = cap;
    this.neededProps = np;
    this.preferredProps = pp;
    this.neededCare = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < stayLength; i++) {
      neededCare.add((int) (Math.random() * 100));
      total += neededCare.get(i);
    }
    totalNeededCare = total;
    assignedRooms = new LinkedHashMap<>();
    roomCosts = new HashMap<>();
    specificRoomCosts = new HashMap<>();
    bed = -1;
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

  public int getSpecificRoomCost(int room, String type) {
    return specificRoomCosts.get(type).get(room);
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

  public int getNeededCare(int day) {
    return neededCare.get(day - admission);
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

  public int getOriginalAD() {
    return originalAD;
  }

  public int getMaxAdm() {
    return maxAdm;
  }

  public Set<Integer> getAdmittedDays() {
    return assignedRooms.keySet();
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
    return assignedRooms.getOrDefault(day, -1);
  }

  public int getBed() {
    return bed;
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

  public Map<String, String> getInfo() {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("Id", String.valueOf(id));
    info.put("Name", name);
    info.put("Gender", gender);
    info.put("Treatment", treatment);
    info.put("Admission", DateConverter.getDateString(admission));
    info.put("Discharge", DateConverter.getDateString(discharge));
    info.put("Delay", String.valueOf(delay));
    info.put("Department", DepartmentList.getRoom(getLastRoom()).getDepartment());
    info.put("MainSpecialism",
        specificRoomCosts.get("speciality").get(getLastRoom()) == 0 ? "True" : "False");
    info.put("Room", DepartmentList.getRoomName(getLastRoom()));
    info.put("RoomCost", String.valueOf(roomCosts.get(getLastRoom())));
    info.put("Bed", String.valueOf(bed));
    return info;
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

  public void setBed(int b) {
    bed = b;
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
    return day >= originalAD && day <= maxAdm;
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