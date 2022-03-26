import model.*;
import util.*;

public class Main {
  public static void main(String[] args) {
    int extend = 14;
    String instance = "or_pas_dept2_short01";

    XMLParser xmlParser = new XMLParser(instance);
    xmlParser.buildDateConverter(extend);
    RoomList roomList = xmlParser.buildRoomList();
    PatientList patientList = xmlParser.buildPatientList();
    Schedule schedule = new Schedule(roomList.getNumberOfRooms(), DateConverter.getTotalHorizon());
    Solver solver = new Solver(patientList, roomList, schedule);
    solver.init();
    solver.solve();
  }
}
