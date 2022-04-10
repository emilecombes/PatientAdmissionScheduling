package model;

import util.DateConverter;

import java.util.*;

public class Solver {
  private final Map<String, Integer> penalties;
  private final PatientList patientList;
  private final DepartmentList departmentList;
  private final Schedule schedule;
  private int patientCost, loadCost;
  private final List<Map<String, Integer>> generatedMoves;
  private Map<String, Integer> lastMove;

  public Solver(PatientList pl, DepartmentList dl, Schedule s) {
    patientList = pl;
    departmentList = dl;
    schedule = s;
    penalties = new HashMap<>();
    generatedMoves = new ArrayList<>();
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

  public HashMap<String, Integer> getCostInfo() {
    HashMap<String, Integer> costs = new HashMap<>();
    int roomCosts = 0;
    int transfer = 0;
    int totalDelay = 0;
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      if (patient.isInitial())
        if (patient.getInitialRoom() != patient.getLastRoom()) transfer += getPenalty("transfer");
      roomCosts += patient.getTotalRoomCost();
      totalDelay += patient.getDelay() * getPenalty("delay");
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

  public HashMap<String, String> getPlanningHorizonInfo() {
    HashMap<String, String> horizon = new HashMap<>();
    horizon.put("start_day", DateConverter.getDateString(0));
    horizon.put("num_days", String.valueOf(DateConverter.getNumDays()));
    horizon.put("current_day", DateConverter.getDateString(0));
    return horizon;
  }

  public List<String> getMoveInfo() {
    List<String> moveInfo = new ArrayList<>();
    String[] columns =
        {"id", "type", "accepted", "first_patient", "second_patient", "first_room", "second_room",
            "first_department", "second_department", "first_shift", "second_shift",
            "patient_savings", "load_savings", "patient_cost", "load_cost"};

    StringBuilder header = new StringBuilder();
    for (String key : columns) header.append(key).append(',');
    header.setLength(header.length() - 1);
    header.append('\n');
    moveInfo.add(header.toString());

    for (int i = 0; i < generatedMoves.size(); i++) {
      Map<String, Integer> move = generatedMoves.get(i);
      StringBuilder info = new StringBuilder(String.valueOf(i) + ',');
      for (String key : columns)
        if (!key.equals("id")) info.append(move.getOrDefault(key, -1)).append(',');
      info.setLength(info.length() - 1);
      info.append('\n');
      moveInfo.add(info.toString());
    }
    return moveInfo;
  }

  public void printCosts() {
    System.out.println("\nTotal cost: \t\t\t" + patientCost + "\nCapacity violations: \t" +
        schedule.getCapacityViolations() + "\nPatient cost: \t\t\t" +
        (patientCost - (getPenalty("capacity_violation") * schedule.getCapacityViolations())) +
        "\nLoad cost: \t\t\t\t" + loadCost);
  }


  // Initialize Schedule & Solver
  public void init() {
    patientCost = 0;
    setFeasibleRooms();
    calculateRoomCosts();
    insertInitialPatients();
    assignRandomRooms();
    addDynamicPatientCosts();
    calculateLoadCost();
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
        Room room = departmentList.getRoom(r);

        int propertyCost = 0;
        for (String property : patient.getPreferredProperties())
          if (!room.hasFeature(property)) propertyCost += getPenalty("room_property");
        patient.setRoomCost("room_property", r, propertyCost);

        int capacityCost;
        if (patient.getPreferredCap() < room.getCapacity() && patient.getPreferredCap() != -1)
          capacityCost = getPenalty("capacity_preference");
        else capacityCost = 0;
        patient.setRoomCost("capacity_preference", r, capacityCost);

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

  public void insertInitialPatients() {
    for (Patient p : patientList.getInitialPatients()) {
      int room = p.getRoom(p.getAdmission());
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      patientCost += p.getRoomCost(room);
    }
  }

  public void assignRandomRooms() {
    for (Patient p : patientList.getRegisteredPatients()) {
      int room = p.getNewRandomFeasibleRoom();
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      patientCost += p.getRoomCost(room);
    }
  }

  public void addDynamicPatientCosts() {
    patientCost += schedule.getDynamicGenderViolations() * getPenalty("gender");
    patientCost += schedule.getCapacityViolations() * getPenalty("capacity_violation");
  }

  public void calculateLoadCost() {
    for (int i = 0; i < DateConverter.getTotalHorizon(); i++)
      schedule.calculateDailyLoadCost(i);
    for (int i = 0; i < departmentList.getNumberOfDepartments(); i++)
      schedule.calculateDepartmentLoadCost(i);
    loadCost = schedule.getTotalDailyLoadCost() + schedule.getTotalDepartmentLoadCost();
  }


  // Start Search Procedure
  public void solve(String objective) {
    if (!objective.equals("patient_savings") && !objective.equals("load_savings")) {
      System.err.println("Invalid objective");
      return;
    }
    for (int i = 0; i < 10000; i++) {
      executeNewMove();
      int savings = lastMove.get(objective);
      if (savings > 0) {
        lastMove.put("accepted", 1);
        if (objective.equals("patient_savings")) patientCost -= savings;
        else loadCost -= savings;
        if (schedule.checkLoadMatrix(loadCost)) {
          loadCost = schedule.getTotalDailyLoadCost() + schedule.getTotalDepartmentLoadCost();
        }
      } else {
        lastMove.put("accepted", 0);
        undoLastMove();
        if (schedule.checkLoadMatrix(loadCost)) {
          System.out.println("Last move: " + lastMove.get("load_savings") + ", " + lastMove.get(
              "type"));
          loadCost = schedule.getTotalDailyLoadCost() + schedule.getTotalDepartmentLoadCost();
        }
      }
      generatedMoves.add(lastMove);
      if (i % 1000 == 0) printCosts();
    }
  }


  // Move generation
  public Map<String, Integer> generateMove() {
    int random = (int) (Math.random() * 100);
    int type;
    if (random < 50) type = 0;
    else if (random < 80) type = 1;
    else if (random < 85) type = 2;
    else type = 3;
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
    move.put("type", 0);
    move.put("first_patient", patient.getId());
    move.put("first_room", patient.getLastRoom());
    move.put("first_department", departmentList.getRoom(patient.getLastRoom()).getDepartmentId());
    move.put("second_room", room);
    move.put("second_department", departmentList.getRoom(room).getDepartmentId());
    return move;
  }

  public Map<String, Integer> generateSwapRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapRoomPatient(firstPatient);
    if (secondPatient == null) return generateMove();
    move.put("type", 1);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("first_department",
        departmentList.getRoom(firstPatient.getLastRoom()).getDepartmentId());
    move.put("second_room", secondPatient.getLastRoom());
    move.put("second_department",
        departmentList.getRoom(secondPatient.getLastRoom()).getDepartmentId());
    return move;
  }

  public Map<String, Integer> generateShiftAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = patientList.getRandomShiftPatient();
    int shift = patient.getRandomShift();
    move.put("type", 2);
    move.put("first_patient", patient.getId());
    move.put("first_room", patient.getLastRoom());
    move.put("first_department", departmentList.getRoom(patient.getLastRoom()).getDepartmentId());
    move.put("first_shift", shift);
    return move;
  }

  public Map<String, Integer> generateSwapAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapAdmissionPatient(firstPatient);
    if (secondPatient == null) return generateMove();
    move.put("type", 3);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("first_department",
        departmentList.getRoom(firstPatient.getLastRoom()).getDepartmentId());
    move.put("second_room", secondPatient.getLastRoom());
    move.put("second_department",
        departmentList.getRoom(secondPatient.getLastRoom()).getDepartmentId());
    move.put("first_shift", secondPatient.getAdmission() - firstPatient.getAdmission());
    move.put("second_shift", firstPatient.getAdmission() - secondPatient.getAdmission());
    return move;
  }

