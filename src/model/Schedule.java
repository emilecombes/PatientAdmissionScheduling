package model;

import util.DateConverter;

import java.util.*;

public class Schedule {
  private final DepartmentList departmentList;
  private final PatientList patientList;
  private final Set<Integer>[][] schedule;
  private final Map<Integer, List<String>> currentGenderPolicies;
  private final Map<Integer, List<Integer>> dynamicGenderViolations;
  private final Map<Integer, List<Integer>> capacityViolations;

  public Schedule(DepartmentList dl, PatientList pl) {
    departmentList = dl;
    patientList = pl;
    schedule = new Set[departmentList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    dynamicGenderViolations = new HashMap<>();
    currentGenderPolicies = new HashMap<>();
    capacityViolations = new HashMap<>();

    for (int i = 0; i < departmentList.getNumberOfRooms(); i++) {
      capacityViolations.put(i, new ArrayList<>());
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++) {
        schedule[i][j] = new HashSet<>();
        capacityViolations.get(0).add(0);
      }
      if (departmentList.getRoom(i).hasGenderPolicy("SameGender")) {
        dynamicGenderViolations.put(i, new ArrayList<>());
        currentGenderPolicies.put(i, new ArrayList<>());
        for (int j = 0; j < DateConverter.getTotalHorizon(); j++) {
          dynamicGenderViolations.get(i).add(0);
          currentGenderPolicies.get(i).add("None");
        }
      }
    }
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r : dynamicGenderViolations.keySet())
      for (int d = 0; d < DateConverter.getTotalHorizon(); d++)
        if (dynamicGenderViolations.get(r).get(d) != 0)
          violations++;
    return violations;
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
    if (dynamicGenderViolations.containsKey(room)) {
      if (getCurrentGenderPolicy(room, day).equals("None"))
        currentGenderPolicies.get(room).set(day, pat.getGender());
      else if (!getCurrentGenderPolicy(room, day).equals(pat.getGender())) {
        if (getGenderCount(room, day, "Male") == getGenderCount(room, day, "Female"))
          currentGenderPolicies.get(room).set(day, pat.getGender());
        else dynamicGenderViolations.get(room)
            .set(day, dynamicGenderViolations.get(room).get(day) + 1);
      }
    }
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
  }

  public int getGenderCount(int room, int day, String gender) {
    int count = 0;
    for (int integer : schedule[room][day])
      if (gender.equals(patientList.getPatient(integer).getGender()))
        count++;
    return count;
  }

  public String getCurrentGenderPolicy(int room, int day) {
    return currentGenderPolicies.get(room).get(day);
  }

}