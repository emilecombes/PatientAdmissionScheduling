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
    xmlParser.buildDepartmentList();
    xmlParser.buildPatientList();

    Solver solver = new Solver();


    solver.preProcessing();
    solver.initSchedule();
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
