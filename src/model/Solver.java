package model;

import util.DateConverter;

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
  // private Map<String, String> lastMove; // ex: move="CR", patient="2", room="4", savings="20"

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

  public PatientList getPatientList() {
    return patientList;
  }

  public RoomList getRoomList() {
    return roomList;
  }

  public HashMap<String, Integer> getCosts() {
    HashMap<String, Integer> costs = new HashMap<>();

    int roomCosts = 0;
    int totalDelay = 0;
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      roomCosts += patient.getTotalRoomCost();
      totalDelay += patient.getDelay();
    }
    costs.put("patient_room", roomCosts);
    costs.put("delay", totalDelay);
    costs.put("gender", schedule.getDynamicGenderViolations() * getPenalty("gender"));

    return costs;
  }

  public int getCost() {
    return cost;
  }

  public HashMap<String, String> getPlanningHorizon() {
    HashMap<String, String> horizon = new HashMap<>();
    horizon.put("start_day", DateConverter.getDateString(0));
    horizon.put("num_days", String.valueOf(DateConverter.getNumDays()));
    horizon.put("current_day", DateConverter.getDateString(0));
    return horizon;
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
      for (int r : patient.getFeasibleRooms()) {
        int roomCost = 0;
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
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      cost += p.getRoomCost(room);
    }
  }

  public void solve() {
  }
}
