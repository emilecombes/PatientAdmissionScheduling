package model;

import util.DateConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Solver {
  private final Map<String, Integer> penalties;
  private final PatientList patientList;
  private final DepartmentList departmentList;
  private final Schedule schedule;
  private int cost;

  public Solver(PatientList pl, DepartmentList dl, Schedule s) {
    patientList = pl;
    departmentList = dl;
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

  public int getCost() {
    return cost;
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
      for (Department department : departmentList.getDepartmentsForTreatment(
          patient.getTreatment()))
        feasibleRooms.addAll(department.getRoomIndices());

      Set<Integer> infeasibleRooms = new HashSet<>();
      for (int r : feasibleRooms) {
        Room room = departmentList.getRoom(r);
        if (!room.hasAllFeatures(patient.getNeededProperties()))
          infeasibleRooms.add(r);
      }

      feasibleRooms.removeAll(infeasibleRooms);
      patient.setFeasibleRooms(feasibleRooms);
    }
  }

  public void calculateRoomCosts() {
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      for (int r : patient.getFeasibleRooms()) {
        Room room = departmentList.getRoom(r);

        int propertyCost = 0;
        for (String property : patient.getPreferredProperties())
          if (!room.hasFeature(property))
            propertyCost += getPenalty("roomProperty");
        patient.setRoomCost("roomProperty", r, propertyCost);

        int capacityCost;
        if (patient.getPreferredCap() < room.getCapacity() && patient.getPreferredCap() != -1)
          capacityCost = getPenalty("capacityPreference");
        else capacityCost = 0;
        patient.setRoomCost("capacityPreference", r, capacityCost);

        int specialityCost;
        String specialism = departmentList.getNeededSpecialism(patient.getTreatment());
        Department department = departmentList.getDepartment(room.getDepartment());
        if (!department.hasMainSpecialism(specialism)) specialityCost = getPenalty("speciality");
        else specialityCost = 0;
        patient.setRoomCost("speciality", r, specialityCost);

        int genderCost;
        if (!room.canHostGender(patient.getGender())) genderCost = getPenalty("gender");
        else genderCost = 0;
        patient.setRoomCost("gender", r, genderCost);

        int transferCost;
        if (patient.isInitial() && patient.getRoom(patient.getAdmission()) != r)
          transferCost = getPenalty("transfer");
        else transferCost = 0;
        patient.setRoomCost("transfer", r, transferCost);
      }

      patient.calculateTotalRoomCost();
    }
  }

  public int getCancellationSavings(Patient pat) {
    int savings = 0;
    int room = pat.getLastRoom();
    savings += pat.getRoomCost(room);
    for (int day = pat.getAdmission(); day < pat.getDischarge(); day++) {
      if (schedule.getCapacityViolations(room, day) > 0)
        savings += getPenalty("capacityViolation");
      if (departmentList.getRoom(room).hasGenderPolicy("SameGender"))
        if (schedule.getGenderViolations(room, day) > 0
            && schedule.getGenderCount(room, day, pat.getGender()) == 1)
          savings += getPenalty("gender");
    }
    return savings;
  }

  public int getAssignmentCost(Patient pat, int room) {
    int cost = pat.getRoomCost(room);
    for (int day = pat.getAdmission(); day < pat.getDischarge(); day++) {
      if (schedule.getCapacityMargin(room, day) == 0)
        cost += getPenalty("capacityViolation");
      if (departmentList.getRoom(room).hasGenderPolicy("SameGender")) {
        String otherGender = (pat.getGender().equals("Male")) ? "Female" : "Male";
        if (schedule.getGenderViolations(room, day) == 0
            && schedule.getGenderCount(room, day, otherGender) > 0)
          cost += getPenalty("gender");
      }
    }
    return cost;
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
    Map<String, Integer> move = generateNewMove();
  }

  public void executeMove(Map<String, Integer> move) {
    System.out.println("TODO");
    executeChangeRoom(move.get("patient"), move.get("new_room"));
  }

  public void executeChangeRoom(int pat, int room) {
  }

  public Map<String, Integer> generateNewMove() {
    return generateChangeRoom();
  }

  public Map<String, Integer> generateChangeRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = patientList.getRandomPatient();
    int room = patient.getRandomFeasibleRoom();
    move.put("type", 1);
    move.put("patient", patient.getId());
    move.put("original_room", patient.getLastRoom());
    move.put("new_room", room);
    move.put("savings", getCancellationSavings(patient) - getAssignmentCost(patient, room));
    return move;
  }
}
