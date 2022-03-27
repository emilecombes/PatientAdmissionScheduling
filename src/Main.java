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
    Schedule schedule = new Schedule(roomList, patientList);
    Solver solver = new Solver(patientList, roomList, schedule);

    solver.setPenalty("roomProperty", 20);
    solver.setPenalty("capacityPreference", 10);
    solver.setPenalty("speciality", 20);
    solver.setPenalty("gender", 50);
    solver.setPenalty("transfer", 100);
    solver.setPenalty("delay", 5);
    solver.setPenalty("capacityViolation", 1000);

    solver.init();
    solver.solve();
    xmlParser.writeSolution(solver);
  }
}
