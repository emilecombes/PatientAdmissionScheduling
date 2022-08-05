package model;

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

  public Schedule(Solution s) {
    schedule = s.getSchedule();
    dynamicGenderCount = s.getDynamicGenderCount();
    loadMatrix = s.getLoadMatrix();
    avgDailyLoads = s.getAverageDailyLoads();
    dailyLoadCosts = s.getDailyLoadCosts();
    avgDepLoads = s.getAverageDepartmentLoads();
    depLoadCosts = s.getDepartmentLoadCosts();
  }
  
  public boolean isFeasible() {
    return capacityViolations == 0;
  }

  public int getCapacityViolations() {
    return capacityViolations;
  }

  public double[][] getLoadMatrix() {
    return loadMatrix;
  }

  public double[] getDailyLoadCosts() {
    return dailyLoadCosts;
  }

  public double[] getAverageDailyLoads() {
    return avgDailyLoads;
  }

  public double[] getDepartmentLoadCosts() {
    return depLoadCosts;
  }

  public double[] getAverageDepartmentLoads() {
    return avgDepLoads;
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
        : getFastSwapPatient(pat, pat.getAdmission(), pat.getDischarge());
  }

  public Patient getSwapAdmissionPatient(Patient pat) {
    return Variables.EXHAUSTIVE
        ? getExhaustiveSwapAdmissionPatient(pat)
        : getFastSwapPatient(pat, 0, DateConverter.getTotalHorizon());
  }

  public Patient getFastSwapPatient(Patient pat, int startDate, int endDate) {
    Set<Integer> rooms = new HashSet<>(pat.getFeasibleRooms());
    rooms.remove(pat.getLastRoom());
    if(rooms.isEmpty()) return null;
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
      if (swapPat.hasFeasibleRoom(pat.getLastRoom())) candidates.add(swapPat);
    }
    List<Patient> badCandidates = new ArrayList<>();
    for (Patient c : candidates) {
      if (!c.isAdmissibleOn(pat.getAdmission()) || !pat.isAdmissibleOn(c.getAdmission()))
        badCandidates.add(c);
    }
    candidates.removeAll(badCandidates);
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


  // Load cost related functions
  public double getDepartmentLoadCost(int dep) {
    return depLoadCosts[dep];
  }

  public int getTotalDepartmentLoadCost() {
    return (int) Arrays.stream(depLoadCosts).sum();
  }

  public void calculateDepartmentLoadCost(int dep) {
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

  public double getDailyLoadCost(int day) {
    return dailyLoadCosts[day];
  }

  public int getTotalDailyLoadCost() {
    return (int) Arrays.stream(dailyLoadCosts).sum();
  }

  public void calculateDailyLoadCost(int day) {
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
  public Set<Integer>[][] copySchedule() {
    Set<Integer>[][] copy = new Set[schedule.length][schedule[0].length];
    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++)
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        copy[i][j] = new HashSet<>();

    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);
      for (int j = patient.getAdmission(); j < patient.getDischarge(); j++)
        copy[i][j].add(patient.getId());
    }

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

}