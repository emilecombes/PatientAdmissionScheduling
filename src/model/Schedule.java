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
  private int capacityViolations;

  private final double[][] loadMatrix;
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
    capacityViolations = 0;

    loadMatrix = new double[departmentCount][horizonLength];
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

  public boolean checkLoadMatrix(int solverCost) {
    boolean error = false;
    // Build matrix from patients
    double[][] tempMatrix = new double[departmentCount][horizonLength];
    for (int pat = 0; pat < patientList.getNumberOfPatients(); pat++) {
      Patient patient = patientList.getPatient(pat);
      for (int i = patient.getAdmission(); i < patient.getDischarge(); i++) {
        int dep = departmentList.getRoom(patient.getRoom(i)).getDepartmentId();
        tempMatrix[dep][i] += patient.getNeededCare(i);
      }
    }
    for (int i = 0; i < departmentCount; i++)
      for (int j = 0; j < horizonLength; j++) {
        tempMatrix[i][j] /= departmentList.getDepartment(i).getSize();
        int act = (int) (Math.round(10000 * loadMatrix[i][j]));
        int tmp = (int) (Math.round(10000 * tempMatrix[i][j]));
        if (Math.abs(act - tmp) > 1) {
          error = true;
          System.err.println(
              "Value in loadmatrix is wrong (" + loadMatrix[i][j] + "≠" + tempMatrix[i][j] + ")");
        }
      }

    // Calculate averages
    for (int i = 0; i < departmentCount; i++) {
      double avg = 0;
      for (int j = 0; j < horizonLength; j++) {
        avg += loadMatrix[i][j];
      }
      avg /= horizonLength;
      int act = (int) Math.round((10000 * avg));
      int tmp = (int) Math.round((10000 * averageDepartmentLoads[i]));
      if (Math.abs(act - tmp) > 1) {
        error = true;
        System.err.println("Wrong avg dep load (" + averageDepartmentLoads[i] + "≠" + avg + ")");
      }
    }
    for (int j = 0; j < horizonLength; j++) {
      double avg = 0;
      for (int i = 0; i < departmentCount; i++) {
        avg += loadMatrix[i][j];
      }
      avg /= departmentCount;
      int act = (int) Math.round((10000 * avg));
      int tmp = (int) Math.round((10000 * averageDailyLoads[j]));
      if (Math.abs(act - tmp) > 1) {
        error = true;
        System.err.println("Wrong avg daily load (" + averageDailyLoads[j] + "≠" + avg + ")");
      }
    }

    // Compare costs
    int correctCost =
        (int) (Math.round(Arrays.stream(departmentLoads).sum() + Arrays.stream(dailyLoads).sum()));
    if (Math.abs(correctCost - solverCost) > 2) {
      error = true;
      System.err.println("Costs don't match (" + correctCost + "≠" + solverCost + ")");
    }
    return error;
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
    for (int day = firstPatient.getOriginalAD(); day <= firstPatient.getMaxAdm(); day++)
      for (int searchRoom : rooms)
        candidates.addAll(getSwappablePatients(searchRoom, swapRoom, day));

    Set<Integer> badCandidates = new HashSet<>();
    for (int candidate : candidates) {
      if (!patientList.getPatient(candidate).isAdmissibleOn(firstAdmission))
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
    if (dynamicGenderCount.containsKey(room))
      incrementGenderCount(room, day, pat.getGender());
    if (schedule[room][day].size() > departmentList.getRoom(room).getCapacity())
      capacityViolations++;

    int dep = departmentList.getDepartment(departmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / departmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] += delta;
    incrementAverageDailyLoad(day, delta / departmentCount);
    incrementAverageDepartmentLoad(dep, delta / horizonLength);
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getRoom(day);
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
    if (dynamicGenderCount.containsKey(room))
      decrementGenderCount(room, day, pat.getGender());
    if (schedule[room][day].size() >= departmentList.getRoom(room).getCapacity())
      capacityViolations--;

    int dep = departmentList.getDepartment(departmentList.getRoom(room).getDepartment()).getId();
    double delta = (double) pat.getNeededCare(day) / departmentList.getDepartment(dep).getSize();
    loadMatrix[dep][day] -= delta;
    decrementAverageDailyLoad(day, delta / departmentCount);
    decrementAverageDepartmentLoad(dep, delta / horizonLength);
  }


  // Patient cost related functions
  public int getCapacityViolations() {
    return capacityViolations;
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
    return departmentLoads[dep];
  }

  public int getTotalDepartmentLoadCost() {
    return (int) Arrays.stream(departmentLoads).sum();
  }

  public void calculateDepartmentLoadCost(int dep) {
    if (dep == -1) return;
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