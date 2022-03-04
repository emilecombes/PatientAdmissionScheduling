package model;

import java.util.*;

public class Scheduler {
  private int nDepartments, nRooms, nFeatures, nPatients, nSpecialisms, nTreatments, nDays;
  private GregorianCalendar startDay;
  private HashMap<String, String> treatmentSpecialismMap;
  private List<Department> departments;
  private List<Room> rooms;
  private List<List<Patient>> patients;
  private Set<Patient>[][] schedule;
  private Map<String, Integer> roomIndices;
  private Map<String, Integer> dayIndices;
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
    writeDateIndices();
  }

  public void setDepartments(List<Department> departments) {
    this.departments = departments;
  }

  public void setRooms(List<Room> rooms) {
    this.rooms = rooms;
    writeRoomIndices();
  }

  public void setTreatmentSpecialismMap(HashMap<String, String> treatmentSpecialismMap) {
    this.treatmentSpecialismMap = treatmentSpecialismMap;
  }

  public void setPatients(List<List<Patient>> patients) {
    this.patients = patients;
  }

  public int getNDays() {
    return nDays;
  }

  public Department getDepartment(String name) {
    for (Department d : departments) {
      if (d.getName().equals(name)) return d;
    }
    return null;
  }

  public String getDateString(GregorianCalendar date) {
    StringBuilder sb = new StringBuilder();
    String day = String.valueOf(date.get(Calendar.DATE));
    if (day.length() == 1) day = "0" + day;
    String month = String.valueOf(date.get(Calendar.MONTH));
    if (month.length() == 1) month = "0" + month;
    String year = String.valueOf(date.get(Calendar.YEAR));
    sb.append(year);
    sb.append("-");
    sb.append(month);
    sb.append("-");
    sb.append(day);
    return sb.toString();
  }

  public String getSpecialism(String tr) {
    return treatmentSpecialismMap.get(tr);
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

  public int getDateIndex(String date) {
    return dayIndices.get(date);
  }

  public void writeRoomIndices() {
    roomIndices = new HashMap<>();
    for (int i = 0; i < nRooms; i++) {
      Room r = rooms.get(i);
      roomIndices.put(r.getName(), i);
    }
  }

  public int getRoomIndex(String name) {
    return roomIndices.get(name);
  }


  public void dynamicSolve() {
    schedule = new Set[nRooms][nDays * 3];
    for (int i = 0; i < nRooms; i++)
      for (int j = 0; j < schedule[0].length; j++)
        schedule[i][j] = new HashSet<>();

    assignInitialPatients();

    int i = 0;
    while (i < 1) {
      buildPenaltyMatrix(i);
      insertPatients(i);
      i++;
    }
  }

  public void assignInitialPatients() {
    for (int i = 0; i < patients.get(0).size(); i++) {
      Patient patient = patients.get(0).get(i);
      if (patient.getAssignedRoom(0) != -1)
        assignRoom(i, patient.getAssignedRoom(0), 0);
    }
  }

  public void assignRoom(int p, int r, int day) {
    Patient patient = patients.get(day).get(p);
    schedule[r][day].add(patient);
    patient.assignRoom(day, r);
  }

  public void buildPenaltyMatrix(int day) {
    // What will not be in this matrix:
    //   1. delay costs,
    //   2. D gender policy cost (may be different policy over a period)
    //   3. Capacity violations (weight 10 000 -- per extra patient, per day)

    List<Patient> regPatients = patients.get(day);
    penaltyMatrix = new int[regPatients.size()][nRooms];
    for (int i = 0; i < regPatients.size(); i++) {
      Patient patient = regPatients.get(i);
      for (int j = 0; j < nRooms; j++) {
        Room room = rooms.get(j);
        int length = patient.getDelay() + patient.getDischargeDate() - day;
        int penalty = room.getRoomPenalty(patient);
        penaltyMatrix[i][j] = (penalty == -1) ? -1 : length * penalty;
      }
    }

    for(int i = 0; i < regPatients.size(); i++){
      Patient patient = regPatients.get(i);
      if(patient.getAssignedRoom(day) != -1)
        for(int j = 0; j < nRooms; j++)
          if(j != patient.getAssignedRoom(day) && penaltyMatrix[i][j] != -1)
            penaltyMatrix[i][j] += 100;
    }
    System.out.println("aids");
  }

  public void insertPatients(int day){

  }





  public Set<Room> getMainSpecialityRooms(Patient patient) {
    // TODO return a set of feasible (free space & needed features) rooms
    Set<Room> mainRooms = new HashSet<>();
    for (Department department : departments) {
      if (department.hasMainSpecialism(treatmentSpecialismMap.get(patient.getTreatment()))) {
        mainRooms.addAll(department.getRooms());
      }
    }
    // Remove fully occupied rooms & rooms w/ lacking features
    Set<Room> badRooms = new HashSet<>();
    for (Room room : mainRooms) {
      for (int i = patient.getAdmissionDate(); i < patient.getDischargeDate(); i++) {
        if (schedule[roomIndices.get(room.getName())][i].size() == room.getCapacity()) {
          badRooms.add(room);
        } else if (!room.canHost(patient)) {
          badRooms.add(room);
        }
      }
    }
    return null;
  }

  public Set<Room> getAuxSpecialismRooms(Patient patient) {
    // TODO return a set of feasible (free space & needed features) rooms
    return null;
  }


}
