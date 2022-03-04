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

    int i = 0;
    while (i < 1) {
      assignInitialPatients(i);
      buildPenaltyMatrix(i);
      i++;
    }
  }

  public void assignInitialPatients(int day) {
    for (int i = 0; i < patients.get(day).size(); i++) {
      Patient patient = patients.get(day).get(i);
      if (patient.getAssignedRoom(day) != -1) {
        assignRoom(i, patient.getAssignedRoom(day), day);
        System.out.println("Assigned patient " + i + " to room " + patient.getAssignedRoom(day));
      }
    }
  }

  public void assignRoom(int p, int r, int day) {
    Patient patient = patients.get(day).get(p);
    Room room = rooms.get(r);
    schedule[r][day].add(patient);
    patient.assignRoom(day, r);

    // Reset gender policy
    if(room.getGenderPolicy().equals("SameGender")){
      int male = 0;
      int female = 0;
      for(Patient pat : schedule[r][day]){
        if(pat.getGender().equals("Male")) male++;
        else female++;
      }
      if(male > female) room.setCurrentPolicy("Male");
      else if(female > male) room.setCurrentPolicy("Female");
    }
  }

  public void buildPenaltyMatrix(int day) {
    // TODO add 100 for all initial patients


    List<Patient> regPatients = patients.get(day);
    penaltyMatrix = new int[regPatients.size()][nRooms];
    for (int i = 0; i < regPatients.size(); i++) {
      Patient patient = regPatients.get(i);

      // Patient - Room costs
      for (int j = 0; j < nRooms; j++) {
        Room room = rooms.get(j);
        if (room.canHost(patient)) {
          int roomEq = 20 * room.getPreferredPropertiesPenalty(patient.getPreferredProperties());
          int roomCap = 10 * room.getCapacityPenalty(patient.getPreferredCapacity());
          int partSpec = 20 * room.getTreatmentPenalty(patient.getNeededSpecialism());
          int genPol = 50 * room.getGenderPenalty(patient.getGender());
          penaltyMatrix[i][j] = roomEq + roomCap + partSpec + genPol;
        } else {
          penaltyMatrix[i][j] = -1;
        }
      }
    }
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
