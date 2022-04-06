package model;

import util.DateConverter;

import java.util.*;

public class Solver {
  private final Map<String, Integer> penalties;
  private final PatientList patientList;
  private final DepartmentList departmentList;
  private final Schedule schedule;
  private int cost;
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

  public HashMap<String, String> getPlanningHorizon() {
    HashMap<String, String> horizon = new HashMap<>();
    horizon.put("start_day", DateConverter.getDateString(0));
    horizon.put("num_days", String.valueOf(DateConverter.getNumDays()));
    horizon.put("current_day", DateConverter.getDateString(0));
    return horizon;
  }

  public void printCosts() {
    System.out.println("\nTotal cost: \t\t\t" + cost +
        "\nCapacity violations: \t" + schedule.getCapacityViolations() +
        "\nSoft cost: \t\t\t\t" +
        (cost - (getPenalty("capacity_violation") * schedule.getCapacityViolations())));
  }

  public void init() {
    cost = 0;
    setFeasibleRooms();
    calculateRoomCosts();
    insertInitialPatients();
    assignRandomRooms();
    cost += schedule.getDynamicGenderViolations() * getPenalty("gender");
    cost += schedule.getCapacityViolations() * getPenalty("capacity_violation");
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
            propertyCost += getPenalty("room_property");
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

  public int getDynamicCancellationSavings(Patient patient, int day) {
    int room = patient.getRoom(day);
    int savings = 0;
    if (schedule.getCapacityViolations(room, day) > 0)
      savings += getPenalty("capacity_violation");
    if (schedule.hasSingleDynamicGenderViolation(room, day, patient.getGender()))
      savings += getPenalty("gender");
    return savings;
  }

  public int getDynamicAssignmentCost(Patient patient, int room, int day) {
    int cost = 0;
    if (schedule.getCapacityMargin(room, day) == 0)
      cost += getPenalty("capacity_violation");
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
    for (int i = 0; i < 100000; i++) {
      executeNewMove();
      int savings = lastMove.get("savings");
      if (savings > 0) {
        lastMove.put("accepted", 1);
        cost -= savings;
      } else {
        lastMove.put("accepted", 0);
        undoLastMove();
      }
      generatedMoves.add(lastMove);
      if (i % 10000 == 0) printCosts();
    }
  }

  public void undoLastMove() {
    switch (lastMove.get("type")) {
      case 0 -> undoChangeRoom();
      case 1 -> undoSwapRoom();
      case 2 -> undoShiftAdmission();
      case 3 -> undoSwapAdmission();
    }
  }

  public void undoChangeRoom() {
    Patient patient = patientList.getPatient(lastMove.get("patient"));
    int originalRoom = lastMove.get("original_room");
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
    Patient patient = patientList.getPatient(lastMove.get("patient"));
    int shift = lastMove.get("shift");
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

  public void executeNewMove() {
    lastMove = generateMove();
    lastMove.put("savings", switch (lastMove.get("type")) {
      case 0 -> executeChangeRoom(lastMove.get("patient"), lastMove.get("new_room"));
      case 1 -> executeSwapRoom(lastMove.get("first_patient"), lastMove.get("second_patient"));
      case 2 -> executeShiftAdmission(lastMove.get("patient"), lastMove.get("shift"));
      case 3 -> executeSwapAdmission(lastMove.get("first_patient"), lastMove.get("second_patient"));
      default -> 0;
    });
  }

  public int executeChangeRoom(int pat, int room) {
    Patient patient = patientList.getPatient(pat);
    int savings = patient.getRoomCost(patient.getLastRoom()) - patient.getRoomCost(room);
    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      savings += getDynamicCancellationSavings(patient, day);
      schedule.cancelPatient(patient, day);
      savings -= getDynamicAssignmentCost(patient, room, day);
      schedule.assignPatient(patient, room, day);
    }
    return savings;
  }

  public int executeSwapRoom(int fPat, int sPat) {
    int fRoom = patientList.getPatient(fPat).getLastRoom();
    int sRoom = patientList.getPatient(sPat).getLastRoom();
    patientList.getPatient(fPat).verifyLOS("sr");
    patientList.getPatient(sPat).verifyLOS("sr");
    return executeChangeRoom(fPat, sRoom) + executeChangeRoom(sPat, fRoom);
  }

  public int executeShiftAdmission(int pat, int shift) {
    Patient patient = patientList.getPatient(pat);
    int room = patient.getLastRoom();
    int savings = -shift * getPenalty("delay");
    for (int i = 0; i < Math.min(patient.getStayLength(), Math.abs(shift)); i++) {
      int cancelDay = (shift > 0)
          ? patient.getAdmission() + i
          : patient.getDischarge() - 1 - i;
      int assignmentDay = (shift > 0)
          ? patient.getDischarge() - 1 + shift - i
          : patient.getAdmission() + shift + i;
      savings += getDynamicCancellationSavings(patient, cancelDay);
      schedule.cancelPatient(patient, cancelDay);
      savings -= getDynamicAssignmentCost(patient, room, assignmentDay);
      schedule.assignPatient(patient, room, assignmentDay);
    }
    patient.shiftAdmission(shift);
    return savings;
  }

  public int executeSwapAdmission(int fPat, int sPat) {
    Patient firstPatient = patientList.getPatient(fPat);
    Patient secondPatient = patientList.getPatient(sPat);
    int savings = firstPatient.getCurrentRoomCost()
        - firstPatient.getRoomCost(secondPatient.getLastRoom())
        + secondPatient.getCurrentRoomCost()
        - secondPatient.getRoomCost(firstPatient.getLastRoom());
    // Since both patients can be admitted on the other AD, the total delay cost stays the same
    int firstShift = secondPatient.getAdmission() - firstPatient.getAdmission();
    int secondShift = firstPatient.getAdmission() - secondPatient.getAdmission();
    int firstRoom = firstPatient.getLastRoom();
    int secondRoom = secondPatient.getLastRoom();
    savings += getDynamicSwapAdmissionSavings(firstPatient, firstShift, secondRoom);
    savings += getDynamicSwapAdmissionSavings(secondPatient, secondShift, firstRoom);
    return savings;
  }

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
    move.put("patient", patient.getId());
    move.put("original_room", patient.getLastRoom());
    move.put("new_room", room);
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
    move.put("patient", patient.getId());
    move.put("shift", shift);
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
    return move;
  }

}
