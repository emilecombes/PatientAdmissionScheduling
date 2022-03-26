package model;

import java.util.HashSet;
import java.util.Set;

public class Schedule {
  private Set[][] schedule;

  public Schedule(int rooms, int days) {
    schedule = new HashSet[rooms][days];
  }

  public Patient getSwapRoomPatient(int pat) {
    // First count to 3 in feasRooms btwn ad & dd
    return null;
  }

  public Patient getSwapAdmissionPatient(int pat) {
    // Don't count, there will always be a feasible patient
    return null;
  }
}