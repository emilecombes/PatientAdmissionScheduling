package model;

import java.util.*;

public class Solution implements Comparable<Solution> {
  private final int patientCost;
  private final long creationTime;
  private final double equityCost;
  private final Schedule schedule;
  private final Map<Patient, Integer> delays;
  private final Map<Patient, Room> assignedRooms;
  private double penaltyCoefficient;

  public Solution(Schedule schedule, int pc, double ec, double penaltyCoefficient) {
    this.creationTime = System.currentTimeMillis();
    this.patientCost = pc;
    this.equityCost = ec;
    this.penaltyCoefficient = penaltyCoefficient;
    this.schedule = schedule.getCopy();
    this.assignedRooms = new HashMap<>();
    this.delays = new HashMap<>();
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient patient = PatientList.getPatient(i);
      Room room = DepartmentList.getRoom(patient.getLastRoom());
      delays.put(patient, patient.getDelay());
      assignedRooms.put(patient, room);
    }
  }

  public Schedule copySchedule() {
    return schedule.getCopy();
  }

  public void setPenaltyCoefficient(double p) {
    penaltyCoefficient = p;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public int getPatientCost() {
    return patientCost;
  }

  public double getEquityCost() {
    return equityCost;
  }

  public double getPenaltyCoefficient() {
    return penaltyCoefficient;
  }

  public int getDelay(Patient p) {
    return delays.get(p);
  }

  public Room getAssignedRoom(Patient p) {
    return assignedRooms.get(p);
  }

  public int getCapacityViolations() {
    return schedule.getCapacityViolations();
  }

  public int getDynamicGenderViolations() {
    return schedule.getDynamicGenderViolations();
  }

  public boolean strictlyDominates(Solution s) {
    return strictlyDominates(s.getPatientCost(), (int) s.getEquityCost());
  }

  public boolean strictlyDominates(int pc, int ec) {
    return equityCost <= ec && patientCost <= pc && !(equityCost == ec && patientCost == pc);
  }

  public void loadPatientConfiguration() {
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient p = PatientList.getPatient(i);
      p.setDelay(delays.get(p));
      p.assignRoom(assignedRooms.get(p));
    }
  }

  @Override
  public int compareTo(Solution s) {
    return (int) (equityCost - s.getEquityCost());
  }

  public String toString() {
    return "{\"creation_time\":\"" + getCreationTime() +
        "\",\"patient_cost\":\"" + getPatientCost() +
        "\",\"equity_cost\":\"" + getEquityCost() +
        "\",\"penalty_coefficient\":\"" + getPenaltyCoefficient() +
        "\"}";
  }
}
