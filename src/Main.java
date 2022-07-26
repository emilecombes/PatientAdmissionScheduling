import model.*;
import util.*;

import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    Variables.RESET = false;
    Variables.EXTEND = 14;
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

    String[] i = System.getProperty("instance").split("/");
    Variables.INSTANCE = i[i.length-1].split("\\.")[0];
    Variables.PATH = System.getProperty("path");

    Variables.PCR = Integer.parseInt(System.getProperty("pcr"));
    Variables.PSR = Integer.parseInt(System.getProperty("psr"));
    Variables.PSHA = Integer.parseInt(System.getProperty("psha"));
    Variables.EXHAUSTIVE = System.getProperty("exhaustive").equals("1");
    Variables.SWAP_LOOPS = Integer.parseInt(System.getProperty("swap_loops"));
    Variables.PC_START_TEMP = Double.parseDouble(System.getProperty("t_start"));
    Variables.PC_STOP_TEMP = Double.parseDouble(System.getProperty("t_stop"));
    Variables.PC_ITERATIONS = Integer.parseInt(System.getProperty("iterations"));
    Variables.PC_ALPHA = Double.parseDouble(System.getProperty("alpha"));

    XMLParser xmlParser = new XMLParser();
    xmlParser.buildDateConverter();
    xmlParser.buildDepartmentList();
    xmlParser.buildPatientList();

    Solver solver = new Solver();

    solver.preProcessing();
    solver.initSchedule();
    solver.optimizePatientCost();
    xmlParser.writeSolution(solver);
    solver.printCost();

    CSVParser csvParser = new CSVParser(solver);
//    csvParser.buildMoveInfoCSV();
//    csvParser.buildScheduleCSV();
//
//    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
//        "./solutions/xml/" + instance + "_sol.xml");
  }
}
