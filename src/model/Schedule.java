package model;

import util.DateConverter;

import java.util.*;

public class Schedule {
  private final DepartmentList departmentList;
  private final PatientList patientList;
  private final Set<Integer>[][] schedule;
  private final Map<Integer, List<Map<String, Integer>>> genderCount;

  public Schedule(DepartmentList dl, PatientList pl) {
    departmentList = dl;
    patientList = pl;
    schedule = new Set[departmentList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    genderCount = new HashMap<>();

    for (int i = 0; i < departmentList.getNumberOfRooms(); i++) {
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        schedule[i][j] = new HashSet<>();
      if (departmentList.getRoom(i).hasGenderPolicy("SameGender")) {
        genderCount.put(i, new ArrayList<>());
        for (int j = 0; j < DateConverter.getTotalHorizon(); j++) {
          genderCount.get(i).add(new HashMap<>());
          genderCount.get(i).get(j).put("Male", 0);
          genderCount.get(i).get(j).put("Female", 0);
        }
      }
    }
  }

  public Set<Integer> getPatients(int room, int day) {
    return schedule[room][day];
  }

  public int getCapacityViolations() {
    int violations = 0;
    for (int room = 0; room < departmentList.getNumberOfRooms(); room++)
      for (int day = 0; day < DateConverter.getTotalHorizon(); day++)
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
    return genderCount.get(room).get(day).get(gender);
  }

  public int getOtherGenderCount(int room, int day, String gender) {
    String otherGender = (gender.equals("Male")) ? "Female" : "Male";
    return getGenderCount(room, day, otherGender);
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r : genderCount.keySet())
      for (int d = 0; d < DateConverter.getTotalHorizon(); d++)
        if (getDynamicGenderViolations(r, d) > 0)
          violations++;
    return violations;
  }

  public int getDynamicGenderViolations(int room, int day) {
    return Math.min(getGenderCount(room, day, "Male"), getGenderCount(room, day, "Female"));
  }

  public void incrementGenderCount(int room, int day, String gender) {
    genderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) + 1);
  }

  public void decrementGenderCount(int room, int day, String gender) {
    genderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) - 1);
  }

  public boolean hasSingleGenderViolation(int room, int day, String gender) {
    if (!genderCount.containsKey(room)) return false;
    return getDynamicGenderViolations(room, day) == 1 && getGenderCount(room, day, gender) == 1;
  }

  public boolean isFirstGenderViolation(int room, int day, String gender) {
    if (!genderCount.containsKey(room)) return false;
    return getDynamicGenderViolations(room, day) == 0 && getOtherGenderCount(room, day, gender) > 1;
  }

  public List<Integer> getSwapRoomPatients(Patient patient, boolean overlapping) {
    int start = (overlapping) ? patient.getAdmission() : 0;
    int end = (overlapping) ? patient.getDischarge() : DateConverter.getTotalHorizon();

    Set<Integer> candidates = new HashSet<>();
    Set<Integer> rooms = patient.getFeasibleRooms();
    rooms.remove(patient.getLastRoom());
    for (int room : rooms)
      for (int i = start; i < end; i++)
        candidates.addAll(getPatients(room, i));

    Set<Integer> feasiblePatients = new HashSet<>();
    for (int pat : candidates)
      if (patientList.getPatient(pat).hasFeasibleRoom(patient.getLastRoom()))
        if (overlapping) feasiblePatients.add(pat);
        else if (patientList.getPatient(pat).isAdmissibleOn(patient.getAdmission()) &&
            patient.isAdmissibleOn(patientList.getPatient(pat).getAdmission()))
          feasiblePatients.add(pat);

    return new ArrayList<>(feasiblePatients);
  }

  public Patient getSwapRoomPatient(Patient firstPatient) {
    List<Integer> feasiblePatients = getSwapRoomPatients(firstPatient, true);
    if (feasiblePatients.isEmpty()) return null;
    int patient = feasiblePatients.get((int) (Math.random() * feasiblePatients.size()));
    return patientList.getPatient(patient);
  }

  public Patient getSwapAdmissionPatient(Patient firstPatient) {
    List<Integer> feasiblePatients = getSwapRoomPatients(firstPatient, false);
    if (feasiblePatients.isEmpty()) return null;
    int patient = feasiblePatients.get((int) (Math.random() * feasiblePatients.size()));
    return patientList.getPatient(patient);
  }

  public void assignPatient(Patient pat, int room, int day) {
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
    if (genderCount.containsKey(room)) incrementGenderCount(room, day, pat.getGender());
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getLastRoom();
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
    if (genderCount.containsKey(room)) decrementGenderCount(room, day, pat.getGender());
  }
}