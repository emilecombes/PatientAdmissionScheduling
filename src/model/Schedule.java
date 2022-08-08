package model;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import util.DateConverter;
import util.Variables;

import java.util.*;

public class Schedule {
  private final Set<Integer>[][] schedule;
  private final Map<Integer, List<Map<String, Integer>>> dynamicGenderCount;
  private int capacityViolations;
  private final double[][] loadMatrix;
  private final double[] avgDailyLoads;
  private final double[] dailyLoadCosts;
  private final double[] avgDepLoads;
  private final double[] depLoadCosts;

  public Schedule() {
    int horizonLength = DateConverter.getTotalHorizon();
    int departmentCount = DepartmentList.getNumberOfDepartments();
    schedule = new Set[DepartmentList.getNumberOfRooms()][horizonLength];
    dynamicGenderCount = new HashMap<>();
    capacityViolations = 0;

    loadMatrix = new double[departmentCount][horizonLength];
    avgDailyLoads = new double[horizonLength];
    dailyLoadCosts = new double[horizonLength];
    avgDepLoads = new double[departmentCount];
    depLoadCosts = new double[departmentCount];

    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++) {
      for (int j = 0; j < horizonLength; j++)
        schedule[i][j] = new HashSet<>();
      if (DepartmentList.getRoom(i).hasGenderPolicy("SameGender")) {
        dynamicGenderCount.put(i, new ArrayList<>());
        for (int j = 0; j < horizonLength; j++) {
          dynamicGenderCount.get(i).add(new HashMap<>());
          dynamicGenderCount.get(i).get(j).put("Male", 0);
          dynamicGenderCount.get(i).get(j).put("Female", 0);
        }
      }
    }
  }

  public Schedule(Set<Integer>[][] s, Map<Integer, List<Map<String, Integer>>> d, int c,
                  double[][] lm, double[] aDay, double[] dayL, double[] aDep, double[] depL) {
    schedule = s;
    dynamicGenderCount = d;
    capacityViolations = c;
    loadMatrix = lm;
    avgDailyLoads = aDay;
    dailyLoadCosts = dayL;
    avgDepLoads = aDep;
    depLoadCosts = depL;
  }

  public boolean isFeasible() {
    return capacityViolations == 0;
  }

  public int getCapacityViolations() {
    return capacityViolations;
  }

  // Getting patients
  public Set<Integer> getPatients(int room, int day) {
    return schedule[room][day];
  }

  public Set<Integer> getSwappablePatients(int searchRoom, int swapRoom, int day) {
    Set<Integer> candidates = new HashSet<>(getPatients(searchRoom, day));
    Set<Integer> badCandidates = new HashSet<>();
    for (int pat : candidates)
      if (!PatientList.getPatient(pat).hasFeasibleRoom(swapRoom))
        badCandidates.add(pat);
    candidates.removeAll(badCandidates);
    return candidates;
  }

  public Patient getSwapRoomPatient(Patient pat) {
    return Variables.EXHAUSTIVE
        ? getExhaustiveSwapRoomPatient(pat)
        : getFastSwapPatient(pat, pat.getAdmission(), pat.getDischarge(), false);
  }

  public Patient getSwapAdmissionPatient(Patient pat) {
    return Variables.EXHAUSTIVE
        ? getExhaustiveSwapAdmissionPatient(pat)
        : getFastSwapPatient(pat, 0, DateConverter.getTotalHorizon(), true);
  }

  public Patient getFastSwapPatient(Patient pat, int startDate, int endDate, boolean shift) {
    Set<Integer> rooms = new HashSet<>(pat.getFeasibleRooms());
    rooms.remove(pat.getLastRoom());
    if (rooms.isEmpty()) return null;
    List<Patient> candidates = new ArrayList<>();
    int loops = Math.min(
        Variables.SWAP_LOOPS,
        pat.getFeasibleRooms().size() * 4 * (endDate - startDate)
    );
    for (int i = 0; i < loops; i++) {
      int room = rooms.stream().skip(new Random().nextInt(rooms.size())).findFirst().orElse(-1);
      int day = startDate + new Random().nextInt(endDate - startDate);
      Set<Integer> patients = getPatients(room, day);
      if (patients.isEmpty()) continue;
      int sp = patients.stream().skip(new Random().nextInt(patients.size())).findFirst().orElse(-1);
      Patient swapPat = PatientList.getPatient(sp);
      if (swapPat.hasFeasibleRoom(pat.getLastRoom()) && (!shift ||
          pat.isAdmissibleOn(swapPat.getAdmission()) && swapPat.isAdmissibleOn(pat.getAdmission())))
        candidates.add(swapPat);
    }

    return candidates.isEmpty() ? null : candidates.get(new Random().nextInt(candidates.size()));
  }

  public Patient getExhaustiveSwapRoomPatient(Patient firstPatient) {
    int swapRoom = firstPatient.getLastRoom();
    Set<Integer> rooms = new HashSet<>(firstPatient.getFeasibleRooms());
    rooms.remove(swapRoom);

    Set<Integer> candidates = new HashSet<>();
    for (int day = firstPatient.getAdmission(); day < firstPatient.getDischarge(); day++)
      for (int searchRoom : rooms)
        candidates.addAll(getSwappablePatients(searchRoom, swapRoom, day));

    if (candidates.isEmpty()) return null;
    List<Integer> swapPatients = new ArrayList<>(candidates);
    Collections.shuffle(swapPatients);
    return PatientList.getPatient(swapPatients.get(0));
  }

  public Patient getExhaustiveSwapAdmissionPatient(Patient firstPatient) {
    int firstAdmission = firstPatient.getAdmission();
    int swapRoom = firstPatient.getLastRoom();
    Set<Integer> rooms = new HashSet<>(firstPatient.getFeasibleRooms());
    rooms.remove(swapRoom);

    Set<Integer> candidates = new HashSet<>();
    for (int day = firstPatient.getOriginalAD(); day <= firstPatient.getMaxAdm(); day++)
      for (int searchRoom : rooms)
        candidates.addAll(getSwappablePatients(searchRoom, swapRoom, day));

    Set<Integer> badCandidates = new HashSet<>();
    for (int candidate : candidates) {
      if (!PatientList.getPatient(candidate).isAdmissibleOn(firstAdmission)
          || !firstPatient.isAdmissibleOn(PatientList.getPatient(candidate).getAdmission()))
        badCandidates.add(candidate);
    }
    candidates.removeAll(badCandidates);

    if (candidates.isEmpty()) return null;
    List<Integer> swapPatients = new ArrayList<>(candidates);
    Collections.shuffle(swapPatients);
    return PatientList.getPatient(swapPatients.get(0));
  }


  // Schedule functions
  public void assignPatient(Patient pat, int room, int day) {
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
    if (dynamicGenderCount.containsKey(room))
      incrementGenderCount(room, day, pat.getGender());
    if (schedule[room][day].size() > DepartmentList.getRoom(room).getCapacity())
      capacityViolations++;

    int dep = DepartmentList.getDepartment(DepartmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / DepartmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] += delta;
    incrementAverageDailyLoad(day, delta / DepartmentList.getNumberOfDepartments());
    incrementAverageDepartmentLoad(dep, delta / DateConverter.getTotalHorizon());
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getRoom(day);
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
    if (dynamicGenderCount.containsKey(room))
      decrementGenderCount(room, day, pat.getGender());
    if (schedule[room][day].size() >= DepartmentList.getRoom(room).getCapacity())
      capacityViolations--;

    int dep = DepartmentList.getDepartment(DepartmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / DepartmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] -= delta;
    decrementAverageDailyLoad(day, delta / DepartmentList.getNumberOfDepartments());
    decrementAverageDepartmentLoad(dep, delta / DateConverter.getTotalHorizon());
  }


  // Patient cost related functions
  public int getGenderCount(int room, int day, String gender) {
    return dynamicGenderCount.get(room).get(day).get(gender);
  }

  public int getOtherGenderCount(int room, int day, String gender) {
    String otherGender = (gender.equals("Male")) ? "Female" : "Male";
    return getGenderCount(room, day, otherGender);
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r : dynamicGenderCount.keySet())
      for (int d = 0; d < DateConverter.getTotalHorizon(); d++)
        if (getDynamicGenderViolations(r, d) > 0)
          violations++;
    return violations;
  }

  public int getDynamicGenderViolations(int room, int day) {
    return Math.min(getGenderCount(room, day, "Male"), getGenderCount(room, day, "Female"));
  }

  public boolean hasSingleDynamicGenderViolation(int room, int day, String gender) {
    if (!dynamicGenderCount.containsKey(room))
      return false;
    return getDynamicGenderViolations(room, day) == 1 && getGenderCount(room, day, gender) == 1;
  }

  public boolean isFirstDynamicGenderViolation(int room, int day, String gender) {
    if (!dynamicGenderCount.containsKey(room))
      return false;
    return getDynamicGenderViolations(room, day) == 0 && getOtherGenderCount(room, day, gender) > 0;
  }

  public void incrementGenderCount(int room, int day, String gender) {
    dynamicGenderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) + 1);
  }

  public void decrementGenderCount(int room, int day, String gender) {
    dynamicGenderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) - 1);
  }


  // Equity cost related functions
  public double getLoad(int dep, int day) {
    return loadMatrix[dep][day];
  }

  public double getDepartmentEquityCost(int dep) {
    return depLoadCosts[dep];
  }

  public int getTotalDepartmentEquityCost() {
    return (int) Arrays.stream(depLoadCosts).sum();
  }

  public void calculateDepartmentEquityCost(int dep) {
    if (dep == -1) return;
    double cost = 0;
    for (int i = 0; i < DateConverter.getTotalHorizon(); i++)
      cost += Math.pow(avgDepLoads[dep] - loadMatrix[dep][i], 2);
    depLoadCosts[dep] = cost;
  }

  public void incrementAverageDepartmentLoad(int dep, double delta) {
    avgDepLoads[dep] += delta;
  }

  public void decrementAverageDepartmentLoad(int dep, double delta) {
    avgDepLoads[dep] -= delta;
  }

  public double getDailyEquityCost(int day) {
    return dailyLoadCosts[day];
  }

  public int getTotalDailyEquityCost() {
    return (int) Arrays.stream(dailyLoadCosts).sum();
  }

  public void calculateDailyEquityCost(int day) {
    double cost = 0;
    for (int i = 0; i < DepartmentList.getNumberOfDepartments(); i++)
      cost += Math.pow(avgDailyLoads[day] - loadMatrix[i][day], 2);
    dailyLoadCosts[day] = cost;
  }

  public void incrementAverageDailyLoad(int day, double delta) {
    avgDailyLoads[day] += delta;
  }

  public void decrementAverageDailyLoad(int day, double delta) {
    avgDailyLoads[day] -= delta;
  }


  // Copy functions
  public Schedule getCopy() {
    return new Schedule(copySchedule(), copyDynamicGenderViolations(),
        capacityViolations, copyLoadMatrix(),
        copyADayL(), copyDayLC(),
        copyADepL(), copyDepLC());
  }

  public Set<Integer>[][] copySchedule() {
    Set<Integer>[][] copy = new Set[schedule.length][schedule[0].length];
    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++)
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        copy[i][j] = new HashSet<>();

    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++)
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        for (int k : schedule[i][j])
          copy[i][j].add(k);

    return copy;
  }

  public Map<Integer, List<Map<String, Integer>>> copyDynamicGenderViolations() {
    Map<Integer, List<Map<String, Integer>>> copy = new HashMap<>();
    for (int k : dynamicGenderCount.keySet()) {
      List<Map<String, Integer>> counts = new ArrayList<>();
      for (int i = 0; i < dynamicGenderCount.get(k).size(); i++) {
        Map<String, Integer> map = new HashMap<>();
        for (String g : dynamicGenderCount.get(k).get(i).keySet())
          map.put(g, dynamicGenderCount.get(k).get(i).get(g));
        counts.add(map);
      }
      copy.put(k, counts);
    }
    return copy;
  }

  public double[][] copyLoadMatrix() {
    double[][] loads = new double[loadMatrix.length][loadMatrix[0].length];
    for (int i = 0; i < loadMatrix.length; i++)
      System.arraycopy(loadMatrix[i], 0, loads[i], 0, loadMatrix[0].length);
    return loads;
  }

  public double[] copyADayL() {
    double[] t = new double[avgDailyLoads.length];
    System.arraycopy(avgDailyLoads, 0, t, 0, avgDailyLoads.length);
    return t;
  }

  public double[] copyDayLC() {
    double[] t = new double[dailyLoadCosts.length];
    System.arraycopy(dailyLoadCosts, 0, t, 0, dailyLoadCosts.length);
    return t;
  }

  public double[] copyADepL() {
    double[] t = new double[avgDepLoads.length];
    System.arraycopy(avgDepLoads, 0, t, 0, avgDepLoads.length);
    return t;
  }

  public double[] copyDepLC() {
    double[] t = new double[depLoadCosts.length];
    System.arraycopy(depLoadCosts, 0, t, 0, depLoadCosts.length);
    return t;
  }
}