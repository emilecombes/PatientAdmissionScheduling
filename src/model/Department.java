package model;

import java.util.*;

public class Department {
  private final Map<Integer, Room> rooms;
  private final Set<String> mainSpecialisms;
  private final Set<String> auxSpecialisms;

  public Department(Map<Integer, Room> rooms, Set<String> mainSpec, Set<String> auxSpec) {
    this.rooms = rooms;
    this.mainSpecialisms = mainSpec;
    this.auxSpecialisms = auxSpec;
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
