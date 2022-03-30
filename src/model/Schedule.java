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

  public int getCapacityViolations(int room, int day) {
    return Math.max(0, schedule[room][day].size() - departmentList.getRoom(room).getCapacity());
  }

  public int getCapacityMargin(int room, int day) {
    return Math.max(0, departmentList.getRoom(room).getCapacity() - schedule[room][day].size());
  }

  public int getGenderCount(int room, int day, String gender) {
    return genderCount.get(room).get(day).get(gender);
  }

  public int getGenderViolations(int room, int day) {
    return Math.min(getGenderCount(room, day, "Male"), getGenderCount(room, day, "Female"));
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r : genderCount.keySet())
      for (int d = 0; d < DateConverter.getTotalHorizon(); d++)
        if (getGenderViolations(r, d) > 0)
          violations++;
    return violations;
  }

  public void incrementGenderCount(int room, int day, String gender) {
    genderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) + 1);
  }

  public void decrementGenderCount(int room, int day, String gender) {
    genderCount.get(room).get(day).put(gender, getGenderCount(room, day, gender) - 1);
  }

  public Patient getSwapRoomPatient(int pat) {
    Patient firstPat = patientList.getPatient(pat);
    int firstRoom = firstPat.getRoom(firstPat.getAdmission());
    for (int feasibleRoom : firstPat.getFeasibleRooms()) {

    }
    // First count to 3 in feasRooms btwn ad & dd
    return null;
  }

  public Patient getSwapAdmissionPatient(int pat) {
    // Don't count, there will always be a feasible patient
    return null;
  }

  public void assignPatient(Patient pat, int room, int day) {
    if (genderCount.containsKey(room)) incrementGenderCount(room, day, pat.getGender());
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getLastRoom();
    if (genderCount.containsKey(room)) decrementGenderCount(room, day, pat.getGender());
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
  }


}