  // Move Execution
  public void executeNewMove() {
    lastMove = generateMove();
    switch (lastMove.get("type")) {
      case 0 -> executeChangeRoom();
      case 1 -> executeSwapRoom();
      case 2 -> executeShiftAdmission();
      case 3 -> executeSwapAdmission();
    }
  }

  public int getNewAssignmentSavings(Patient patient, int room, int shift) {
    if (patient.getLastRoom() == room && shift == 0) return 0;

    int savings = patient.getCurrentRoomCost() - patient.getRoomCost(room);
    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      savings += getDynamicCancellationSavings(patient, day);
      schedule.cancelPatient(patient, day);
    }
    patient.shiftAdmission(shift);
    savings -= getPenalty("delay") * shift;
    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      savings -= getDynamicAssignmentCost(patient, room, day);
      schedule.assignPatient(patient, room, day);
    }
    return savings;
  }

  public int getDynamicCancellationSavings(Patient patient, int day) {
    int room = patient.getRoom(day);
    int savings = 0;
    if (schedule.getCapacityViolations(room, day) > 0) savings += getPenalty("capacity_violation");
    if (schedule.hasSingleDynamicGenderViolation(room, day, patient.getGender()))
      savings += getPenalty("gender");
    return savings;
  }

  public int getDynamicAssignmentCost(Patient patient, int room, int day) {
    int cost = 0;
    if (schedule.getCapacityMargin(room, day) == 0) cost += getPenalty("capacity_violation");
    if (schedule.isFirstDynamicGenderViolation(room, day, patient.getGender()))
      cost += getPenalty("gender");
    return cost;
  }

  public int getDynamicSwapAdmissionSavings(Patient patient, int shift, int room) {
    int savings = 0;
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      savings += getDynamicCancellationSavings(patient, i);
      schedule.cancelPatient(patient, i);
    }
    patient.shiftAdmission(shift);
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      savings -= getDynamicAssignmentCost(patient, room, i);
      schedule.assignPatient(patient, room, i);
    }
    return savings;
  }

  public double getDepartmentLoadSavings(Set<Integer> departments) {
    double savings = 0;
    for (int dep : departments) {
      savings += schedule.getDepartmentLoadCost(dep);
      schedule.calculateDepartmentLoadCost(dep);
      savings -= schedule.getDepartmentLoadCost(dep);
    }
    return savings;
  }

  public double getDailyLoadSavings(Set<Integer> days) {
    double savings = 0;
    for (int day : days) {
      savings += schedule.getDailyLoadCost(day);
      schedule.calculateDailyLoadCost(day);
      savings -= schedule.getDailyLoadCost(day);
    }
    return savings;
  }

  public void executeChangeRoom() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    int room = lastMove.get("second_room");

    int patientSavings = patient.getCurrentRoomCost() - patient.getRoomCost(room);
    patientSavings += getNewAssignmentSavings(patient, room, 0);

    Set<Integer> departments = new HashSet<>();
    departments.add(lastMove.get("first_department"));
    departments.add(lastMove.get("second_department"));
    double loadSavings = getDepartmentLoadSavings(departments);
    loadSavings += getDailyLoadSavings(patient.getAdmittedDays());

    lastMove.put("patient_savings", patientSavings);
    lastMove.put("load_savings", (int) (Math.round(loadSavings)));
  }

  public void executeSwapRoom() {
    int patientSavings = 0;
    Set<Integer> affectedDays = new HashSet<>();
    Set<Integer> departments = new HashSet<>();
    departments.add(lastMove.get("first_department"));
    departments.add(lastMove.get("second_department"));

    for (int i = 0; i < 2; i++) {
      String[] keys = (i == 0)
          ? new String[]{"first_patient", "second_room"}
          : new String[]{"second_patient", "first_room"};
      Patient patient = patientList.getPatient(lastMove.get(keys[0]));
      int room = lastMove.get(keys[1]);
      affectedDays.addAll(patient.getAdmittedDays());
      patientSavings += getNewAssignmentSavings(patient, room, 0);
    }

    double loadSavings = getDailyLoadSavings(affectedDays);
    loadSavings += getDepartmentLoadSavings(departments);

    lastMove.put("patient_savings", patientSavings);
    lastMove.put("load_savings", (int) (Math.round(loadSavings)));
  }

  public void executeShiftAdmission() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    Set<Integer> affectedDays = patient.getAdmittedDays();
    int patientSavings =
        getNewAssignmentSavings(patient, lastMove.get("first_room"), lastMove.get("first_shift"));
    affectedDays.addAll(patient.getAdmittedDays());

    Set<Integer> departments = new HashSet<>();
    departments.add(lastMove.get("first_department"));
    double loadSavings = getDepartmentLoadSavings(departments);
    loadSavings += getDailyLoadSavings(affectedDays);

    lastMove.put("patient_savings", patientSavings);
    lastMove.put("load_savings", (int) (Math.round(loadSavings)));
  }

  public void executeSwapAdmission() {
    Patient firstPatient = patientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = patientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDays = new HashSet<>();
    Set<Integer> departments = new HashSet<>();
    departments.add(lastMove.get("first_department"));
    departments.add(lastMove.get("second_department"));
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    int patientSavings = getNewAssignmentSavings(firstPatient, lastMove.get("second_room"),
        lastMove.get("first_shift"));
    patientSavings += getNewAssignmentSavings(secondPatient, lastMove.get("first_room"),
        lastMove.get("second_shift"));
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());

    double loadSavings = getDailyLoadSavings(affectedDays) + getDepartmentLoadSavings(departments);
    lastMove.put("patient_savings", patientSavings);
    lastMove.put("load_savings", (int) (Math.round(loadSavings)));
  }

  // Undo Move
  public void undoLastMove() {
    switch (lastMove.get("type")) {
      case 0 -> undoChangeRoom();
      case 1 -> undoSwapRoom();
      case 2 -> undoShiftAdmission();
      case 3 -> undoSwapAdmission();
    }
  }

  public void readmitPatient(Patient patient, int room, int shift) {
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.cancelPatient(patient, i);
    patient.shiftAdmission(shift);
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.assignPatient(patient, room, i);
  }

  public void undoChangeRoom() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    readmitPatient(patient, lastMove.get("first_room"), 0);
    for (int day : patient.getAdmittedDays()) schedule.calculateDailyLoadCost(day);
    schedule.calculateDepartmentLoadCost(lastMove.get("first_department"));
    schedule.calculateDepartmentLoadCost(lastMove.get("second_department"));
  }

  public void undoSwapRoom() {
    Set<Integer> affectedDays = new HashSet<>();
    for (int i = 0; i < 2; i++) {
      String[] keys = (i == 0)
          ? new String[]{"first_patient", "first_room"}
          : new String[]{"second_patient", "second_room"};
      Patient patient = patientList.getPatient(lastMove.get(keys[0]));
      affectedDays.addAll(patient.getAdmittedDays());
      readmitPatient(patient, lastMove.get(keys[1]), 0);
    }
    for (int day : affectedDays) schedule.calculateDailyLoadCost(day);
    schedule.calculateDepartmentLoadCost(lastMove.get("first_department"));
    schedule.calculateDepartmentLoadCost(lastMove.get("second_department"));
  }

  public void undoShiftAdmission() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    int shift = lastMove.get("first_shift");
    int room = patient.getLastRoom();
    int department = departmentList.getRoom(room).getDepartmentId();

    Set<Integer> affectedDays = new HashSet<>(patient.getAdmittedDays());
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.cancelPatient(patient, i);
    patient.shiftAdmission(-shift);
    affectedDays.addAll(patient.getAdmittedDays());
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.assignPatient(patient, room, i);

    schedule.calculateDepartmentLoadCost(department);
    for (int day : affectedDays)
      schedule.calculateDailyLoadCost(day);
  }

  public void undoSwapAdmission() {
    Patient firstPatient = patientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = patientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDepartments = new HashSet<>();
    affectedDepartments.add(lastMove.get("first_department"));
    affectedDepartments.add(lastMove.get("second_department"));

    Set<Integer> affectedDays = new HashSet<>(firstPatient.getAdmittedDays());
    for (int day = firstPatient.getAdmission(); day < firstPatient.getDischarge(); day++)
      schedule.cancelPatient(firstPatient, day);
    firstPatient.shiftAdmission(lastMove.get("second_shift"));
    affectedDays.addAll(firstPatient.getAdmittedDays());
    for (int day = firstPatient.getAdmission(); day < firstPatient.getDischarge(); day++)
      schedule.assignPatient(firstPatient, lastMove.get("first_room"), day);

    affectedDays.addAll(secondPatient.getAdmittedDays());
    for (int day = secondPatient.getAdmission(); day < secondPatient.getDischarge(); day++)
      schedule.cancelPatient(secondPatient, day);
    secondPatient.shiftAdmission(lastMove.get("first_shift"));
    affectedDays.addAll(secondPatient.getAdmittedDays());
    for (int day = secondPatient.getAdmission(); day < secondPatient.getDischarge(); day++)
      schedule.assignPatient(secondPatient, lastMove.get("second_room"), day);

    for (int day : affectedDays)
      schedule.calculateDailyLoadCost(day);
    for (int dep : affectedDepartments)
      schedule.calculateDepartmentLoadCost(dep);
  }
}
