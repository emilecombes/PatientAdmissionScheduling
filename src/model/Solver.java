package model;

import java.util.HashMap;
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

  public int getPenalty(String type) {
    return penalties.get(type);
  }

  public void setPenalty(String type, int value) {
    penalties.put(type, value);
  }

  public void init(){
    // TODO calculate PR costs, Set feasible rooms, insert initial patients, ...
  }

  public void setFeasibleRooms() {
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      Set<Room> feasibleDepartments = roomList.getRoomsForTreatment(patient.getTreatment());
      for (int j = 0; j < roomList.getNumberOfRooms(); j++) {
        Room room = roomList.getRoom(j);
      }
    }
  }

  public void calculateRoomCosts() {
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      for (int j = 0; j < roomList.getNumberOfRooms(); j++) {

      }
    }
  }

  public void insertInitialPatients() {
    for (Patient p : patientList.getInitialPatients()) {
      int room = p.getRoom(p.getAdmission());
      for (int i = p.getAdmission(); i < p.getDischarge(); i++) {
        schedule.assignPatient(p, i);
      }
    }
  }

  public void assignPatients() {
  }

  public void solve() {
  }
}
