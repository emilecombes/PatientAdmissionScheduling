package model;

import util.DateConverter;
import util.Variables;

import java.util.*;

public class Solver {
  // SOLUTION
  private Schedule schedule;
  private int patientCost, loadCost;
  private List<Solution> approximationArchive, intermediateApproximationArchive;
  private List<Rectangle> rectangleArchive;

  // MOVES
  private final List<Map<String, Integer>> generatedMoves;
  private Map<String, Integer> lastMove;

  public Solver() {
    schedule = new Schedule();
    generatedMoves = new ArrayList<>();
  }

  // Functions to remove
  public Map<String, Integer> getCostInfo() {
    Map<String, Integer> costs = new LinkedHashMap<>();
    int roomCosts = 0;
    int transfer = 0;
    int totalDelay = 0;
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);
      if (patient.isInitial())
        if (patient.getInitialRoom() != patient.getLastRoom())
          transfer += Variables.TRANSFER_PEN;
      roomCosts += patient.getTotalRoomCost();
      totalDelay += patient.getDelay() * Variables.DELAY_PEN;
    }
    roomCosts -= transfer;
    int gender = schedule.getDynamicGenderViolations() * Variables.GENDER_PEN;
    int capacity = schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
    int load = schedule.getTotalDailyLoadCost() + schedule.getTotalDepartmentLoadCost() + capacity;
    costs.put("capacity_violations", capacity);
    costs.put("transfer", transfer);
    costs.put("patient_room", roomCosts);
    costs.put("delay", totalDelay);
    costs.put("gender", gender);
    costs.put("total_patient", patientCost);
    costs.put("patient", patientCost - capacity);
    costs.put("total_load", load);
    costs.put("load", load - capacity);
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
            "patient_savings", "load_savings", "total_patient_cost", "total_load_cost",
            "patient_cost", "load_cost"};

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

  public void printCost() {
    System.out.println(patientCost);
  }

  public void printCosts() {
    int violationCost = schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
    System.out.println(
        "\nCapacity Violations\t:" + schedule.getCapacityViolations() + " (" + violationCost + ")"
            + "\nPatient Cost\t\t:" + (patientCost - violationCost)
            + "\nLoad Cost\t\t:" + (loadCost - violationCost)
    );
  }

  public void setPatientBedNumbers() {
    Set<Integer>[][] usedBeds =
        new Set[DepartmentList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    for (int i = 0; i < usedBeds.length; i++)
      for (int j = 0; j < usedBeds[0].length; j++)
        usedBeds[i][j] = new HashSet<>();
    for (int pat = 0; pat < PatientList.getNumberOfPatients(); pat++) {
      Patient patient = PatientList.getPatient(pat);
      int bed = 0;
      int day = patient.getAdmission();
      while (day < patient.getDischarge()) {
        if (usedBeds[patient.getRoom(day)][day].contains(bed)) {
          bed++;
          day = patient.getAdmission();
        }
        day++;
      }
      patient.setBed(bed);
      for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
        usedBeds[patient.getRoom(i)][i].add(bed);
      }
    }
  }

  // Preprocessing
  public void preProcessing() {
    setFeasibleRooms();
    calculateRoomCosts();
  }

  public void setFeasibleRooms() {
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);

      Set<Integer> feasibleRooms = new HashSet<>();
      List<Department> feasibleDepartments =
          DepartmentList.getDepartmentsForTreatment(patient.getTreatment());
      for (Department department : feasibleDepartments)
        feasibleRooms.addAll(department.getRoomIndices());

      Set<Integer> infeasibleRooms = new HashSet<>();
      for (int r : feasibleRooms) {
        Room room = DepartmentList.getRoom(r);
        if (!room.hasAllFeatures(patient.getNeededProperties()))
          infeasibleRooms.add(r);
      }

      feasibleRooms.removeAll(infeasibleRooms);
      patient.setFeasibleRooms(feasibleRooms);
    }
  }

  public void calculateRoomCosts() {
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);
      for (int r : patient.getFeasibleRooms()) {
        Room room = DepartmentList.getRoom(r);

        int propertyCost = 0;
        for (String property : patient.getPreferredProperties())
          if (!room.hasFeature(property)) propertyCost += Variables.ROOM_PROP_PEN;
        patient.setRoomCost("room_property", r, propertyCost);

        int capacityCost;
        if (patient.getPreferredCap() < room.getCapacity() && patient.getPreferredCap() != -1)
          capacityCost = Variables.PREF_CAP_PEN;
        else capacityCost = 0;
        patient.setRoomCost("capacity_preference", r, capacityCost);

        int specialityCost;
        String specialism = DepartmentList.getNeededSpecialism(patient.getTreatment());
        Department department = DepartmentList.getDepartment(room.getDepartment());
        if (!department.hasMainSpecialism(specialism)) specialityCost = Variables.SPECIALITY_PEN;
        else specialityCost = 0;
        patient.setRoomCost("speciality", r, specialityCost);

        int genderCost;
        if (!room.canHostGender(patient.getGender())) genderCost = Variables.GENDER_PEN;
        else genderCost = 0;
        patient.setRoomCost("gender", r, genderCost);

        int transferCost;
        if (patient.isInitial() && patient.getRoom(patient.getAdmission()) != r)
          transferCost = Variables.TRANSFER_PEN;
        else transferCost = 0;
        patient.setRoomCost("transfer", r, transferCost);
      }

      patient.calculateTotalRoomCost();
    }
  }


  // Initializing
  public void initSchedule() {
    patientCost = 0;
    loadCost = 0;
    insertInitialPatients();
    assignRandomRooms();
    patientCost += schedule.getDynamicGenderViolations() * Variables.GENDER_PEN;
    for (int i = 0; i < DateConverter.getTotalHorizon(); i++)
      schedule.calculateDailyLoadCost(i);
    for (int i = 0; i < DepartmentList.getNumberOfDepartments(); i++)
      schedule.calculateDepartmentLoadCost(i);
    loadCost += schedule.getTotalDailyLoadCost();
    loadCost += schedule.getTotalDepartmentLoadCost();
    loadCost += schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
    patientCost += schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
  }

  public void insertInitialPatients() {
    for (Patient p : PatientList.getInitialPatients()) {
      int room = p.getRoom(p.getAdmission());
      for (int i = p.getAdmission(); i < p.getDischarge(); i++)
        schedule.assignPatient(p, room, i);
      patientCost += p.getRoomCost(room);
    }
  }

  public void assignRandomRooms() {
    for (Patient p : PatientList.getRegisteredPatients()) {
      int room = p.getRandomFeasibleRoom();
      for (int i = 0; i < p.getStayLength(); i++)
        schedule.assignPatient(p, room, i + p.getOriginalAD());
      patientCost += p.getRoomCost(room);
    }
  }

  public void loadSchedule(Solution s) {
    patientCost = s.getPatientCost();
    loadCost = s.getEquityCost();
    schedule = new Schedule(s);
  }

  // Local search
  public void boxSplitting(int maxPC, int minWE, int delta) {
    approximationArchive = new LinkedList<>();
    intermediateApproximationArchive = new LinkedList<>();
    rectangleArchive = new LinkedList<>();
    optimizePatientCost();
    rectangleArchive.add(new Rectangle(new Point(patientCost, loadCost), new Point(maxPC, minWE)));
    while (!rectangleArchive.isEmpty()) {
      Rectangle rectangle = findBiggestRectangle();
      int c = (rectangle.getTop() - rectangle.getBottom()) / 2;
      optimizePatientCost(c);
      if (!currentlyNonDominated())
        rectangle.setLowerRight(new Point(rectangle.getRight(), c));
      else {
        updateApproximationArchive(-1);
        Rectangle horizontalHit = findRectangleOnX(patientCost);
        if (horizontalHit != null) {
          int top = Math.max(horizontalHit.getBottom(), c);
          Point updatedPoint = new Point(patientCost, top);
          if (horizontalHit.getTop() - top > delta)
            addRectangle(new Rectangle(horizontalHit.getUpperLeft(), updatedPoint));
        }
        Rectangle verticalHit = findRectangleOnY(loadCost);
        if (verticalHit != null) {
          int left = Math.max(patientCost, verticalHit.getLeft());
          Point updatedPoint = new Point(left, loadCost);
          if (loadCost - verticalHit.getBottom() > delta)
            addRectangle(new Rectangle(updatedPoint, verticalHit.getLowerRight()));
        }
        rectangleArchive.remove(horizontalHit);
        rectangleArchive.remove(verticalHit);
        removeCurrentlyDominatedRectangles();
      }
    }
  }

  public boolean currentlyNonDominated() {
    // TODO
    return schedule.isFeasible();
  }

  public void addRectangle(Rectangle r) {
    // TODO
  }

  public Rectangle findBiggestRectangle() {
    // TODO
    return null;
  }

  public Rectangle findRectangleOnX(int pc) {
    // TODO
    return null;
  }

  public Rectangle findRectangleOnY(int we) {
    // TODO
    return null;
  }

  public void removeCurrentlyDominatedRectangles() {
    // TODO
  }

  public void adjustLoadCost() {
    loadCost = schedule.getTotalDailyLoadCost()
        + schedule.getTotalDepartmentLoadCost()
        + schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
  }


  public void optimizePatientCost() {
    double stop = System.currentTimeMillis() + Variables.TIMEOUT_INSTANCE;
    double temp = Variables.PC_START_TEMP;
    while (temp > Variables.PC_STOP_TEMP && System.currentTimeMillis() < stop) {
      for (int i = 0; i < Variables.PC_ITERATIONS; i++) {
        executeNewMove();
        int savings = lastMove.get("patient_savings");
        if (savings > 0 || Math.random() < Math.exp(savings / temp)) acceptMove();
        else undoLastMove();
        generatedMoves.add(lastMove);
      }
      temp *= Variables.PC_ALPHA;
      adjustLoadCost();
      if (currentlyNonDominated()) updateApproximationArchive(temp);
    }
  }

  public void optimizePatientCost(int c) {
    Solution initialSolution = findNearestSolution(c);
    double temp = initialSolution.getTemperature();
    while (temp > Variables.PC_STOP_TEMP) {
      for (int i = 0; i < Variables.PC_ITERATIONS; i++) {
        executeNewMove();
        int ps = lastMove.get("patient_savings");
        int ls = lastMove.get("load_savings");
        int savings = ps + Math.max(c, loadCost) - Math.max(c, loadCost - ls);
        if (savings > 0 || Math.random() < Math.exp(savings / temp)) acceptMove();
        else undoLastMove();
        generatedMoves.add(lastMove);
      }
      temp *= Variables.PC_ALPHA;
      if (currentlyNonDominated()) updateApproximationArchive(temp);
      adjustLoadCost();
    }
  }

  public Solution findNearestSolution(int c) {
    // TODO
    return null;
  }

  public void updateApproximationArchive(double temp) {
    // TODO
  }


  // Move functionality
  public void acceptMove() {
    lastMove.put("accepted", 1);
    patientCost -= lastMove.get("patient_savings");
    loadCost -= lastMove.get("load_savings");
  }

  public Map<String, Integer> generateMove() {
    int random = (int) (Math.random() * 100);
    int type;
    if (random < 50) type = 0;
    else if (random < 80) type = 1;
    else if (random < 85) type = 2;
    else type = 3;
    switch (type) {
      case 0:
        return generateChangeRoom();
      case 1:
        return generateSwapRoom();
      case 2:
        return generateShiftAdmission();
      case 3:
        return generateSwapAdmission();
      default:
        return null;
    }
  }

  public Map<String, Integer> generateChangeRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = PatientList.getRandomPatient();
    int room = patient.getRandomFeasibleRoom();
    move.put("type", 0);
    move.put("first_patient", patient.getId());
    move.put("first_room", patient.getLastRoom());
    move.put("first_department", DepartmentList.getRoom(patient.getLastRoom()).getDepartmentId());
    move.put("second_room", room);
    move.put("second_department", DepartmentList.getRoom(room).getDepartmentId());
    return move;
  }

  public Map<String, Integer> generateSwapRoom() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = PatientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapRoomPatient(firstPatient);
    if (secondPatient == null) return generateMove();
    move.put("type", 1);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("first_department",
        DepartmentList.getRoom(firstPatient.getLastRoom()).getDepartmentId());
    move.put("second_room", secondPatient.getLastRoom());
    move.put("second_department",
        DepartmentList.getRoom(secondPatient.getLastRoom()).getDepartmentId());
    return move;
  }

  public Map<String, Integer> generateShiftAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient patient = PatientList.getRandomShiftPatient();
    int shift = patient.getRandomShift();
    move.put("type", 2);
    move.put("first_patient", patient.getId());
    move.put("first_room", patient.getLastRoom());
    move.put("first_department", DepartmentList.getRoom(patient.getLastRoom()).getDepartmentId());
    move.put("first_shift", shift);
    return move;
  }

  public Map<String, Integer> generateSwapAdmission() {
    Map<String, Integer> move = new HashMap<>();
    Patient firstPatient = PatientList.getRandomPatient();
    Patient secondPatient = schedule.getSwapAdmissionPatient(firstPatient);
    if (secondPatient == null) return generateMove();
    move.put("type", 3);
    move.put("first_patient", firstPatient.getId());
    move.put("second_patient", secondPatient.getId());
    move.put("first_room", firstPatient.getLastRoom());
    move.put("first_department",
        DepartmentList.getRoom(firstPatient.getLastRoom()).getDepartmentId());
    move.put("second_room", secondPatient.getLastRoom());
    move.put("second_department",
        DepartmentList.getRoom(secondPatient.getLastRoom()).getDepartmentId());
    move.put("first_shift", secondPatient.getAdmission() - firstPatient.getAdmission());
    move.put("second_shift", firstPatient.getAdmission() - secondPatient.getAdmission());
    return move;
  }

  public void executeNewMove() {
    lastMove = generateMove();
    if (lastMove.get("type") == 0) executeChangeRoom();
    else if (lastMove.get("type") == 1) executeSwapRoom();
    else if (lastMove.get("type") == 2) executeShiftAdmission();
    else if (lastMove.get("type") == 3) executeSwapAdmission();
  }

  public int[] getAssignmentSavings(Patient patient, int room, int shift) {
    if (patient.getLastRoom() == room && shift == 0)
      return new int[]{0, 0};
    int capacityViolations = schedule.getCapacityViolations();
    int patientSavings = patient.getCurrentRoomCost() - patient.getRoomCost(room);

    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      if (schedule.hasSingleDynamicGenderViolation(patient.getLastRoom(), day, patient.getGender()))
        patientSavings += Variables.GENDER_PEN;
      schedule.cancelPatient(patient, day);
    }

    patientSavings -= shift * Variables.DELAY_PEN;
    patient.shiftAdmission(shift);

    for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
      if (schedule.isFirstDynamicGenderViolation(room, day, patient.getGender()))
        patientSavings -= Variables.GENDER_PEN;
      schedule.assignPatient(patient, room, day);
    }

    int capacitySavings = (capacityViolations - schedule.getCapacityViolations())
        * Variables.CAP_VIOL_PEN;
    return new int[]{patientSavings, capacitySavings};
  }

  public double getDepartmentLoadSavings() {
    Set<Integer> departments = new HashSet<>();
    departments.add(lastMove.get("first_department"));
    if (lastMove.get("type") != 2) departments.add(lastMove.get("second_department"));

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

  public void addSavingsToMove(int capacitySavings, int patientSavings, double loadSavings) {
    int capacityViolations = schedule.getCapacityViolations();
    int capacityViolationCost = capacityViolations * Variables.CAP_VIOL_PEN;
    lastMove.put("capacity_violations", capacityViolations);
    lastMove.put("capacity_violation_savings", capacitySavings);
    lastMove.put("total_patient_cost", patientCost);
    lastMove.put("patient_cost", patientCost - capacityViolationCost);
    lastMove.put("patient_savings", patientSavings);
    lastMove.put("total_load_cost", loadCost);
    lastMove.put("load_cost", loadCost - capacityViolationCost);
    lastMove.put("load_savings", (int) loadSavings);
  }

  public void executeChangeRoom() {
    Patient patient = PatientList.getPatient(lastMove.get("first_patient"));

    int[] assignmentSavings = getAssignmentSavings(patient, lastMove.get("second_room"), 0);

    int patientSavings = assignmentSavings[0] + assignmentSavings[1];
    double loadSavings = getDepartmentLoadSavings()
        + getDailyLoadSavings(patient.getAdmittedDays())
        + assignmentSavings[1];

    addSavingsToMove(assignmentSavings[1], patientSavings, loadSavings);
  }

  public void executeSwapRoom() {
    Patient firstPatient = PatientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = PatientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDays = new HashSet<>();

    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    int[] firstAssignmentSavings = getAssignmentSavings(
        firstPatient, lastMove.get("second_room"), 0
    );
    int[] secondAssignmentSavings = getAssignmentSavings(
        secondPatient, lastMove.get("first_room"), 0
    );
    int[] assignmentSavings = new int[]{
        firstAssignmentSavings[0] + secondAssignmentSavings[0],
        firstAssignmentSavings[1] + secondAssignmentSavings[1]
    };

    int patientSavings = assignmentSavings[0] + assignmentSavings[1];
    double loadSavings = getDepartmentLoadSavings()
        + getDailyLoadSavings(affectedDays)
        + assignmentSavings[1];

    addSavingsToMove(assignmentSavings[1], patientSavings, loadSavings);
  }

  public void executeShiftAdmission() {
    Patient patient = PatientList.getPatient(lastMove.get("first_patient"));
    Set<Integer> affectedDays = new HashSet<>();

    affectedDays.addAll(patient.getAdmittedDays());
    int[] assignmentSavings = getAssignmentSavings(
        patient, lastMove.get("first_room"), lastMove.get("first_shift")
    );
    affectedDays.addAll(patient.getAdmittedDays());

    int patientSavings = assignmentSavings[0] + assignmentSavings[1];
    double loadSavings = getDepartmentLoadSavings()
        + getDailyLoadSavings(affectedDays)
        + assignmentSavings[1];

    addSavingsToMove(assignmentSavings[1], patientSavings, loadSavings);
  }

  public void executeSwapAdmission() {
    Patient firstPatient = PatientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = PatientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDays = new HashSet<>();

    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    int[] firstAssignmentSavings = getAssignmentSavings(
        firstPatient, lastMove.get("second_room"), lastMove.get("first_shift")
    );
    int[] secondAssignmentSavings = getAssignmentSavings(
        secondPatient, lastMove.get("first_room"), lastMove.get("second_shift")
    );
    int[] assignmentSavings = new int[]{
        firstAssignmentSavings[0] + secondAssignmentSavings[0],
        firstAssignmentSavings[1] + secondAssignmentSavings[1]
    };
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());

    int patientSavings = assignmentSavings[0] + assignmentSavings[1];
    double loadSavings = getDailyLoadSavings(affectedDays)
        + getDepartmentLoadSavings()
        + assignmentSavings[1];

    addSavingsToMove(assignmentSavings[1], patientSavings, loadSavings);
  }

  // Undo Move
  public void undoLastMove() {
    lastMove.put("accepted", 0);
    if (lastMove.get("type") == 0) undoChangeRoom();
    else if (lastMove.get("type") == 1) undoSwapRoom();
    else if (lastMove.get("type") == 2) undoShiftAdmission();
    else if (lastMove.get("type") == 3) undoSwapAdmission();
  }

  public void readmitPatient(Patient patient, int room, int shift) {
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.cancelPatient(patient, i);
    patient.shiftAdmission(shift);
    for (int i = patient.getAdmission(); i < patient.getDischarge(); i++)
      schedule.assignPatient(patient, room, i);
  }

  public void recalculateLoadCosts(Set<Integer> days) {
    for (int day : days) schedule.calculateDailyLoadCost(day);
    int fDep = lastMove.get("first_department");
    int sDep = lastMove.getOrDefault("second_department", -1);
    schedule.calculateDepartmentLoadCost(fDep);
    if (fDep != sDep && sDep != -1)
      schedule.calculateDepartmentLoadCost(sDep);
  }

  public void undoChangeRoom() {
    Patient patient = PatientList.getPatient(lastMove.get("first_patient"));
    readmitPatient(patient, lastMove.get("first_room"), 0);
    recalculateLoadCosts(patient.getAdmittedDays());
  }

  public void undoSwapRoom() {
    Patient firstPatient = PatientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = PatientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDays = new HashSet<>();
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    readmitPatient(firstPatient, lastMove.get("first_room"), 0);
    readmitPatient(secondPatient, lastMove.get("second_room"), 0);
    recalculateLoadCosts(affectedDays);
  }

  public void undoShiftAdmission() {
    Patient patient = PatientList.getPatient(lastMove.get("first_patient"));
    Set<Integer> affectedDays = new HashSet<>(patient.getAdmittedDays());
    readmitPatient(patient, lastMove.get("first_room"), (-lastMove.get("first_shift")));
    affectedDays.addAll(patient.getAdmittedDays());
    recalculateLoadCosts(affectedDays);
  }

  public void undoSwapAdmission() {
    Patient firstPatient = PatientList.getPatient(lastMove.get("first_patient"));
    Patient secondPatient = PatientList.getPatient(lastMove.get("second_patient"));
    Set<Integer> affectedDays = new HashSet<>();
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    readmitPatient(firstPatient, lastMove.get("first_room"), lastMove.get("second_shift"));
    readmitPatient(secondPatient, lastMove.get("second_room"), lastMove.get("first_shift"));
    affectedDays.addAll(firstPatient.getAdmittedDays());
    affectedDays.addAll(secondPatient.getAdmittedDays());
    recalculateLoadCosts(affectedDays);
  }
}
