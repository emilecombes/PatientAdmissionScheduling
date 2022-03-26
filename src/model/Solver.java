package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Solver {
  private final Map<String, Integer> penalties;
  private final PatientList patientList;
  private final RoomList roomList;
  private final Schedule schedule;
  private int cost;
  private Map<String, String> lastMove; // ex: move="CR", patient="2", room="4", savings="20"

  public Solver(PatientList pl, RoomList rl, Schedule s) {
    patientList = pl;
    roomList = rl;
    schedule = s;
    penalties = new HashMap<>();
  }

  public void setPenalty(String type, int value) {
    penalties.put(type, value);
  }

  public int getPenalty(String type) {
    return penalties.get(type);
  }

  public int getCost() {
    return cost;
  }

  public void init() {
    cost = 0;
    setFeasibleRooms();
    calculateRoomCosts();
    insertInitialPatients();
    assignRandomRooms();
  }

  public void setFeasibleRooms() {
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);

      Set<Integer> feasibleRooms = new HashSet<>();
      for (Room room : roomList.getRoomsForTreatment(patient.getTreatment())) {
        feasibleRooms.add(room.getId());
      }

      Set<Integer> infeasibleRooms = new HashSet<>();
      for (int r : feasibleRooms) {
        Room room = roomList.getRoom(r);
        if (!room.hasAllFeatures(patient.getNeededProperties())) infeasibleRooms.add(r);
      }

      feasibleRooms.removeAll(infeasibleRooms);
      patient.setFeasibleRooms(feasibleRooms);
    }
  }

  public void calculateRoomCosts() {
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      int roomCost = 0;
      for (int r : patient.getFeasibleRooms()) {
        Room room = roomList.getRoom(r);

        for (String property : patient.getPreferredProperties())
          if (!room.hasFeature(property))
            roomCost += getPenalty("roomProperty");
        if (patient.getPreferredCap() < room.getCapacity() && patient.getPreferredCap() != -1)
          roomCost += getPenalty("capacityPreference");
        if (!roomList.getMainRoomsForTreatment(patient.getTreatment()).contains(room))
          roomCost += getPenalty("speciality");
        if (!room.canHostGender(patient.getGender()))
          roomCost += getPenalty("gender");

        roomCost *= patient.getStayLength();
        if (patient.isInitial() && patient.getRoom(patient.getAdmission()) != r)
          roomCost += getPenalty("transfer");

        patient.setRoomCost(r, roomCost);
      }
    }
  }

  public void insertInitialPatients() {
    for (Patient p : patientList.getInitialPatients()) {
      int room = p.getRoom(p.getAdmission());
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      cost += p.getRoomCost(room);
    }
  }

  public void assignRandomRooms() {
    for (Patient p : patientList.getRegisteredPatients()) {
      int room = p.getRandomFeasibleRoom();
      for(int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      cost += p.getRoomCost(room);
    }
  }

  public void solve() {
  }
}
