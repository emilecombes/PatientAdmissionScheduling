package model;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Scheduler {
  private int nDepartments, nRooms, nFeatures, nPatients, nSpecialisms, nTreatments, nDays;
  private String startDay;
  private HashMap<String, String> treatments;
  private List<Department> departments;
  private List<Room> rooms;
  private List<Patient> patients;

  private Solution solution;
  private int[][] penaltyMatrix;

  public Scheduler() {
  }

  public void setDescription(int nDepartments, int nRooms, int nFeatures, int nPatients,
                             int nSpecialisms, int nTreatments, int nDays, String startDay) {
    this.nDepartments = nDepartments;
    this.nRooms = nRooms;
    this.nFeatures = nFeatures;
    this.nPatients = nPatients;
    this.nSpecialisms = nSpecialisms;
    this.nTreatments = nTreatments;
    this.nDays = nDays;
    this.startDay = startDay;
  }

  public void setDepartments(List<Department> departments) {
    this.departments = departments;
  }

  public void setRooms(List<Room> rooms) {
    this.rooms = rooms;
  }

  public void setTreatments(HashMap<String, String> treatments) {
    this.treatments = treatments;
  }

  public void setPatients(List<Patient> patients) {
    this.patients = patients;
  }

  public Department getDepartment(String name) {
    for (Department d : departments) {
      if (d.getName().equals(name)) return d;
    }
    return null;
  }

  public void buildPenaltyMatrix() {
    penaltyMatrix = new int[nPatients][nRooms];
    for (int i = 0; i < nPatients; i++) {
      Patient patient = patients.get(i);
      for (int j = 0; j < nRooms; j++) {
        Room room = rooms.get(j);
        // gender constraints
        penaltyMatrix[i][j] += room.getGenderPenalty(patient.getGender());
        // preferred capacity
        penaltyMatrix[i][j] += room.getCapacityPenalty(patient.getPreferredCapacity());
        // preferred properties
        penaltyMatrix[i][j] += room.getPreferredPropertiesPenalty(patient.getPreferredProperties());
        // needed properties
        penaltyMatrix[i][j] += room.getNeededPropertiesPenalty(patient.getNeededProperties());
        // specialisms for treatment
        penaltyMatrix[i][j] += room.getTreatmentPenalty(treatments.get(patient.getTreatment()));
      }
    }
  }

  public void makeInitialPlanning() {
    System.out.println("Hello");
  }
}
