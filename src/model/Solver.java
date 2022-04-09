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

  public int getPatientCost() {
    return patientCost;
  }

  public int getLoadCost() {
    return loadCost;
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
        {"id", "type", "first_patient", "second_patient", "first_room", "second_room",
            "first_shift", "second_shift", "patient_savings", "load_savings", "patient_cost",
            "load_cost"};

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
        schedule.getCapacityViolations() + "\nSoft cost: \t\t\t\t" +
        (patientCost - (getPenalty("capacity_violation") * schedule.getCapacityViolations())));
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
  }


  // Start Search Procedure
  public void solve(String type) {
    if (!type.equals("patient_savings") && !type.equals("load_savings")) {
      System.err.println("Invalid objective");
      return;
    }
    for (int i = 0; i < 100000; i++) {
      executeNewMove();
      int savings = lastMove.get(type);
      if (savings > 0) {
        lastMove.put("accepted", 1);
        if (type.equals("patient_savings")) patientCost -= savings;
        else loadCost -= savings;
      } else {
        lastMove.put("accepted", 0);
        undoLastMove();
      }
      generatedMoves.add(lastMove);
      if (i % 10000 == 0) printCosts();
    }
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
    move.put("second_room", room);
    return move;
  }

  public Map<String, Integer> generateSwapRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapRoomPatient(firstPatient);
    if (secondPatient == null) return generateSwapRoom();
    move.put("type", 1);
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
    move.put("type", 2);
    move.put("first_patient", patient.getId());
    move.put("first_room", patient.getLastRoom());
    move.put("first_shift", shift);
    return move;
  }

  public Map<String, Integer> generateSwapAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = patientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapAdmissionPatient(firstPatient);
    if (secondPatient == null) return generateSwapAdmission();
    move.put("type", 3);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("second_room", secondPatient.getLastRoom());
    move.put("first_shift", secondPatient.getAdmission() - firstPatient.getAdmission());
    move.put("second_shift", firstPatient.getAdmission() - secondPatient.getAdmission());
    return move;
  }

  // Move Execution
  public void executeNewMove() {
    lastMove = generateMove();
    int[] savings;
    savings = switch (lastMove.get("type")) {
      case 0 -> executeChangeRoom(lastMove.get("first_patient"), lastMove.get("second_room"));
      case 1 -> executeSwapRoom(lastMove.get("first_patient"), lastMove.get("second_patient"));
      case 2 -> executeShiftAdmission(lastMove.get("first_patient"), lastMove.get("first_shift"));
      case 3 -> executeSwapAdmission(lastMove.get("first_patient"), lastMove.get("second_patient"));
      default -> new int[]{0, 0};
    };
    lastMove.put("patient_savings", savings[0]);
    lastMove.put("load_savings", savings[1]);
  }

  public int[] executeChangeRoom(int pat, int room) {
    Patient patient = patientList.getPatient(pat);
    int originalRoom = patient.getLastRoom();
    int patientSavings = patient.getRoomCost(originalRoom) - patient.getRoomCost(room);
    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      patientSavings += getDynamicCancellationSavings(patient, day);
      schedule.cancelPatient(patient, day);
      patientSavings -= getDynamicAssignmentCost(patient, room, day);
      schedule.assignPatient(patient, room, day);
    }

    double loadSavings = 0;
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      loadSavings += schedule.getDailyLoadCost(i);
      schedule.calculateDailyLoadCost(i);
      loadSavings -= schedule.getDailyLoadCost(i);
    }
    int firstDepartment = departmentList.getRoom(originalRoom).getDepartmentId();
    int secondDepartment = departmentList.getRoom(room).getDepartmentId();
    if (firstDepartment != secondDepartment)
      for (int dep : new int[]{firstDepartment, secondDepartment}) {
        loadSavings += schedule.getDepartmentLoadCost(dep);
        schedule.calculateDepartmentLoadCost(dep);
        loadSavings -= schedule.getDepartmentLoadCost(dep);
      }
    return new int[]{patientSavings, (int) loadSavings};
  }

  public int[] executeSwapRoom(int fPat, int sPat) {
    int fRoom = patientList.getPatient(fPat).getLastRoom();
    int sRoom = patientList.getPatient(sPat).getLastRoom();
    int[] firstSavings = executeChangeRoom(fPat, sRoom);
    int[] secondSavings = executeChangeRoom(sPat, fRoom);
    int patientSavings = firstSavings[0] + secondSavings[0];
    int loadSavings = firstSavings[1] + secondSavings[1];
    return new int[]{patientSavings, loadSavings};
  }

  public int[] executeShiftAdmission(int pat, int shift) {
    if (shift == 0) return new int[]{0, 0};
    Patient patient = patientList.getPatient(pat);
    int room = patient.getLastRoom();

    Set<Integer> affectedDays = new HashSet<>();
    int patientSavings = -shift * getPenalty("delay");
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      affectedDays.add(i);
      patientSavings += getDynamicCancellationSavings(patient, i);
      schedule.cancelPatient(patient, i);
    }
    patient.shiftAdmission(shift);
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      affectedDays.add(i);
      int assignmentDay = (shift > 0) ? patient.getDischarge() - 1 - i : patient.getAdmission() + i;
      patientSavings -= getDynamicAssignmentCost(patient, room, assignmentDay);
      schedule.assignPatient(patient, room, assignmentDay);
    }

    double loadSavings = 0;
    int dep = departmentList.getRoom(room).getDepartmentId();
    loadSavings += schedule.getDepartmentLoadCost(dep);
    schedule.calculateDepartmentLoadCost(dep);
    loadSavings -= schedule.getDepartmentLoadCost(dep);

    for (int i : affectedDays) {
      loadSavings += schedule.getDailyLoadCost(i);
      schedule.calculateDailyLoadCost(i);
      loadSavings -= schedule.getDailyLoadCost(i);
    }

    return new int[]{patientSavings, (int) loadSavings};
  }

  public int[] executeSwapAdmission(int fPat, int sPat) {
    Patient firstPatient = patientList.getPatient(fPat);
    Patient secondPatient = patientList.getPatient(sPat);
    int firstShift = secondPatient.getAdmission() - firstPatient.getAdmission();
    int secondShift = firstPatient.getAdmission() - secondPatient.getAdmission();
    int firstRoom = firstPatient.getLastRoom();
    int secondRoom = secondPatient.getLastRoom();
    int firstDep = departmentList.getRoom(firstRoom).getDepartmentId();
    int secondDep = departmentList.getRoom(secondRoom).getDepartmentId();

    int patientSavings = firstPatient.getCurrentRoomCost()
        - firstPatient.getRoomCost(secondPatient.getLastRoom())
        + secondPatient.getCurrentRoomCost()
        - secondPatient.getRoomCost(firstPatient.getLastRoom());
    patientSavings += getDynamicSwapAdmissionSavings(firstPatient, firstShift, secondRoom);
    patientSavings += getDynamicSwapAdmissionSavings(secondPatient, secondShift, firstRoom);

    double loadSavings = 0;
    Set<Integer> affectedDays = new HashSet<>();
    int longestStay = Math.max(firstPatient.getStayLength(), secondPatient.getStayLength());
    for (int i = 0; i < longestStay; i++) {
      affectedDays.add(firstPatient.getAdmission() + i);
      affectedDays.add(secondPatient.getAdmission() + i);
    }
    for (int dep : new int[]{firstDep, secondDep}) {
      loadSavings += schedule.getDepartmentLoadCost(dep);
      schedule.calculateDepartmentLoadCost(dep);
      loadSavings -= schedule.getDepartmentLoadCost(dep);
    }
    for (int day : affectedDays) {
      loadSavings += schedule.getDailyLoadCost(day);
      schedule.calculateDailyLoadCost(day);
      loadSavings -= schedule.getDailyLoadCost(day);
    }

    return new int[]{patientSavings, (int) loadSavings};
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

  public void undoChangeRoom() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    int originalRoom = lastMove.get("first_room");
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
      schedule.cancelPatient(patient, i);
      schedule.assignPatient(patient, originalRoom, i);
    }
  }

  public void undoSwapRoom() {
    Patient firstPatient = patientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = patientList.getPatient(lastMove.get("second_patient"));
    int firstRoom = lastMove.get("first_room");
    int secondRoom = lastMove.get("second_room");
    for (int i = firstPatient.getAdmission(); i < firstPatient.getDischarge(); i++) {
      schedule.cancelPatient(firstPatient, i);
      schedule.assignPatient(firstPatient, firstRoom, i);
    }
    for (int i = secondPatient.getAdmission(); i < secondPatient.getDischarge(); i++) {
      schedule.cancelPatient(secondPatient, i);
      schedule.assignPatient(secondPatient, secondRoom, i);
    }
  }

  public void undoShiftAdmission() {
    Patient patient = patientList.getPatient(lastMove.get("first_patient"));
    int shift = lastMove.get("first_shift");
    int room = patient.getLastRoom();
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.cancelPatient(patient, i);
    patient.shiftAdmission(-shift);
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.assignPatient(patient, room, i);
  }

  public void undoSwapAdmission() {
    Patient firstPatient = patientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = patientList.getPatient(lastMove.get("second_patient"));
    int firstRoom = secondPatient.getLastRoom();
    int secondRoom = firstPatient.getLastRoom();
    int delta = secondPatient.getAdmission() - firstPatient.getAdmission();

    for (int i = firstPatient.getAdmission(); i < firstPatient.getDischarge(); i++)
      schedule.cancelPatient(firstPatient, i);
    firstPatient.shiftAdmission(delta);
    for (int i = firstPatient.getAdmission(); i < firstPatient.getDischarge(); i++)
      schedule.assignPatient(firstPatient, firstRoom, i);

    for (int i = secondPatient.getAdmission(); i < secondPatient.getDischarge(); i++)
      schedule.cancelPatient(secondPatient, i);
    secondPatient.shiftAdmission(-delta);
    for (int i = secondPatient.getAdmission(); i < secondPatient.getDischarge(); i++)
      schedule.assignPatient(secondPatient, secondRoom, i);
  }
}
