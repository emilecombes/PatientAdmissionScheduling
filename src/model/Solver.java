package model;

import util.DateConverter;
import util.JSONParser;
import util.Variables;

import java.util.*;

// TODO: Randomization should be proportional to solution temperature
// TODO: Cost-optimal solutions shouldn't have p=1
// TODO: If R contains known solutions, searching for a solution there shouldn't yield null

public class Solver {
  // SOLUTION
  private Schedule schedule;
  private int patientCost, patientSavings, c;
  private double equityCost, equitySavings, pen, decrease, increase;
  private List<Solution> solutionArchive;
  private Queue<Rectangle> rectangleArchive;
  // MOVES
  private final List<Map<String, Integer>> generatedMoves;
  private Map<String, Integer> lastMove;
  private final JSONParser jsonParser;

  public Solver() {
    generatedMoves = new ArrayList<>();
    jsonParser = new JSONParser();
  }

  // Functions to remove
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
    schedule = new Schedule();
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

  public void loadSolution(Solution sol) {
    schedule = sol.copySchedule();
    patientCost = sol.getPatientCost();
    equityCost = sol.getEquityCost();
    pen = sol.getPenaltyCoefficient();
    sol.loadPatientConfiguration();
    randomizeSchedule();
    if (equityCost > c) repairSchedule(Variables.T_START, Variables.SUB_ITERATIONS);
  }

