package model;

import java.util.*;

public class Scheduler {
  private int nDepartments, nRooms, nFeatures, nPatients, nSpecialisms, nTreatments, nDays;
  private final int EQUIPMENT = 20, PREFERENCE = 10, SPECIALITY = 20, GENDER = 50, TRANSFER = 100,
      DELAY = 5, CAPACITY = 10000, EXTEND = 1;
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
  private String[] lastMove;

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
    this.startDay = new GregorianCalendar(Integer.parseInt(date[0]), Integer.parseInt(date[1]),
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

  public int getCurrentDay() {
    return currentDay;
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

  public List<String> getDateStrings() {
    List<String> dates = new ArrayList<>();
    for (String date : dayIndices.keySet()) {
      dates.add(dayIndices.get(date), date);
    }
    return dates;
  }

  public int getHorizonLength() {
    return nDays + EXTEND * nDays;
  }

  public String getSpecialism(String tr) {
    return treatmentSpecialismMap.get(tr);
  }

  public void writeDateIndices() {
    dayIndices = new HashMap<>();
    int horizon = nDays * (1 + EXTEND);
    for (int i = 0; i <= horizon; i++) {
      GregorianCalendar date = startDay;
      dayIndices.put(getDateString(date), i);
      date.add(Calendar.DAY_OF_YEAR, 1);
    }
    startDay.add(Calendar.DAY_OF_YEAR, -horizon - 1);
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
  }

  public void assignInitialPatients() {
    schedule = new Set[nRooms][nDays * (EXTEND + 1)];
    for (int i = 0; i < nRooms; i++) {
      for (int j = 0; j < schedule[0].length; j++)
        schedule[i][j] = new HashSet<>();
    }

    for (int i = 0; i < patients.get(0).size(); i++) {
      Patient patient = patients.get(0).get(i);
      if (patient.getAssignedRoom(0) != -1) for (int j = 0; j < patient.getDischargeDate(); j++)
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
      if (patient.getAssignedRoom(currentDay - 1) != -1) for (int j = 0; j < nRooms; j++)
        if (j != patient.getAssignedRoom(currentDay) && penaltyMatrix[i][j] != -1)
          penaltyMatrix[i][j] += TRANSFER;
    }
  }

  public void insertPatients() {
    List<Patient> registeredPatients = getRegisteredPatients();
    for (int pat = 0; pat < registeredPatients.size(); pat++) {
      Patient patient = registeredPatients.get(pat);
      if (patient.getLastRoom() == -1) {
        int room = getFeasibleRoom(pat);
        for (int i = patient.getAdmissionDate(); i < patient.getDischargeDate(); i++)
          assignRoom(patient, room, i);
      }
    }
  }

  public void simulatedAnnealing() {
    double temperature = 155;
    double endTemperature = 1.55;
    double alpha = 0.999;
    int iterations = 1000;
    int savings;

    while (temperature > endTemperature) {
      for (int i = 0; i < iterations; i++) {

      }
    }
  }

  public int doMove() {
    // Return savings of a valid move with prob: 0.49 CR, 0.35 SR, 0.01 ShA, 0.15 SwA
    // Save last move globally
    return 0;
  }

  public void undoMove() {

  }

  public void solve() {
    List<Patient> registeredPatients = getRegisteredPatients();
    int cost = getCost();
    System.out.println("Cost on start day " + currentDay + ": " + cost + "\t violations: " +
        getCapacityViolations());
    cost += CAPACITY * getCapacityViolations();

    int N = 1000;
    for (int i = 0; i < N; i++) {
      int firstPat = getShiftPatient();
      Patient firstPatient = getRegisteredPatients().get(firstPat);
      int secPat;
      Patient secPatient;
      int ad = firstPatient.getActualAdmission();
      int dd = firstPatient.getActualDischarge();
      int count = 0;
      do {
        secPat = getSwapPatient(firstPat);
        secPatient = getRegisteredPatients().get(secPat);
        count++;
      }
      while (secPatient.getActualAdmission() < currentDay
          || !secPatient.isAdmittedOn(ad, dd)
          || !firstPatient.canHaveAdmissionDate(secPatient.getActualAdmission())
          || !secPatient.canHaveAdmissionDate(firstPatient.getActualAdmission())
          && count < 1000);

      if (count < 1000) {
        int savings = swapAdmission(firstPat, secPat);
        if (savings > 0) cost -= savings;
        else swapAdmission(secPat, firstPat);
      }
    }

    cost -= CAPACITY * getCapacityViolations();
    System.out.println(
        "Cost on end of day " + currentDay + ": " + cost + " (" + getCost() + ")\t violations: " +
            getCapacityViolations());
  }

  public int changeRoom(int p, int newRoom) {
    Patient patient = patients.get(currentDay).get(p);
    int originalRoom = patient.getLastRoom();
    int firstDay = Math.max(patient.getActualAdmission(), currentDay);

    int removedCost = penaltyMatrix[p][originalRoom];
    for (int i = firstDay; i < patient.getActualDischarge(); i++) {
      boolean gender = getGenderViolations(originalRoom, i) > 0;
      int cap = getCapacityViolations(originalRoom, i);
      cancelRoom(patient, i);
      if (gender && getGenderViolations(originalRoom, i) == 0) removedCost += GENDER;
      removedCost += CAPACITY * (cap - getCapacityViolations(originalRoom, i));
    }

    int addedCost = penaltyMatrix[p][newRoom];
    for (int i = firstDay; i < patient.getActualDischarge(); i++) {
      boolean gender = getGenderViolations(newRoom, i) > 0;
      int cap = getCapacityViolations(newRoom, i);
      assignRoom(patient, newRoom, i);
      if (!gender && getGenderViolations(newRoom, i) > 0) addedCost += GENDER;
      addedCost += CAPACITY * (getCapacityViolations(newRoom, i) - cap);
    }

    return removedCost - addedCost;
  }

  public int swapRooms(int firstPatient, int secondPatient) {
    int firstRoom = getRegisteredPatients().get(firstPatient).getLastRoom();
    int secondRoom = getRegisteredPatients().get(secondPatient).getLastRoom();
    int savings = changeRoom(firstPatient, secondRoom);
    savings += changeRoom(secondPatient, firstRoom);
    return savings;
  }

  public int shiftAdmission(int pat, int shift) {
    Patient patient = getRegisteredPatients().get(pat);
    int room = patient.getLastRoom();
    int savings = 0;

    // Cancel patient
    for (int i = patient.getActualAdmission(); i < patient.getActualDischarge(); i++) {
      if (getCapacityViolations(room, i) > 0) savings += CAPACITY;
      boolean gender = getGenderViolations(room, i) > 0;
      cancelRoom(patient, i);
      if (gender && getGenderViolations(room, i) == 0) savings += GENDER;
    }

    // Assign patient
    patient.addDelay(shift);
    for (int i = patient.getActualAdmission(); i < patient.getActualDischarge(); i++) {
      boolean gender = getGenderViolations(room, i) > 0;
      assignRoom(patient, room, i);
      if (!gender && getGenderViolations(room, i) > 0) savings -= GENDER;
      if (getCapacityViolations(room, i) > 0) savings -= CAPACITY;
    }

    return savings - shift * DELAY;
  }

  public int swapAdmission(int firstPat, int secPat) {
    Patient firstPatient = getRegisteredPatients().get(firstPat);
    Patient secPatient = getRegisteredPatients().get(secPat);
    int delta = secPatient.getActualAdmission() - firstPatient.getActualAdmission();

    return shiftAdmission(firstPat, delta) + shiftAdmission(secPat, -delta)
        + swapRooms(firstPat, secPat);
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
    List<Patient> registeredPatients = getRegisteredPatients();
    do {
      random = (int) (Math.random() * registeredPatients.size());
      patient = registeredPatients.get(random);
    } while (patient.getActualDischarge() <= currentDay);
    return random;
  }

  public int getShiftPatient() {
    int random;
    do random = getMovablePatient();
    while (getRegisteredPatients().get(random).getAssignedRoom(currentDay - 1) != -1
        || getRegisteredPatients().get(random).getMaxAdmission() == currentDay);
    return random;
  }

  public int getAdmittedPatient(int start, int end) {
    int patient;
    do patient = getMovablePatient();
    while (!getRegisteredPatients().get(patient).isAdmittedOn(start, end));
    return patient;
  }

  public int getSwapPatient(int pat) {
    Patient patient = getRegisteredPatients().get(pat);
    int firstRoom = patient.getLastRoom();
    int start = Math.max(currentDay, patient.getActualAdmission());
    int end = patient.getActualDischarge();

    int sp;
    int secondRoom;
    int count = 0;
    do {
      sp = getAdmittedPatient(start, end);
      Patient swapPatient = getRegisteredPatients().get(sp);
      secondRoom = swapPatient.getLastRoom();
      count++;
    } while (sp == pat || penaltyMatrix[pat][secondRoom] == -1 ||
        penaltyMatrix[sp][firstRoom] == -1 && count < 1000);
    return (count < 1000) ? sp : -1;
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
      for (int i = p.getActualAdmission(); i < p.getActualDischarge(); i++) {
        Room room = rooms.get(p.getAssignedRoom(i));
        features += room.getPreferredPropertiesPenalty(p.getPreferredProperties());
        preference += room.getCapacityPenalty(p.getPreferredCapacity());
        dept += room.getTreatmentPenalty(p.getNeededSpecialism());
        fixedGender += room.getGenderPenalty(p.getGender());
      }
    }
    int total =
        EQUIPMENT * features + PREFERENCE * preference + SPECIALITY * dept + GENDER * fixedGender;
    return new int[]{total, features, preference, dept, fixedGender};
  }

  public int getGenderViolations() {
    int dynamicGender = 0;
    for (int i = 0; i < nRooms; i++)
      for (int j = 0; j < (1 + EXTEND) * nDays; j++)
        if (getGenderViolations(i, j) != 0) dynamicGender++;
    return dynamicGender;
  }

  public int getCapacityViolations() {
    int cap = 0;
    for (int i = 0; i < nRooms; i++)
      for (int j = 0; j < (1 + EXTEND) * nDays; j++) cap += getCapacityViolations(i, j);
    return cap;
  }

  public int getTransfers() {
    int transfer = 0;
    List<Patient> registeredPatients = getRegisteredPatients();
    for (Patient p : registeredPatients) transfer += p.getTransfers();
    return transfer;
  }

  public int getDelays() {
    int delays = 0;
    for (Patient p : getRegisteredPatients()) delays += p.getDelay();
    return delays;
  }

  public int getCost() {
    return getPRCosts()[0] + GENDER * getGenderViolations() + TRANSFER * getTransfers() +
        DELAY * getDelays();
  }

}
