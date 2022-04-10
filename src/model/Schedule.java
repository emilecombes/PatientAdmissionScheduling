package model;

import util.DateConverter;

import java.lang.reflect.Array;
import java.util.*;

public class Schedule {
  private final DepartmentList departmentList;
  private final PatientList patientList;
  private final int horizonLength;
  private final int departmentCount;

  private final Set<Integer>[][] schedule;
  private final Map<Integer, List<Map<String, Integer>>> dynamicGenderCount;

  private final int[][] loadMatrix;
  private final double[] averageDailyLoads;
  private final double[] dailyLoads;
  private final double[] averageDepartmentLoads;
  private final double[] departmentLoads;

  public Schedule(DepartmentList dl, PatientList pl) {
    departmentList = dl;
    patientList = pl;
    horizonLength = DateConverter.getTotalHorizon();
    departmentCount = departmentList.getNumberOfDepartments();
    schedule = new Set[departmentList.getNumberOfRooms()][horizonLength];
    dynamicGenderCount = new HashMap<>();
    loadMatrix = new int[departmentCount][horizonLength];
    averageDailyLoads = new double[horizonLength];
    dailyLoads = new double[horizonLength];
    averageDepartmentLoads = new double[departmentCount];
    departmentLoads = new double[departmentCount];

    for (int i = 0; i < departmentList.getNumberOfRooms(); i++) {
      for (int j = 0; j < horizonLength; j++)
        schedule[i][j] = new HashSet<>();
      if (departmentList.getRoom(i).hasGenderPolicy("SameGender")) {
        dynamicGenderCount.put(i, new ArrayList<>());
        for (int j = 0; j < horizonLength; j++) {
          dynamicGenderCount.get(i).add(new HashMap<>());
          dynamicGenderCount.get(i).get(j).put("Male", 0);
          dynamicGenderCount.get(i).get(j).put("Female", 0);
        }
      }
    }
  }

  public Set<Integer> getPatients(int room, int day) {
    return schedule[room][day];
  }

  public Set<Integer> getSwappablePatients(int searchRoom, int swapRoom, int day) {
    Set<Integer> candidates = new HashSet<>(getPatients(searchRoom, day));
    Set<Integer> badCandidates = new HashSet<>();
    for (int pat : candidates)
      if (!patientList.getPatient(pat).hasFeasibleRoom(swapRoom))
        badCandidates.add(pat);
    candidates.removeAll(badCandidates);
    return candidates;
  }


  public Patient getSwapRoomPatient(Patient firstPatient) {
    int swapRoom = firstPatient.getLastRoom();
    Set<Integer> rooms = firstPatient.getFeasibleRooms();
    rooms.remove(swapRoom);

    Set<Integer> candidates = new HashSet<>();
    for (int day = firstPatient.getAdmission(); day < firstPatient.getDischarge(); day++)
      for (int searchRoom : rooms)
        candidates.addAll(getSwappablePatients(searchRoom, swapRoom, day));

    if (candidates.isEmpty()) return null;
    List<Integer> swapPatients = new ArrayList<>(candidates);
    Collections.shuffle(swapPatients);
    return patientList.getPatient(swapPatients.get(0));
  }

  public Patient getSwapAdmissionPatient(Patient firstPatient) {
    int firstAdmission = firstPatient.getAdmission();
    int swapRoom = firstPatient.getLastRoom();
    Set<Integer> rooms = firstPatient.getFeasibleRooms();
    rooms.remove(swapRoom);

    Set<Integer> candidates = new HashSet<>();
    for (int day = 0; day < horizonLength; day++)
      for (int searchRoom : rooms)
        candidates.addAll(getSwappablePatients(searchRoom, swapRoom, day));

    Set<Integer> badCandidates = new HashSet<>();
    for (int candidate : candidates) {
      Patient secondPatient = patientList.getPatient(candidate);
      int secondAdmission = secondPatient.getAdmission();
      if (!firstPatient.isAdmissibleOn(secondAdmission) ||
          !secondPatient.isAdmissibleOn(firstAdmission))
        badCandidates.add(candidate);
    }
    candidates.removeAll(badCandidates);

    if (candidates.isEmpty()) return null;
    List<Integer> swapPatients = new ArrayList<>(candidates);
    Collections.shuffle(swapPatients);
    return patientList.getPatient(swapPatients.get(0));
  }

