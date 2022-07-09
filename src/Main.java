import model.*;
import util.*;

import java.io.IOException;
import java.util.Random;

public class Main {
  public static void main(String[] args) throws IOException {
    Variables.EXHAUSTIVE = true;
    Variables.RESET = false;
    Variables.EXTEND = 14;
    Variables.SWAP_LOOPS = 25;
    Variables.TIMEOUT_SOLUTION = 1000 * 60 * 5;
    Variables.TIMEOUT_INSTANCE = 1000 * 60 * 8;
    Variables.TRADEOFF = 1.25;

    Variables.ROOM_PROP_PEN = 20;
    Variables.PREF_CAP_PEN = 10;
    Variables.SPECIALITY_PEN = 20;
    Variables.GENDER_PEN = 50;
    Variables.DELAY_PEN = 5;
    Variables.TRANSFER_PEN = 100;
    Variables.CAP_VIOL_PEN = 1000;

    String instance = System.getProperty("i");
    Variables.PC_START_TEMP = Double.parseDouble(System.getProperty("t0"));
    Variables.PC_STOP_TEMP = Double.parseDouble(System.getProperty("te"));
    Variables.PC_ITERATIONS = Integer.parseInt(System.getProperty("it"));
    Variables.PC_ALPHA = Double.parseDouble(System.getProperty("a"));

    XMLParser xmlParser = new XMLParser(instance);
    xmlParser.buildDateConverter();
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
