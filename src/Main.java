import model.*;
import util.*;

// Run validator with: ./or_pas_validator Instances/or_pas_dept2_short01.xml
// ../out/solutions/or_pas_dept2_short01_sol.xml in ./data

public class Main {
  public static void main(String[] args) {
    int extend = 14;
    String instance = "or_pas_dept6_long01";

    XMLParser xmlParser = new XMLParser(instance);
    xmlParser.buildDateConverter(extend);
    DepartmentList departmentList = xmlParser.buildDepartmentList();
    PatientList patientList = xmlParser.buildPatientList();
    Schedule schedule = new Schedule(departmentList, patientList);
    Solver solver = new Solver(patientList, departmentList, schedule);

    solver.setPenalty("room_property", 20);
    solver.setPenalty("capacity_preference", 10);
    solver.setPenalty("speciality", 20);
    solver.setPenalty("gender", 50);
    solver.setPenalty("transfer", 100);
    solver.setPenalty("delay", 5);
    solver.setPenalty("capacity_violation", 1000);

    solver.init();
    solver.solve();
    xmlParser.writeSolution(solver);
    System.out.println("\t\t\t\t\t\t" + solver.getCost());

    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
        "./out/solutions/" + instance + "_sol.xml");
  }
}
