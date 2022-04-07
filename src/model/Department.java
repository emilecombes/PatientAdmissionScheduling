package model;

import util.DateConverter;

import java.util.*;

public class Department {
  private final int id;
  private final Map<Integer, Room> rooms;
  private final Set<String> mainSpecialisms;
  private final Set<String> auxSpecialisms;
  private final int size;
  private final List<Float> workLoads;
  private float workLoadVariance;

  public Department(int id, Map<Integer, Room> rooms, Set<String> mainSpec, Set<String> auxSpec) {
    this.id = id;
    this.rooms = rooms;
    this.mainSpecialisms = mainSpec;
    this.auxSpecialisms = auxSpec;

    int s = 0;
    for (int r : rooms.keySet()) s += rooms.get(r).getCapacity();
    size = s;

    workLoads = new ArrayList<>();
    for (int i = 0; i < DateConverter.getTotalHorizon(); i++) workLoads.add((float) 0);
  }

  public int getId() {
    return id;
  }

  public Set<Integer> getRoomIndices() {
    return rooms.keySet();
  }

  public float getWorkLoadVariance() {
    return workLoadVariance;
  }

  public boolean hasMainSpecialism(String spec) {
    return mainSpecialisms.contains(spec);
  }

  public boolean hasSpecialism(String spec) {
    return mainSpecialisms.contains(spec) || auxSpecialisms.contains(spec);
  }

  public void addWorkLoad(int day, int load) {
    workLoads.set(day, workLoads.get(day) + load / size);
  }

  public void removeWorkLoad(int day, int load) {
    workLoads.set(day, workLoads.get(day) - load / size);
  }

  public void calculateWorkLoadVariance() {
    float meanLoadVariance = 0;
    for (float load : workLoads)
      meanLoadVariance += load;
    meanLoadVariance = meanLoadVariance / size;

    workLoadVariance = 0;
    for (float load : workLoads)
      workLoadVariance += Math.pow(meanLoadVariance - load, 2);
  }
}
