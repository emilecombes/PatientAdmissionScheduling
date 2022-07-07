package model;

import util.DateConverter;

import java.util.*;

public class Solution {
  private final int patientCost, equityCost;
  private final Set<Integer>[][] schedule;
  private final Map<Integer, List<Map<String, Integer>>> dynamicGenderCount;
  private final Map<Patient, Integer> delays;
  private final Map<Patient, Room> assignedRooms;
  private int capacityViolations;
  private final double[][] loadMatrix;
  private final double[] averageDailyLoads;
  private final double[] dailyLoadCosts;
  private final double[] averageDepartmentLoads;
  private final double[] departmentLoadCosts;

  public Solution(int patientCost, int equityCost, Schedule schedule) {
    this.patientCost = patientCost;
    this.equityCost = equityCost;
    this.delays = new HashMap<>();
    this.assignedRooms = new HashMap<>();
    this.schedule = new Set[DepartmentList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    this.dynamicGenderCount = schedule.copyDynamicGenderViolations();
    this.capacityViolations = schedule.getCapacityViolations();
    this.loadMatrix = schedule.getLoadMatrix();
    this.averageDailyLoads = schedule.getAverageDailyLoads();
    this.dailyLoadCosts = schedule.getDailyLoadCosts();
    this.averageDepartmentLoads = schedule.getAverageDepartmentLoads();
    this.departmentLoadCosts = schedule.getDepartmentLoadCosts();


    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++)
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        this.schedule[i][j] = new HashSet<>();

    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);
      Room room = DepartmentList.getRoom(patient.getLastRoom());
      delays.put(patient, patient.getDelay());
      assignedRooms.put(patient, room);
      for (int j = patient.getAdmission(); j < patient.getDischarge(); j++)
        this.schedule[i][j].add(patient.getId());
    }
  }

  public int getPatientCost() {
    return patientCost;
  }

  public int getEquityCost() {
    return equityCost;
  }

  public int getDelay(Patient pat) {
    return delays.get(pat);
  }

  public Room getRoom(Patient pat) {
    return assignedRooms.get(pat);
  }

  public Set<Integer>[][] getSchedule() {
    return schedule;
  }

  public Map<Integer, List<Map<String, Integer>>> getDynamicGenderCount() {
    return dynamicGenderCount;
  }

  public int getCapacityViolations() {
    return capacityViolations;
  }

  public double[][] getLoadMatrix() {
    return loadMatrix;
  }

  public double[] getAverageDailyLoads() {
    return averageDailyLoads;
  }

  public double[] getDailyLoadCosts() {
    return dailyLoadCosts;
  }

  public double[] getAverageDepartmentLoads() {
    return averageDepartmentLoads;
  }

  public double[] getDepartmentLoadCosts() {
    return departmentLoadCosts;
  }
}
