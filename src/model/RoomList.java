package model;

import java.util.*;

public class RoomList {
  private final List<Room> rooms;
  private static Map<String, Integer> roomIndices;
  private static Map<String, Set<Room>> departments;
  private final Map<String, Set<String>> mainSpecialisms;
  private final Map<String, Set<String>> auxSpecialisms;

  public RoomList(List<Room> rooms, List<String> depNames) {
    this.rooms = rooms;
    departments = new HashMap<>();
    roomIndices = new HashMap<>();
    mainSpecialisms = new HashMap<>();
    auxSpecialisms = new HashMap<>();
    for(String d : depNames) {
      departments.computeIfAbsent(d, k -> new HashSet<>());
      mainSpecialisms.computeIfAbsent(d, k -> new HashSet<>());
      auxSpecialisms.computeIfAbsent(d, k -> new HashSet<>());
    }
    for (Room room : rooms) {
      roomIndices.put(room.getName(), room.getId());
      departments.get(room.getDepartment()).add(room);
    }
  }

  public void addMainSpecialism(String dep, String spec){
    mainSpecialisms.get(dep).add(spec);
  }

  public void addAuxSpecialism(String dep, String spec){
    auxSpecialisms.get(dep).add(spec);
  }

  public Room getRoom(int index){
    return rooms.get(index);
  }

  public Set<Room> getDepartmentRooms(String dep){
    return departments.get(dep);
  }

  public static int getRoomIndex(String name) {
    return roomIndices.get(name);
  }

}
