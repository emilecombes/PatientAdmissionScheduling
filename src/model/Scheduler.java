package model;

import java.util.*;

public class Scheduler {
  private int nDepartments, nRooms, nFeatures, nPatients, nSpecialisms, nTreatments, nDays;
  private final int EQUIPMENT = 20, PREFERENCE = 10, SPECIALITY = 20, GENDER = 50, TRANSFER = 100,
      DELAY = 5, CAPACITY = 10000;
  private GregorianCalendar startDay;
  private HashMap<String, String> treatmentSpecialismMap;
  private List<Department> departments;
  private List<Room> rooms;
  private List<List<Patient>> patients;
  private Set<Patient>[][] schedule;
  private Map<String, Integer> roomIndices;
  private Map<String, Integer> dayIndices;
  private int[][] penaltyMatrix;
  private int currentDay;

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

  public List<Patient> getRegisteredPatients() {
    if (currentDay < nDays) return patients.get(currentDay);
    return patients.get(nDays - 1);
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
    currentDay = 0;
    while (currentDay < nDays) {
      buildPenaltyMatrix();
      insertPatients();
      solve();
      currentDay++;
    }
    currentDay--;
    sanityCheck();
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

  public void cancelRoom(Patient pat, int day) {
    int room = pat.getAssignedRoom(day);
    schedule[room][day].remove(pat);
    pat.cancelRoom(day);
  }

  public void buildPenaltyMatrix() {
    List<Patient> registeredPatients = getRegisteredPatients();
    penaltyMatrix = new int[registeredPatients.size()][nRooms];
    for (int i = 0; i < registeredPatients.size(); i++) {
      Patient patient = registeredPatients.get(i);
      for (int j = 0; j < nRooms; j++) {
        Room room = rooms.get(j);
        penaltyMatrix[i][j] = room.getRoomPenalty(patient);
        if (penaltyMatrix[i][j] != -1) penaltyMatrix[i][j] *= patient.getRestingLOT(currentDay);
      }
    }

    for (int i = 0; i < registeredPatients.size(); i++) {
      Patient patient = registeredPatients.get(i);
      if (patient.getAssignedRoom(currentDay) != -1)
        for (int j = 0; j < nRooms; j++)
          if (j != patient.getAssignedRoom(currentDay) && penaltyMatrix[i][j] != -1)
            penaltyMatrix[i][j] += TRANSFER;
    }
  }

  public void insertPatients() {
    for (int pat = 0; pat < patients.get(currentDay).size(); pat++) {
      Patient patient = patients.get(currentDay).get(pat);
      int room = patient.getAssignedRoom(patient.getAdmissionDate());
      if (room == -1) {
        do room = (int) (nRooms * Math.random());
        while (penaltyMatrix[pat][room] < 0);
      }
      for (int d = patient.getAdmissionDate(); d < patient.getDischargeDate(); d++)
        assignRoom(patient, room, d);
    }
  }

  public void solve() {
    List<Patient> registeredPatients = getRegisteredPatients();
    int cost = getCost();
    System.out.println("Cost on start day " + currentDay + ": " + cost + "\t violations: " +
        getCapacityViolations());
    cost += CAPACITY * getCapacityViolations();

    int N = 100000;
    for (int i = 0; i < N; i++) {
      // Change room move
      int pat = getMovablePatient();
      int newRoom = getFeasibleRoom(pat);
      int originalRoom = registeredPatients.get(pat).getLastRoom();

      int savings = changeRoom(pat, newRoom);
      if (savings >= 0) cost -= savings;
      else changeRoom(pat, originalRoom);
    }

    cost -= CAPACITY * getCapacityViolations();
    System.out.println("Cost on end day " + currentDay + ": " + cost + "\t violations: " +
        getCapacityViolations() + " (actual: " + getCost() + ")");
  }

  public int changeRoom(int p, int newRoom) {
    Patient patient = patients.get(currentDay).get(p);
    int originalRoom = patient.getLastRoom();
    int firstDay = Math.max(patient.getAdmissionDate(), currentDay);

    int removedCost = penaltyMatrix[p][originalRoom];
    for (int i = firstDay; i < patient.getDischargeDate(); i++) {
      boolean gender = getGenderViolations(originalRoom, i) > 0;
      int cap = getCapacityViolations(originalRoom, i);
      cancelRoom(patient, i);
      if (gender && getGenderViolations(originalRoom, i) == 0) removedCost += GENDER;
      removedCost += CAPACITY * (cap - getCapacityViolations(originalRoom, i));
    }

    int addedCost = penaltyMatrix[p][newRoom];
    for (int i = firstDay; i < patient.getDischargeDate(); i++) {
      boolean gender = getGenderViolations(newRoom, i) > 0;
      int cap = getCapacityViolations(newRoom, i);
      assignRoom(patient, newRoom, i);
      if (!gender && getGenderViolations(newRoom, i) > 0) addedCost += GENDER;
      addedCost += CAPACITY * (getCapacityViolations(newRoom, i) - cap);
    }

    return removedCost - addedCost;
  }

  public void swapRooms(Patient first, Patient second) {
    // TODO patients who have overlapping stays exchange their rooms
  }

  public void shiftAdmission(Patient patient, int days) {
    // TODO patient is delayed or advanced to min. planned admission, max. max admission
  }

  public void swapAdmission(Patient first, Patient second) {
    // TODO patients swap admission dates and rooms
  }

  public int getFeasibleRoom(int pat) {
    int room;
    do room = (int) (nRooms * Math.random());
    while (penaltyMatrix[pat][room] == -1);
    return room;
  }

  public int getMovablePatient() {
    int random;
    Patient patient;
    List<Patient> registeredPatients = patients.get(currentDay);
    do {
      random = (int) (Math.random() * registeredPatients.size());
      patient = registeredPatients.get(random);
    } while (patient.getDischargeDate() < currentDay);
    return random;
  }

  public int getCapacityViolations(int room, int day) {
    int cap = rooms.get(room).getCapacity();
    int load = schedule[room][day].size();
    return (load > cap) ? load - cap : 0;
  }

  public int getGenderViolations(int room, int day) {
    // Returns number of wrong gender.
    if (!rooms.get(room).hasDynamicGenderPolicy()) return 0;
    int male = 0;
    int female = 0;
    for (Patient p : schedule[room][day])
      if (p.isMale()) male++;
      else female++;
    return Math.min(male, female);
  }

  public int[] getPRCosts() {
    int features = 0, preference = 0, dept = 0, fixedGender = 0;
    List<Patient> registeredPatients = getRegisteredPatients();
    for (Patient p : registeredPatients) {
      for (int i = p.getAdmissionDate(); i < p.getDischargeDate(); i++) {
        Room room = rooms.get(p.getAssignedRoom(i));
        features += room.getPreferredPropertiesPenalty(p.getPreferredProperties());
        preference += room.getCapacityPenalty(p.getPreferredCapacity());
        dept += room.getTreatmentPenalty(p.getNeededSpecialism());
        fixedGender += room.getGenderPenalty(p.getGender());
      }
    }
    int total = EQUIPMENT * features + PREFERENCE * preference + SPECIALITY * dept
        + GENDER * fixedGender;
    return new int[]{total, features, preference, dept, fixedGender};
  }

  public int getGenderViolations() {
    int dynamicGender = 0;
    for (int i = 0; i < nRooms; i++)
      for (int j = 0; j < nDays; j++)
        if (getGenderViolations(i, j) != 0) dynamicGender++;
    return dynamicGender;
  }

  public int getCapacityViolations() {
    int cap = 0;
    for (int i = 0; i < nRooms; i++)
      for (int j = 0; j < nDays; j++)
        cap += getCapacityViolations(i, j);
    return cap;
  }

  public int getTransfers() {
    int transfer = 0;
    List<Patient> registeredPatients = getRegisteredPatients();
    for (Patient p : registeredPatients) {
      transfer += p.getTransfers();
    }
    return transfer;
  }

  public int getDelays() {
    return 0;
  }

  public int getCost() {
    return getPRCosts()[0] + GENDER * getGenderViolations()
        + TRANSFER * getTransfers() + DELAY * getDelays();
  }

  public void sanityCheck() {
    System.out.println("In assigned rooms of patients");
    int[][][] gCount = new int[nDays][nRooms][2];
    for (Patient p : getRegisteredPatients()) {
      for (int i = p.getAdmissionDate(); i < p.getDischargeDate(); i++) {
        if (p.isMale()) gCount[i][p.getAssignedRoom(i)][0]++;
        else gCount[i][p.getAssignedRoom(i)][1]++;
      }
    }

    int viol = 0;
    for(int i = 0; i < nDays; i++){
      for(int j = 0; j < nRooms; j++){
        if(rooms.get(j).hasDynamicGenderPolicy()){
          if(gCount[i][j][0] > 0 && gCount[i][j][1] > 0) viol++;
        }
      }
    }
    System.out.println("There are " + viol + " gender violating rooms.");


//    System.out.println("In the schedule:");
//    int roomViol = 0;
//    int total = 0;
//    for (int i = 0; i < nRooms; i++) {
//      for (int j = 0; j < nDays; j++) {
//        if (getGenderViolations(i, j) != 0) {
//          roomViol++;
//          total += getGenderViolations(i, j);
//          System.out.println("Room " + i + " has " + getGenderViolations(i, j) + " gender " +
//              "violations:");
//          for (Patient p : schedule[i][j])
//            System.out.print("Patient " + p.getName() + " (" + p.getGender() + "), ");
//          System.out.println();
//        }
//      }
//    }
//    System.out.println("There are " + roomViol + " wrong rooms on all days with " + total + " " +
//        "total violations");
  }
}
