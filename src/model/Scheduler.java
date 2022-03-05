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

  public List<Room> getRooms() {
    return rooms;
  }

  public int getNDays() {
    return nDays;
  }

  public GregorianCalendar getStartDay() {
    return startDay;
  }

  public List<Patient> getAllPatients() {
    return patients.get(nDays - 1);
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
    assignInitialPatients();
    int i = 0;
    while (i < nDays) {
      buildPenaltyMatrix(i);
      insertPatients(i);
      solve(i);
      i++;
    }
    calculateCosts();
  }

  public void assignInitialPatients() {
    schedule = new Set[nRooms][nDays * 3];
    for (int i = 0; i < nRooms; i++) {
      for (int j = 0; j < schedule[0].length; j++)
        schedule[i][j] = new HashSet<>();
    }

    for (int i = 0; i < patients.get(0).size(); i++) {
      Patient patient = patients.get(0).get(i);
      if (patient.getAssignedRoom(0) != -1)
        for (int j = 0; j < patient.getDischargeDate(); j++)
          assignRoom(patient, patient.getAssignedRoom(0), j);
    }
  }

  public void assignRoom(Patient pat, int r, int day) {
    schedule[r][day].add(pat);
    pat.assignRoom(day, r);
  }

  public void buildPenaltyMatrix(int day) {
    List<Patient> registeredPatients = patients.get(day);
    penaltyMatrix = new int[registeredPatients.size()][nRooms];
    for (int i = 0; i < registeredPatients.size(); i++) {
      Patient patient = registeredPatients.get(i);
      for (int j = 0; j < nRooms; j++) {
        Room room = rooms.get(j);
        penaltyMatrix[i][j] = room.getRoomPenalty(patient);
      }
    }

    for (int i = 0; i < registeredPatients.size(); i++) {
      Patient patient = registeredPatients.get(i);
      if (patient.getAssignedRoom(day) != -1)
        for (int j = 0; j < nRooms; j++)
          if (j != patient.getAssignedRoom(day) && penaltyMatrix[i][j] != -1)
            penaltyMatrix[i][j] += 100;
    }
  }

  public void insertPatients(int day) {
    for (int pat = 0; pat < patients.get(day).size(); pat++) {
      Patient patient = patients.get(day).get(pat);
      int room = patient.getAssignedRoom(patient.getAdmissionDate());
      if (room == -1) {
        do room = (int) (nRooms * Math.random());
        while (penaltyMatrix[pat][room] < 0);
      }
      for (int d = patient.getAdmissionDate(); d < patient.getDischargeDate(); d++)
        assignRoom(patient, room, d);
    }
  }


  public void solve(int day){
    int random = getMovablePatient(day);
    Patient patient = patients.get(day).get(random);
  }

  public void changeRoom(Patient patient, int room){
    // TODO patient is moved to new room for complete LOT
  }

  public void swapRooms(Patient first, Patient second){
    // TODO patients exchange their rooms
  }

  public void shiftAdmission(Patient patient, int days){
    // TODO patient is delayed or advanced to min. planned admission, max. max admission
  }

  public void swapAdmission(Patient first, Patient second){
    // TODO patients swap admission dates and rooms
  }

  public int getMovablePatient(int day){
    int random;
    Patient patient;
    List<Patient> registeredPatients = patients.get(day);
    do{
      random = (int)(Math.random() * registeredPatients.size());
      patient = registeredPatients.get(random);
    } while (patient.getDischargeDate() < day);
    return random;
  }

  public void calculateCosts() {
    int features = 0, preference = 0, dept = 0, fixedGender = 0, dynamicGender = 0, transfer = 0;
    List<Patient> allPatients = patients.get(nDays - 1);
    for (Patient p : allPatients) {
      int previousRoom = p.getAssignedRoom(p.getAdmissionDate() - 1);
      for (int i = p.getAdmissionDate(); i < p.getDischargeDate(); i++) {
        if (previousRoom != p.getAssignedRoom(i) && previousRoom != -1) {
          transfer++;
          previousRoom = p.getAssignedRoom(i);
        }
        Room room = rooms.get(p.getAssignedRoom(i));
        features += room.getPreferredPropertiesPenalty(p.getPreferredProperties());
        if (room.getCapacityPenalty(p.getPreferredCapacity()) > 0) preference++;
        if (room.getTreatmentPenalty(p.getNeededSpecialism()) > 0) dept++;
        if (room.getGenderPenalty(p.getGender()) > 0) fixedGender++;
      }
    }


    for (int i = 0; i < nRooms; i++) {
      Room room = rooms.get(i);
      if (room.hasDynamicGenderPolicy()) {
        for (int j = 0; j < nDays; j++) {
          int male = 0, female = 0;
          for (Patient p : schedule[i][j]) {
            if (p.getAssignedRoom(j) != i) System.out.println("This is your fault");
            if (p.getGender().equals("Male")) male++;
            else female++;
          }
          if (Math.min(male, female) > 0) dynamicGender++;
        }
      }
    }

    int PRC = features * 20 + preference * 10 + dept * 20 + fixedGender * 50;
    System.out.println("COSTS:");
    System.out.println("Patient/room total cost = " + PRC + ": features = "
        + features + " X 20, preference = " + preference + " X 10, dept = "
        + dept + " X 20, fixed gender = " + fixedGender + " X 50");
    System.out.println("Gender cost = " + dynamicGender * 50);
    System.out.println("Transfer cost = " + transfer * 100);
  }
}
