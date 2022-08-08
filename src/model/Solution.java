package model;

import java.util.*;

public class Solution implements Comparable<Solution> {
  private final int patientCost, equityCost;
  private final double temperature, penaltyCoefficient;
  private final Schedule schedule;
  private final Map<Patient, Integer> delays;
  private final Map<Patient, Room> assignedRooms;

  public Solution(Schedule schedule, int patientCost, int equityCost, double temperature,
                  double penaltyCoefficient) {
    this.patientCost = patientCost;
    this.equityCost = equityCost;
    this.temperature = temperature;
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

  public int getPatientCost() {
    return patientCost;
  }

  public int getEquityCost() {
    return equityCost;
  }

  public double getPenaltyCoefficient() {
    return penaltyCoefficient;
  }

  public double getTemperature() {
    return temperature;
  }

  public boolean strictlyDominates(Solution s) {
    return strictlyDominates(s.getPatientCost(), s.getEquityCost());
  }

  public boolean strictlyDominates(int pc, int ec) {
    return equityCost <= ec && patientCost <= pc && !(equityCost == ec && patientCost == pc);
  }

  public void loadPatientConfiguration() {
    for(int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient p = PatientList.getPatient(i);
      p.setDelay(delays.get(p));
      p.assignRoom(assignedRooms.get(p));
    }
  }

  @Override
  public int compareTo(Solution s) {
    return equityCost - s.getEquityCost();
  }
}
