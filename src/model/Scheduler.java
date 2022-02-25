package model;

import java.util.*;

public class Scheduler {
  private int nDepartments, nRooms, nFeatures, nPatients, nSpecialisms, nTreatments, nDays;
  private GregorianCalendar startDay;
  private HashMap<String, String> treatments;
  private List<Department> departments;
  private List<Room> rooms;
  private List<Patient> patients;
  Set<Patient>[][] schedule;
  Map<String, Integer> roomIndices;
  Map<String, Integer> dayIndices;
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
    String[] date = startDay.split("-");
    this.startDay = new GregorianCalendar(
        Integer.parseInt(date[0]),
        Integer.parseInt(date[1]),
        Integer.parseInt(date[2]));
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
        if (room.canHost(patient, treatments.get(patient.getTreatment()))) {
          penaltyMatrix[i][j] = room.getCapacityPenalty(patient.getPreferredCapacity())
              + room.getPreferredPropertiesPenalty(patient.getPreferredProperties())
              + room.getTreatmentPenalty(treatments.get(patient.getTreatment()));
        } else penaltyMatrix[i][j] = -1;
      }
    }
  }

  public void printPenaltyMatrix() {
    for (int i = 0; i < nPatients; i++) {
      for (int j = 0; j < nRooms; j++) {
        System.out.print(penaltyMatrix[i][j] + ",\t");
      }
      System.out.println();
    }
  }

  public void makeInitialPlanning() {
    writeRoomIndices();
    writeDateIndices();
    schedule = new HashSet[nRooms][nDays];
    assignInitialPatients();
    // Assign patients to a random, but suitable, room
  }

  public void writeRoomIndices() {
    roomIndices = new HashMap<>();
    for (int i = 0; i < nRooms; i++) {
      Room r = rooms.get(i);
      roomIndices.put(r.getName(), i);
    }
  }

  public void writeDateIndices() {
    dayIndices = new HashMap<>();
    for (int i = 0; i <= nDays; i++) {
      GregorianCalendar date = startDay;
      dayIndices.put(getDateString(date), i);
      date.add(Calendar.DAY_OF_YEAR, 1);
    }
    startDay.add(Calendar.DAY_OF_YEAR, -nDays - 1);
  }

  public void assignInitialPatients() {
    for (int i = 0; i < nRooms; i++) {
      for (int j = 0; j < nDays; j++) {
        schedule[i][j] = new HashSet<>();
      }
    }

    for (Patient patient : patients) {
      if (patient.getRoom() != null) {
        for (int i = 0; i < dayIndices.get(patient.getDischargeDate()); i++){
          schedule[roomIndices.get(patient.getRoom())][i].add(patient);
        }
      }
    }
  }

  public String getDateString(GregorianCalendar date){
    return date.get(Calendar.DATE) + "-" +
        date.get(Calendar.MONTH) + "-" +
        date.get(Calendar.YEAR);
  }
}
