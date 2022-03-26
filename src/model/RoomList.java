package model;

import java.util.*;

public class RoomList {
  private final List<Room> rooms;
  private static Map<String, Integer> roomIndices;
  private static Map<String, Set<Room>> departments;
  private static Map<String, String> specialisms;         // treatment - specialism
  private final Map<String, Set<String>> mainSpecialisms; // department - specialisms
  private final Map<String, Set<String>> mainDepartments; // specialism - departments
  private final Map<String, Set<String>> auxSpecialisms;  // department - specialisms
  private final Map<String, Set<String>> auxDepartments;  // specialism - departments

  public RoomList(List<Room> rooms, List<String> depNames) {
    this.rooms = rooms;
    departments = new HashMap<>();
    roomIndices = new HashMap<>();
    specialisms = new HashMap<>();
    mainSpecialisms = new HashMap<>();
    mainDepartments = new HashMap<>();
    auxSpecialisms = new HashMap<>();
    auxDepartments = new HashMap<>();

    for (String d : depNames) {
      departments.computeIfAbsent(d, k -> new HashSet<>());
      mainSpecialisms.computeIfAbsent(d, k -> new HashSet<>());
      auxSpecialisms.computeIfAbsent(d, k -> new HashSet<>());
    }

    for (Room room : rooms) {
      roomIndices.put(room.getName(), room.getId());
      departments.get(room.getDepartment()).add(room);
    }
  }

  public void addSpecialism(String treatment, String specialism) {
    specialisms.put(treatment, specialism);
  }

  public void addMainSpecialism(String dep, String spec) {
    mainSpecialisms.get(dep).add(spec);
    mainDepartments.computeIfAbsent(spec, k -> new HashSet<>());
    mainDepartments.get(spec).add(dep);
  }

  public void addAuxSpecialism(String dep, String spec) {
    auxSpecialisms.get(dep).add(spec);
    auxDepartments.computeIfAbsent(spec, k -> new HashSet<>());
    auxDepartments.get(spec).add(dep);
  }

  public Room getRoom(int index) {
    return rooms.get(index);
  }

  public Set<Room> getRoomsForTreatment(String treatment) {
    Set<Room> treatmentRooms = new HashSet<>();
    treatmentRooms.addAll(getMainRoomsForTreatment(treatment));
    treatmentRooms.addAll(getAuxRoomsForTreatment(treatment));
    return treatmentRooms;
  }

  public Set<Room> getMainRoomsForTreatment(String treatment) {
    String specialism = specialisms.get(treatment);
    Set<Room> treatmentRooms = new HashSet<>();
    if (mainDepartments.containsKey(specialism))
      for (String dep : mainDepartments.get(specialism))
        treatmentRooms.addAll(getDepartmentRooms(dep));
    return treatmentRooms;
  }

  public Set<Room> getAuxRoomsForTreatment(String treatment) {
    String specialism = specialisms.get(treatment);
    Set<Room> treatmentRooms = new HashSet<>();
    if (auxDepartments.containsKey(specialism))
      for (String dep : auxDepartments.get(specialism))
        treatmentRooms.addAll(getDepartmentRooms(dep));
    return treatmentRooms;
  }

  public Set<Room> getDepartmentRooms(String dep) {
    return departments.get(dep);
  }

  public static int getRoomIndex(String name) {
    return roomIndices.get(name);
  }

  public int getNumberOfRooms() {
    return rooms.size();
  }
}
