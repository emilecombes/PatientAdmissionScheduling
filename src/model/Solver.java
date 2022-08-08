package model;

import util.DateConverter;
import util.Variables;

import java.util.*;

public class Solver {
  // SOLUTION
  private Schedule schedule;
  private int patientCost, equityCost;
  private List<Solution> solutionArchive;
  private Queue<Rectangle> rectangleArchive;
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
    int load =
        schedule.getTotalDailyEquityCost() + schedule.getTotalDepartmentEquityCost() + capacity;
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
            + "\nLoad Cost\t\t:" + (equityCost - violationCost)
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
    equityCost = 0;
    insertInitialPatients();
    assignRandomRooms();
    patientCost += schedule.getDynamicGenderViolations() * Variables.GENDER_PEN;
    for (int i = 0; i < DateConverter.getTotalHorizon(); i++)
      schedule.calculateDailyEquityCost(i);
    for (int i = 0; i < DepartmentList.getNumberOfDepartments(); i++)
      schedule.calculateDepartmentEquityCost(i);
    equityCost += schedule.getTotalDailyEquityCost();
    equityCost += schedule.getTotalDepartmentEquityCost();
    equityCost += schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
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
      p.setDelay(0);
      int room = p.getRandomFeasibleRoom();
      for (int i = 0; i < p.getStayLength(); i++)
        schedule.assignPatient(p, room, i + p.getOriginalAD());
      patientCost += p.getRoomCost(room);
    }
  }

  // Local search
  public void simpleHBS() {
    System.out.println(new Date() + ": Started");
    solutionArchive = new LinkedList<>();
    rectangleArchive = new PriorityQueue<>();

    Solution sol = optimizePatientCost(null, Integer.MAX_VALUE);
    solutionArchive.add(sol);
    rectangleArchive.add(new Rectangle(
        new Point(sol.getPatientCost(), sol.getEquityCost()),
        new Point((int) (Variables.TRADEOFF * sol.getPatientCost()), Variables.WE_MIN)));
    System.out.println(new Date() + ": Found first solution");
    printArchiveInfo();

    while (!rectangleArchive.isEmpty()) {
      int c = rectangleArchive.peek().c;
      System.out.println(new Date() + ": Looking for a solution with c = " + c);
      sol = optimizePatientCost(null, c);
      updateArchives(sol, c);
      printArchiveInfo();
    }
  }

  public void iteratedHBS() {
    solutionArchive = new LinkedList<>();
    rectangleArchive = new PriorityQueue<>();

    Solution sol = optimizePatientCost(null, Integer.MAX_VALUE);
    solutionArchive.add(sol);
    rectangleArchive.add(new Rectangle(
        new Point(sol.getPatientCost(), sol.getEquityCost()),
        new Point((int) (Variables.TRADEOFF * sol.getPatientCost()), Variables.WE_MIN)));
    System.out.println(new Date() + ": Found first solution");
    printArchiveInfo();

    while (!rectangleArchive.isEmpty()) {
      int c = rectangleArchive.peek().c;
      System.out.println(new Date() + ": Looking for a solution with c = " + c);
      sol = optimizePatientCost(getNearestSolution(c), c);
      updateArchives(sol, c);
      printArchiveInfo();
    }
  }


  public void addSolution(Solution sol) {
    solutionArchive.add(sol);
    Collections.sort(solutionArchive);
    List<Solution> dominatedSolutions = new ArrayList<>();
    for (Solution s : solutionArchive)
      if (sol.strictlyDominates(s)) dominatedSolutions.add(s);
    solutionArchive.removeAll(dominatedSolutions);
  }

  public void updateArchives(Solution s, int c) {
    // TODO: If current solution is non-dominated, add it to approximation set, remove dominated
    //  solutions and split its rectangle.
    //  If the solution is out of bounds of the original rectangle, remove dominated rectangles.
    //  If it is dominated, remove its lower rectangle

    if (s == null || isDominated(s.getPatientCost(), s.getEquityCost())) {
      Rectangle r = rectangleArchive.poll();
      assert r != null;
      r.setLr(new Point(r.getRight(), c));
      rectangleArchive.offer(r);
    } else {
      addSolution(s);

      Rectangle horizontalHit = findRectangleOnX(patientCost);
      if (horizontalHit != null) {
        int top = Math.max(horizontalHit.getBottom(), c);
        Point updatedPoint = new Point(patientCost, top);
        if (horizontalHit.getTop() - top > Variables.DELTA)
          rectangleArchive.offer(new Rectangle(horizontalHit.getUl(), updatedPoint));
      }

      Rectangle verticalHit = findRectangleOnY(equityCost);
      if (verticalHit != null) {
        int left = Math.max(patientCost, verticalHit.getLeft());
        Point updatedPoint = new Point(left, equityCost);
        if (equityCost - verticalHit.getBottom() > Variables.DELTA)
          rectangleArchive.offer(new Rectangle(updatedPoint, verticalHit.getLr()));
      }

      boolean inBounds = horizontalHit == rectangleArchive.peek() && horizontalHit == verticalHit;
      rectangleArchive.remove(horizontalHit);
      rectangleArchive.remove(verticalHit);
      if (!inBounds) removeRectanglesDominatedBy(s);
    }
  }

  public void removeRectanglesDominatedBy(Solution s) {
    List<Rectangle> dominatedRectangles = new ArrayList<>();
    for (Rectangle r : rectangleArchive)
      if (r.isDominatedBy(s))
        dominatedRectangles.add(r);
    rectangleArchive.removeAll(dominatedRectangles);
  }

  public void printArchiveInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Current solution\t( ");
    sb.append(patientCost).append(",").append(equityCost).append(" )\n");

    sb.append("Area archive\t\t{ ");
    for (Rectangle r : rectangleArchive)
      sb.append(r.area).append(", ");
    sb.deleteCharAt(sb.length() - 2);

    sb.append("}\nRectangle x-value's\t{ ");
    for (Rectangle r : rectangleArchive)
      sb.append("(").append(r.getLeft()).append(",").append(r.getRight()).append("), ");
    sb.deleteCharAt(sb.length() - 2);

    sb.append("}\nRectangle y-value's\t{ ");
    for (Rectangle r : rectangleArchive)
      sb.append("(").append(r.getBottom()).append(",").append(r.getTop()).append("), ");
    sb.deleteCharAt(sb.length() - 2);

    sb.append("}\nSolution archive\t{ ");
    for (Solution s : solutionArchive)
      sb.append("(").append(s.getPatientCost()).append(",").append(s.getEquityCost()).append("), ");
    sb.deleteCharAt(sb.length() - 2);
    sb.append("}\n");
    System.out.println(sb);
  }

  public boolean isDominated(int pc, int ec) {
    for (Solution s : solutionArchive)
      if (s.strictlyDominates(pc, ec)) return true;
      else if (s.getPatientCost() < pc) return false;
    return false;
  }

  public boolean isEfficient(int pc, int ec) {
    if (solutionArchive.isEmpty()) return true;

    for (int i = 1; i < solutionArchive.size(); i++)
      if (solutionArchive.get(i).getEquityCost() >= ec &&
          solutionArchive.get(i - 1).getEquityCost() < ec)
        return pc < solutionArchive.get(i).getPatientCost();

    return solutionArchive.get(0).getEquityCost() > ec ||
        solutionArchive.get(solutionArchive.size() - 1).getPatientCost() > pc;
  }

  public Rectangle findRectangleOnX(int pc) {
    for (Rectangle r : rectangleArchive)
      if (r.getRight() >= pc && r.getLeft() <= pc) return r;
    return null;
  }

  public Rectangle findRectangleOnY(int we) {
    for (Rectangle r : rectangleArchive)
      if (r.getTop() >= we && r.getBottom() <= we) return r;
    return null;
  }

  public Solution getNearestSolution(int c) {
    if (solutionArchive.isEmpty()) return null;
    if (solutionArchive.size() == 1 || solutionArchive.get(0).getEquityCost() > c)
      return solutionArchive.get(0);
    for (int i = 1; i < solutionArchive.size(); i++)
      if (solutionArchive.get(i).getEquityCost() >= c &&
          solutionArchive.get(i - 1).getEquityCost() <= c)
        return (solutionArchive.get(i).getEquityCost() - c >
            c - solutionArchive.get(i - 1).getEquityCost())
            ? solutionArchive.get(i - 1)
            : solutionArchive.get(i);
    System.out.println("This should be unreachable...");
    return null;
  }


  public Solution optimizePatientCost(Solution sol, int c) {
    loadInitialSchedule(sol);
    randomizeSchedule();
    double penaltyCoefficient = sol == null
        ? Variables.PENALTY_COEFFICIENT
        : sol.getPenaltyCoefficient();
    double temp = Variables.T_START;
    int repairTries = 0;

    while (temp > Variables.T_STOP) {
      for (int i = 0; i < Variables.T_ITERATIONS; i++) {
        executeNewMove();
        double savings = lastMove.get("patient_savings") + penaltyCoefficient *
            (Math.max(c, equityCost) - Math.max(c, equityCost - lastMove.get("load_savings")));
        if (savings > 0 || Math.random() < Math.exp(savings / temp)) acceptMove();
        else undoLastMove();
      }

      temp *= Variables.ALPHA;
      adjustEquityCost();
      if (equityCost >= 0.95 * c) penaltyCoefficient *= 1.2;
      else if (equityCost <= 1.05 * c) penaltyCoefficient *= 0.85;

      if (isEfficient(patientCost, equityCost))
        addSolution(new Solution(schedule, patientCost, equityCost, temp, penaltyCoefficient));

      if (temp <= Variables.T_STOP && equityCost > c && repairTries < 2) {
        penaltyCoefficient *= 10;
        temp /= Variables.ALPHA;
        repairTries++;
        System.out.println("Repairing solution...");
      }
    }

    if (equityCost > c) return null;
    penaltyCoefficient /= Math.pow(10, repairTries);
    return new Solution(schedule, patientCost, equityCost, temp, penaltyCoefficient);
  }

  public void loadInitialSchedule(Solution sol) {
    if (sol == null) {
      schedule = new Schedule();
      initSchedule();
    } else {
      schedule = sol.copySchedule();
      patientCost = sol.getPatientCost();
      equityCost = sol.getEquityCost();
      sol.loadPatientConfiguration();
    }
    System.out.println("Loaded solution \t( " + patientCost + ", " + equityCost + " )");
  }

  public void randomizeSchedule() {
    for (int i = 0; i < Variables.RANDOMIZATION_ITERATIONS; i++) {
      executeNewMove();
      acceptMove();
    }
    adjustEquityCost();
    System.out.println("Randomized solution\t( " + patientCost + ", " + equityCost + " )");
  }

  public void adjustEquityCost() {
    equityCost = schedule.getTotalDailyEquityCost()
        + schedule.getTotalDepartmentEquityCost()
        + schedule.getCapacityViolations() * Variables.CAP_VIOL_PEN;
  }


  // Move functionality
  public void acceptMove() {
    lastMove.put("accepted", 1);
    patientCost -= lastMove.get("patient_savings");
    equityCost -= lastMove.get("load_savings");
  }

  public Map<String, Integer> generateMove() {
    int random = (int) (Math.random() * 100);
    int type;
    if (random < Variables.PCR) type = 0;
    else if (random - Variables.PCR < Variables.PSR) type = 1;
    else if (random - Variables.PCR - Variables.PSR < Variables.PSHA) type = 2;
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

    if (!firstPatient.hasFeasibleRoom(secondPatient.getLastRoom()) ||
        !secondPatient.hasFeasibleRoom(firstPatient.getLastRoom())) {
      System.out.println("No overlapping feasible room was selected");
    }

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

    if (!firstPatient.hasFeasibleRoom(secondPatient.getLastRoom()) ||
        !secondPatient.hasFeasibleRoom(firstPatient.getLastRoom())) {
      System.out.println("No overlapping feasible room was selected");
    }

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
      if (schedule.hasSingleDynamicGenderViolation(patient.getLastRoom(), day,
          patient.getGender()))
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
      savings += schedule.getDepartmentEquityCost(dep);
      schedule.calculateDepartmentEquityCost(dep);
      savings -= schedule.getDepartmentEquityCost(dep);
    }
    return savings;
  }

  public double getDailyLoadSavings(Set<Integer> days) {
    double savings = 0;
    for (int day : days) {
      savings += schedule.getDailyEquityCost(day);
      schedule.calculateDailyEquityCost(day);
      savings -= schedule.getDailyEquityCost(day);
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
    lastMove.put("total_load_cost", equityCost);
    lastMove.put("load_cost", equityCost - capacityViolationCost);
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
    for (int day : days) schedule.calculateDailyEquityCost(day);
    int fDep = lastMove.get("first_department");
    int sDep = lastMove.getOrDefault("second_department", -1);
    schedule.calculateDepartmentEquityCost(fDep);
    if (fDep != sDep && sDep != -1)
      schedule.calculateDepartmentEquityCost(sDep);
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
