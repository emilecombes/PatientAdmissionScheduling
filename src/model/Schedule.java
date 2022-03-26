package model;

import java.util.HashSet;
import java.util.Set;

public class Schedule {
  private final Set<Integer>[][] schedule;

  public Schedule(int rooms, int days) {
    schedule = new Set[rooms][days];
    for (int i = 0; i < rooms; i++) {
      for (int j = 0; j < days; j++) {
        schedule[i][j] = new HashSet<>();
      }
    }
  }

  public Patient getSwapRoomPatient(int pat) {
    // First count to 3 in feasRooms btwn ad & dd
    return null;
  }

  public Patient getSwapAdmissionPatient(int pat) {
    // Don't count, there will always be a feasible patient
    return null;
  }

  public void assignPatient(Patient pat, int room) {
    for(int i = pat.getAdmission(); i < pat.getDischarge(); i++){
      pat.assignRoom(room, i);
      schedule[room][i].add(pat.getId());
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