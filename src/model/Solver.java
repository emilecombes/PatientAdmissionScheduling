package model;

import util.DateConverter;

import java.util.*;

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
    int transfer = 0;
    int totalDelay = 0;
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      if (patient.isInitial())
        if (patient.getInitialRoom() != patient.getLastRoom())
          transfer += getPenalty("transfer");
      roomCosts += patient.getTotalRoomCost();
      totalDelay += patient.getDelay();
    }
    roomCosts -= transfer;
    int gender = schedule.getDynamicGenderViolations() * getPenalty("gender");
    costs.put("capacity_violations", schedule.getCapacityViolations());
    costs.put("transfer", transfer);
    costs.put("patient_room", roomCosts);
    costs.put("delay", totalDelay);
    costs.put("gender", gender);
    costs.put("total", transfer + roomCosts + totalDelay + gender);

    return costs;
  }

  public HashMap<String, String> getPlanningHorizon() {
    HashMap<String, String> horizon = new HashMap<>();
    horizon.put("start_day", DateConverter.getDateString(0));
    horizon.put("num_days", String.valueOf(DateConverter.getNumDays()));
    horizon.put("current_day", DateConverter.getDateString(0));
    return horizon;
  }

  public boolean verifyCost() {
    return cost == getCosts().get("total") + getCosts().get("capacity_violations") * getPenalty(
        "capacityViolation");
  }

  public void printCosts() {
    System.out.println("Total cost: " + cost +
        "\nCapacity violations: " + schedule.getCapacityViolations() +
        "\nSoft cost: " +
        (cost - (getPenalty("capacityViolation") * schedule.getCapacityViolations())));
  }

  public void init() {
    cost = 0;
    setFeasibleRooms();
    calculateRoomCosts();
    insertInitialPatients();
    assignRandomRooms();
    cost += schedule.getDynamicGenderViolations() * getPenalty("gender");
    cost += schedule.getCapacityViolations() * getPenalty("capacityViolation");
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
        if (schedule.getDynamicGenderViolations(room, day) > 0
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
        if (schedule.getDynamicGenderViolations(room, day) == 0
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
      int room = p.getNewRandomFeasibleRoom();
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      cost += p.getRoomCost(room);
    }
  }

  public void solve() {
    int loops = 1000000;
    while (loops > 0) {
      int savings = executeNewMove();
      if (savings < 0) undoLastMove();
      else cost -= savings;
      if (loops % 100000 == 1) printCosts();
      loops--;
    }
  }

  public void undoLastMove() {

  }

  public int executeNewMove() {
    Map<String, Integer> move = generateMove();
    switch (move.get("type")) {
      case 0 -> executeChangeRoom(move.get("patient"), move.get("new_room"));
      case 1 -> executeSwapRoom(move.get("first_patient"), move.get("second_patient"));
      case 2 -> executeShiftAdmission(move.get("patient"), move.get("shift"));
      case 3 -> executeSwapAdmission(move.get("first_patient"), move.get("second_patient"));
    }
    return 0;
  }

  public void executeChangeRoom(int pat, int room) {
    Patient patient = patientList.getPatient(pat);
    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      schedule.cancelPatient(patient, day);
      schedule.assignPatient(patient, room, day);
    }
  }

  public void executeSwapRoom(int fPat, int sPat) {
  }

  public void executeShiftAdmission(int pat, int shift) {
  }

  public void executeSwapAdmission(int fPat, int sPat) {
  }

  public Map<String, Integer> generateMove() {
    int type = (int) (Math.random() * 4);
    return switch (type) {
      case 0 -> generateChangeRoom();
      case 1 -> generateSwapRoom();
      case 2 -> generateShiftAdmission();
      case 3 -> generateSwapAdmission();
      default -> null;
    };
  }


  public Map<String, Integer> generateChangeRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = patientList.getRandomPatient();
    int room = patient.getNewRandomFeasibleRoom();
    move.put("type", 1);
    move.put("patient", patient.getId());
    move.put("original_room", patient.getLastRoom());
    move.put("new_room", room);
    return move;
  }

  public Map<String, Integer> generateSwapRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapRoomPatient(firstPatient);
    move.put("type", 2);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("second_room", secondPatient.getLastRoom());
    return move;
  }

  public Map<String, Integer> generateShiftAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = patientList.getRandomShiftPatient();
    int shift = patient.getRandomShift();
    move.put("type", 3);
    move.put("patient", patient.getId());
    move.put("shift", shift);
    return move;
  }

  public Map<String, Integer> generateSwapAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapAdmissionPatient(firstPatient);
    move.put("type", 4);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    return move;
  }


}
