package model;

import java.util.*;

public class Department {
  private final int id, size;
  private final Map<Integer, Room> rooms;
  private final Set<String> mainSpecialisms;
  private final Set<String> auxSpecialisms;

  public Department(int id, Map<Integer, Room> rooms, Set<String> mainSpec, Set<String> auxSpec) {
    this.id = id;
    this.rooms = rooms;
    this.mainSpecialisms = mainSpec;
    this.auxSpecialisms = auxSpec;

    int s = 0;
    for (int r : rooms.keySet()) s += rooms.get(r).getCapacity();
    size = s;
  }

  public int getId() {
    return id;
  }

  public int getSize() {
    return size;
  }

  public Set<Integer> getRoomIndices() {
    return rooms.keySet();
  }

  public boolean hasMainSpecialism(String spec) {
    return mainSpecialisms.contains(spec);
  }

  public boolean hasSpecialism(String spec) {
    return mainSpecialisms.contains(spec) || auxSpecialisms.contains(spec);
  }

}
