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

  public void setGenderPolicy(int room, int day, String gender) {
    currentGenderPolicies.get(room).set(day, gender);
  }

  public int getGenderImbalance(int room, int day) {
    int imbalance = 0;
    for (int i : schedule[room][day])
      if (patientList.getPatient(i).getGender().equals("Male")) imbalance++;
      else imbalance--;
    return Math.abs(imbalance);
  }

  public String getCurrentGenderPolicy(int room, int day) {
    return currentGenderPolicies.get(room).get(day);
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r : dynamicGenderViolations.keySet())
      for (int d = 0; d < DateConverter.getTotalHorizon(); d++)
        if (dynamicGenderViolations.get(r).get(d) != 0)
          violations++;
    return violations;
  }

  public void incrementGenderViolations(int room, int day) {
    dynamicGenderViolations.get(room).set(day, dynamicGenderViolations.get(room).get(day) + 1);
  }

  public void decrementGenderViolations(int room, int day) {
    dynamicGenderViolations.get(room).set(day, dynamicGenderViolations.get(room).get(day) - 1);
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
        setGenderPolicy(room, day, pat.getGender());
      else if (!getCurrentGenderPolicy(room, day).equals(pat.getGender()))
        if (getGenderImbalance(room, day) == 0)
          setGenderPolicy(room, day, pat.getGender());
        else incrementGenderViolations(room, day);
    }
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
  }

  public void cancelPatient(Patient pat, int day) {
    int room = pat.getRoom(day);
    if (dynamicGenderViolations.containsKey(room)) {
      if (schedule[room][day].size() == 1) setGenderPolicy(room, day, "None");
      else if (!getCurrentGenderPolicy(room, day).equals(pat.getGender())) {
        if (getGenderImbalance(room, day) == 0) {
          String newPolicy = (pat.getGender().equals("Male")) ? "Female" : "Male";
          setGenderPolicy(room, day, newPolicy);
        } else decrementGenderViolations(room, day);
      }
    }
    pat.cancelRoom(day);
    schedule[room][day].remove(pat.getId());
  }


}