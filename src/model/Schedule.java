package model;

import util.DateConverter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Schedule {
  private final RoomList roomList;
  private final PatientList patientList;
  private final Set<Integer>[][] schedule;
  private int[][] dynamicGenderViolations;
  private int[][] capacityViolations;

  public Schedule(RoomList rl, PatientList pl) {
    roomList = rl;
    patientList = pl;
    schedule = new Set[roomList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    for (int i = 0; i < roomList.getNumberOfRooms(); i++)
      for (int j = 0; j < DateConverter.getTotalHorizon(); j++)
        schedule[i][j] = new HashSet<>();
    dynamicGenderViolations = new int[roomList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
    capacityViolations = new int[roomList.getNumberOfRooms()][DateConverter.getTotalHorizon()];
  }

  public int getDynamicGenderViolations() {
    int violations = 0;
    for (int r = 0; r < roomList.getNumberOfRooms(); r++) {
      Room room = roomList.getRoom(r);
      if (room.hasGenderPolicy("Any")) {
        for (int day = 0; day < DateConverter.getTotalHorizon(); day++) {
          int female = 0;
          int male = 0;
          for (Integer p : schedule[r][day]) {
            Patient patient = patientList.getPatient(p);
            if (patient.getGender().equals("Male")) male++;
            else female++;
          }
          if(Math.min(female, male) > 0) violations++;
        }
      }
    }
    return violations;
  }

  public Patient getSwapRoomPatient(int pat) {
    Patient firstPat = patientList.getPatient(pat);
    int firstRoom = firstPat.getRoom(firstPat.getAdmission());
    for (int feasibleRoom : firstPat.getFeasibleRooms()) {

    }
    // First count to 3 in feasRooms btwn ad & dd
    return null;
  }

  public Patient getSwapAdmissionPatient(int pat) {
    // Don't count, there will always be a feasible patient
    return null;
  }

  public void assignPatient(Patient pat, int room, int day) {
    pat.assignRoom(room, day);
    schedule[room][day].add(pat.getId());
    if (roomList.getRoom(room).hasGenderPolicy("Any")) {

    }
  }

  public void cancelPatient(Patient pat) {
    int ad = pat.getAdmission();
    int dd = pat.getDischarge();
    int room = pat.getRoom(dd);
    for (int i = ad; i < dd; i++) {
      pat.cancelRoom(i);
      schedule[room][i].remove(pat.getId());
    }
  }
}