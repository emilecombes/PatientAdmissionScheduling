package model;

import java.util.Map;

public class Solver {
  private Map<String, Integer> penalties;
  private PatientList patientList;
  private RoomList roomList;
  private Schedule schedule;
  private int cost;
  private Map<String, String> lastMove; // ex: move="CR", patient="2", room="4", savings="20"

  public Solver(){}

  public void calculateRoomCosts(){}

  public void insertInitialPatients(){}

  public void assignPatients(){}

  public void solve(){}
}