  public void assignPatient(Patient pat, int room, int day) {
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
    if (dynamicGenderCount.containsKey(room)) incrementGenderCount(room, day, pat.getGender());

    int dep = departmentList.getDepartment(departmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / departmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] += delta;
    incrementAverageDailyLoad(day, delta / horizonLength);
    incrementAverageDepartmentLoad(dep, delta / departmentCount);
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getRoom(day);
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
    if (dynamicGenderCount.containsKey(room)) decrementGenderCount(room, day, pat.getGender());

    int dep = departmentList.getDepartment(departmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / departmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] -= delta;
    decrementAverageDailyLoad(day, delta / horizonLength);
    decrementAverageDepartmentLoad(dep, delta / departmentCount);
  }


  // Patient cost related functions
  public int getCapacityViolations() {
    int violations = 0;
    for (int room = 0; room < departmentList.getNumberOfRooms(); room++)
      for (int day = 0; day < horizonLength; day++)
        violations += getCapacityViolations(room, day);
    return violations;
  }

  public int getCapacityViolations(int room, int day) {
    return Math.max(0, schedule[room][day].size() - departmentList.getRoom(room).getCapacity());
  }

  public int getCapacityMargin(int room, int day) {
    return Math.max(0, departmentList.getRoom(room).getCapacity() - schedule[room][day].size());
  }

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
      for (int d = 0; d < horizonLength; d++)
        if (getDynamicGenderViolations(r, d) > 0)
          violations++;
    return violations;
  }

  public int getDynamicGenderViolations(int room, int day) {
    return Math.min(getGenderCount(room, day, "Male"), getGenderCount(room, day, "Female"));
  }

  public boolean hasSingleDynamicGenderViolation(int room, int day, String gender) {
    if (!dynamicGenderCount.containsKey(room)) return false;
    return getDynamicGenderViolations(room, day) == 1 && getGenderCount(room, day, gender) == 1;
  }

  public boolean isFirstDynamicGenderViolation(int room, int day, String gender) {
    if (!dynamicGenderCount.containsKey(room)) return false;
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
    return departmentLoads[dep];
  }

  public int getTotalDepartmentLoadCost() {
    return (int) Arrays.stream(departmentLoads).sum();
  }

  public void calculateDepartmentLoadCost(int dep) {
    double cost = 0;
    for (int i = 0; i < horizonLength; i++)
      cost += Math.pow(averageDepartmentLoads[dep] - loadMatrix[dep][i], 2);
    departmentLoads[dep] = cost;
  }

  public void incrementAverageDepartmentLoad(int dep, double delta) {
    averageDepartmentLoads[dep] += delta;
  }

  public void decrementAverageDepartmentLoad(int dep, double delta) {
    averageDepartmentLoads[dep] -= delta;
  }

  public double getDailyLoadCost(int day) {
    return dailyLoads[day];
  }

  public int getTotalDailyLoadCost() {
    return (int) Arrays.stream(dailyLoads).sum();
  }

  public void calculateDailyLoadCost(int day) {
    double cost = 0;
    for (int i = 0; i < departmentCount; i++)
      cost += Math.pow(averageDailyLoads[day] - loadMatrix[i][day], 2);
    dailyLoads[day] = cost;
  }

  public void incrementAverageDailyLoad(int day, double delta) {
    averageDailyLoads[day] += delta;
  }

  public void decrementAverageDailyLoad(int day, double delta) {
    averageDailyLoads[day] -= delta;
  }

}