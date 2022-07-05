import model.*;
import util.*;

import java.io.IOException;
import java.util.Random;

public class Main {
  public static void main(String[] args) throws IOException {
    String instance = "or_pas_dept2_short00";
    boolean exhaustive = false;
    boolean reset = false;
    int extend = 14;
    int swapLoops = 10;
    int timeoutSol = 1000 * 60 * 5;
    int timeoutInst = 1000 * 60 * 5;
    double tradeoff = 1.25;

    new Variables(exhaustive, reset, extend, swapLoops, timeoutSol, timeoutInst, tradeoff);
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
    solver.optimizePatientCost();
    xmlParser.writeSolution(solver);
    solver.printCosts();

    CSVParser csvParser = new CSVParser(instance, solver);
    csvParser.buildMoveInfoCSV();
    csvParser.buildScheduleCSV();

    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
        "./solutions/xml/" + instance + "_sol.xml");
  }
}
