package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentList {
  private static Map<String, Integer> roomIndices;
  private static List<Room> allRooms;
  private static Map<String, Department> departments;
  private static Map<Integer, Department> integerDepartmentMap;
  private final Map<String, String> treatmentToSpecialism;

  public DepartmentList(List<Room> rooms, Map<String, Department> deps, Map<String, String> specs) {
    allRooms = rooms;
    departments = deps;
    integerDepartmentMap = new HashMap<>();
    for (String key : departments.keySet())
      integerDepartmentMap.put(departments.get(key).getId(), departments.get(key));
    treatmentToSpecialism = specs;
    roomIndices = new HashMap<>();
    for (int i = 0; i < allRooms.size(); i++) {
      Room room = allRooms.get(i);
      roomIndices.put(room.getName(), i);
    }
  }

  public static int getRoomIndex(String name) {
    return roomIndices.get(name);
  }

  public static String getRoomId(int idx) {
    return allRooms.get(idx).getName();
  }

  public static int getDepartmentId(String name) {
    return departments.get(name).getId();
  }

  public Department getDepartment(String name) {
    return departments.get(name);
  }

  public Department getDepartment(int id) {
    return integerDepartmentMap.get(id);
  }

  public Room getRoom(int id) {
    return allRooms.get(id);
  }

  public int getNumberOfRooms() {
    return allRooms.size();
  }

  public int getNumberOfDepartments() {
    return departments.size();
  }

  public String getNeededSpecialism(String treatment) {
    return treatmentToSpecialism.get(treatment);
  }

  public List<Department> getDepartmentsForTreatment(String treatment) {
    String specialism = getNeededSpecialism(treatment);
    List<Department> feasibleDepartments = new ArrayList<>();
    for (String dep : departments.keySet())
      if (getDepartment(dep).hasSpecialism(specialism))
        feasibleDepartments.add(departments.get(dep));

    return feasibleDepartments;
  }

}