  public void initRectangleArchive() {
    rectangleArchive = new PriorityQueue<>();
    Variables.PC_MAX = (int) (Variables.TRADEOFF * solutionArchive.get(0).getPatientCost());
    rectangleArchive.add(new Rectangle(
        new Point(patientCost, (int) equityCost),
        new Point(Variables.PC_MAX, Variables.WE_MIN)));
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

  public Solution getNearestSolution() {
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
    return solutionArchive.get(solutionArchive.size() - 1);
  }

  public Solution getNearestFeasibleSolution() {
    if (solutionArchive.size() == 1) return solutionArchive.get(0);
    for (int i = 1; i < solutionArchive.size(); i++)
      if (solutionArchive.get(i).getEquityCost() > c &&
          solutionArchive.get(i - 1).getEquityCost() <= c)
        return solutionArchive.get(i - 1);
    return null;
  }


  // Local search
  public List<Solution> hbs() {
    System.out.println("Start:\t\t\t" + new Date());
    writeStart(-1);
    explore(5);
    writeArchives();
    System.out.println("Initial problem:" + new Date());
    int i = 0;
    while (!rectangleArchive.isEmpty() && i < Variables.MAX_HBS_ITERATIONS) {
      Rectangle r = rectangleArchive.peek();
      c = r.getC();
      decrease = r.getDecreaseLimit();
      increase = r.getIncreaseLimit();
      writeStart(c);
      optimizeSubproblem();
      System.out.println("Subproblem:\t\t" + new Date());
      if (equityCost > c || isDominated(patientCost, (int) equityCost)) r.setBottom(c);
      else {
        Rectangle rx = findRectangleOnX(patientCost);
        if (rx != null) {
          Point newLR = new Point(patientCost, Math.max(rx.getBottom(), c));
          if (rx.getTop() - newLR.y > Variables.DELTA)
            rectangleArchive.offer(new Rectangle(rx.getUl(), newLR));
        }
        Rectangle ry = findRectangleOnY((int) equityCost);
        if (ry != null) {
          Point newUL = new Point(Math.max(ry.getLeft(), patientCost), (int) equityCost);
          if (ry.getTop() - newUL.y > Variables.DELTA)
            rectangleArchive.offer(new Rectangle(newUL, ry.getLr()));
        }
        removeAndUpdateRectangleArchive(rx, ry);
      }
      writeArchives();
      i++;
    }
    writeEnd();
    return solutionArchive;
  }

  public void removeAndUpdateRectangleArchive(Rectangle x, Rectangle y) {
    boolean inBounds = x == y && x == rectangleArchive.poll();
    if (!inBounds) {
      List<Rectangle> dominatedRectangles = new ArrayList<>();
      for (Rectangle r : rectangleArchive)
        if (r.getBottom() >= equityCost && r.getLeft() >= patientCost)
          dominatedRectangles.add(r);
      rectangleArchive.removeAll(dominatedRectangles);
      if (!dominatedRectangles.isEmpty())
        System.out.println(dominatedRectangles.size() + " rectangles were dominated");
    }
  }

  public void exploreSearchSpace() {
    solutionArchive = new LinkedList<>();
    initSchedule();
    c = Integer.MAX_VALUE;
    pen = Variables.PENALTY_COEFFICIENT;
    increase = Integer.MAX_VALUE;
    decrease = 0;
    performSA(Variables.INIT_ITERATIONS);
    writeCurrentSolution("final_solution");
    initRectangleArchive();
  }

  public void explore(int scoutNumber) {
    solutionArchive = new LinkedList<>();
    rectangleArchive = new PriorityQueue<>();
    c = 0;
    increase = Integer.MAX_VALUE;
    decrease = 0;
    for (int i = 0; i < scoutNumber; i++) {
      pen = Math.pow(2, i) - 1;
      writeStart(i-1);
      initSchedule();
      performSA(Variables.INIT_ITERATIONS);
      writeCurrentSolution("final_solution");
      writeArchives();
    }
    writeEnd();
    initRectangleArchive();
  }

  public void optimizeSubproblem() {
    Solution sol = getNearestSolution();
    loadSolution(sol);
    if (equityCost > c && sol.getEquityCost() > c && solutionArchive.indexOf(sol) != 0) {
      sol = getNearestFeasibleSolution();
      loadSolution(sol);
    }
    writeHarvestedSolution(sol);
    writeCurrentSolution("initial_solution");
    performSA(Variables.SUB_ITERATIONS);
    writeCurrentSolution("natural_solution");
    writeCurrentSolution("final_solution");
  }

  public void randomizeSchedule() {
    for (int i = 0; i < Variables.RND_ITERATIONS; i++) {
      executeNewMove();
      acceptMove();
    }
  }

  public void repairSchedule(double temp, int iter) {
    int e = (int) equityCost;
    int p = patientCost;
    int tries = 1;
    while (equityCost > c && tries <= 2) {
      for (int i = 0; i < iter; i++) {
        executeNewMove();
        double savings = patientSavings +
            pen * Math.pow(Variables.REPAIR_INCREASE, tries) *
                (Math.max(c, equityCost) - Math.max(c, equityCost - equitySavings));
        if (savings > 0 || Math.random() < Math.exp(savings / temp)) acceptMove();
        else undoLastMove();
      }
      tries++;
    }
  }

  public void performSA(int iter) {
    int repairCalls = 0;
    int subsequentRepairs = 0;
    int maxSubsequentRepairs = 0;
    boolean subsequent = false;
    double temp = Variables.T_START;
    while (temp > Variables.T_STOP) {
      for (int i = 0; i < iter; i++) {
        executeNewMove();
        double savings = patientSavings + pen *
            (Math.max(c, equityCost) - Math.max(c, equityCost - equitySavings));
        if (savings > 0 || Math.random() < Math.exp(savings / temp)) acceptMove();
        else undoLastMove();
      }
      updatePenalty();
      if (isEfficient(patientCost, (int) equityCost))
        addSolution(new Solution(schedule, patientCost, equityCost, pen));
      if (equityCost > c && c != 0) {
        repairSchedule(temp, iter);
        repairCalls++;
        if (subsequent) subsequentRepairs++;
        else subsequent = true;
      } else {
        maxSubsequentRepairs = Math.max(maxSubsequentRepairs, subsequentRepairs);
        subsequent = false;
      }
      temp *= Variables.ALPHA;
    }
    System.out.println(
        "Repair calls: " + repairCalls + "/" + Variables.SUBPROBLEM_TOTAL_ITERATIONS / iter +
            ", Subsequent: " + maxSubsequentRepairs + ", c = " + c + ", p = " + pen);
  }

  public void tuning() {
    initSchedule();
    double temp = Variables.T_START;
    while (temp > Variables.T_STOP) {
      for (int i = 0; i < Variables.INIT_ITERATIONS; i++) {
        executeNewMove();
        if (patientSavings > 0 || Math.random() < Math.exp(patientSavings / temp)) acceptMove();
        else undoLastMove();
      }
      temp *= Variables.ALPHA;
    }
    System.out.println(patientCost);
  }

  public void updatePenalty() {
    if (equityCost >= increase) pen *= Variables.PENALTY_INCREASE;
    else if (equityCost <= decrease) pen *= Variables.PENALTY_DECREASE;
  }

  public void addSolution(Solution sol) {
    // TODO: Remove dominated solutions at the end of an iteration
    solutionArchive.add(sol);
    Collections.sort(solutionArchive);
    List<Solution> dominatedSolutions = new ArrayList<>();
    for (Solution s : solutionArchive)
      if (sol.strictlyDominates(s)) dominatedSolutions.add(s);
    solutionArchive.removeAll(dominatedSolutions);
  }

  public boolean isDominated(int pc, int ec) {
    for (Solution s : solutionArchive)
      if (s.strictlyDominates(pc, ec)) return true;
      else if (s.getPatientCost() < pc) return false;
    return false;
  }

  public boolean isEfficient(int pc, int ec) {
    // TODO: make this more efficient
    if (solutionArchive.isEmpty()) return true;
    if (solutionArchive.get(0).getEquityCost() > ec) return true;
    if (solutionArchive.get(0).getEquityCost() == ec)
      return solutionArchive.get(0).getPatientCost() > pc;

    for (int i = 1; i < solutionArchive.size(); i++) {
      Solution lowerEquityCost = solutionArchive.get(i - 1);
      Solution higherEquityCost = solutionArchive.get(i);
      if (higherEquityCost.getEquityCost() >= ec && lowerEquityCost.getEquityCost() < ec)
        return pc <= higherEquityCost.getPatientCost() &&
            !(higherEquityCost.getEquityCost() == ec && higherEquityCost.getPatientCost() == pc);
    }

    return solutionArchive.get(solutionArchive.size() - 1).getPatientCost() > pc;
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


  // Move functionality
  public void acceptMove() {
    lastMove.put("accepted", 1);
    patientCost -= patientSavings;
    equityCost -= equitySavings;
  }

  public Map<String, Integer> generateMove() {
    int random = (int) (Math.random() * 100);
    int type;
    if (random < Variables.PCR) type = 0;
    else if (random < Variables.PSR) type = 1;
    else if (random < Variables.PSHA) type = 2;
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

  public void addSavingsToMove(int capacitySavings, int ps, double ls) {
    int capacityViolations = schedule.getCapacityViolations();
    int capacityViolationCost = capacityViolations * Variables.CAP_VIOL_PEN;
    lastMove.put("capacity_violations", capacityViolations);
    lastMove.put("capacity_violation_savings", capacitySavings);
    lastMove.put("total_patient_cost", ps);
    lastMove.put("patient_cost", patientCost - capacityViolationCost);
    lastMove.put("patient_savings", patientSavings);
    lastMove.put("total_load_cost", (int) equityCost);
    lastMove.put("load_cost", (int) equityCost - capacityViolationCost);
    lastMove.put("load_savings", (int) ls);
    equitySavings = ls;
    patientSavings = ps;
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
    equitySavings = getDailyLoadSavings(affectedDays)
        + getDepartmentLoadSavings()
        + assignmentSavings[1];

    addSavingsToMove(assignmentSavings[1], patientSavings, equitySavings);
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

  public void writeStart(int c) {
    StringBuilder sb = new StringBuilder();
    if (c == -1) sb.append("{\n\"iterations\":[\n");
    sb.append("{\n\"time\":\"").append(new Date()).append("\",\n");
    if (c != -1) sb.append("\"c\":\"").append(c).append("\",\n");
    jsonParser.write(sb.toString());
  }

  public void writeHarvestedSolution(Solution sol) {
    jsonParser.write("\"harvested_solution\":" + sol + ",\n");
  }

  public void writeCurrentSolution(String key) {
    String sb = "\"" + key + "\":{\"patient_cost\":\"" + patientCost +
        "\"," + "\"equity_cost\":\"" + equityCost +
        "\"," + "\"penalty_coefficient\":\"" + pen + "\"},\n";
    jsonParser.write(sb);
  }

  public void writeArchives() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"rectangle_archive\":[");
    for (Rectangle r : rectangleArchive)
      sb.append(r.toString()).append(",\n");
    if (!rectangleArchive.isEmpty()) sb.deleteCharAt(sb.lastIndexOf(","));

    sb.append("],\n\"solution_archive\":[");
    for (Solution s : solutionArchive)
      sb.append(s.toString()).append(",\n");
    if (!solutionArchive.isEmpty()) sb.deleteCharAt(sb.lastIndexOf(","));
    sb.append("]\n},");
    jsonParser.write(sb.toString());
  }

  public void writeEnd() {
    jsonParser.write("]\n}");
    jsonParser.closeWriter();
  }
}
