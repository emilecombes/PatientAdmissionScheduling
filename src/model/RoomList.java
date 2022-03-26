package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomList {
  private final List<Room> rooms;
  private static Map<String, Integer> roomIndices;

  public RoomList(List<Room> rooms) {
    this.rooms = rooms;
    roomIndices = new HashMap<>();
    for (Room room : rooms) roomIndices.put(room.getName(), room.getId());
  }

  public static int getRoomIndex(String name) {
    return roomIndices.get(name);
  }

}
