package model;

import util.DateConverter;

import java.util.*;

public class Solution {
  private final int patientCost, equityCost;
  private final Map<Patient, Integer> delays;
  private final Map<Patient, Room> assignedRooms;
  private final Map<Room, List<Set<Patient>>> admittedPatients;

  public Solution(PatientList patientList, int patientCost, int equityCost) {
    this.patientCost = patientCost;
    this.equityCost = equityCost;
    this.delays = new HashMap<>();
    this.assignedRooms = new HashMap<>();
    this.admittedPatients = new HashMap<>();

    for (int i = 0; i < DepartmentList.getNumberOfRooms(); i++) {
      Room room = DepartmentList.getRoom(i);
      List<Set<Patient>> roomPatients = new ArrayList<>();
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        roomPatients.add(new HashSet<>());
      admittedPatients.put(room, roomPatients);
    }

    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient patient = patientList.getPatient(i);
      Room room = DepartmentList.getRoom(patient.getLastRoom());
      delays.put(patient, patient.getDelay());
      assignedRooms.put(patient, room);
      for (int j = patient.getAdmission(); j < patient.getDischarge(); j++)
        admittedPatients.get(room).get(j).add(patient);
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

  public Set<Patient> getAdmittedPatients(Room room, int day) {
    return admittedPatients.get(room).get(day);
  }
}
