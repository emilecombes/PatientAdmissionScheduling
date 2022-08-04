import model.*;
import util.*;

import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    Variables.START_TIME = System.currentTimeMillis();
    Variables.RESET = false;
    Variables.EXTEND = 14;
    Variables.EXHAUSTIVE = false;

    Variables.ROOM_PROP_PEN = 20;
    Variables.PREF_CAP_PEN = 10;
    Variables.SPECIALITY_PEN = 20;
    Variables.GENDER_PEN = 50;
    Variables.DELAY_PEN = 5;
    Variables.TRANSFER_PEN = 100;
    Variables.CAP_VIOL_PEN = 1000;

    String[] path_and_file = System.getProperty("instance").split("/Instances/");
    Variables.INSTANCE = path_and_file[1].split("\\.")[0];
    Variables.PATH = path_and_file[0];

    Variables.T_START = Integer.parseInt(System.getProperty("t_start"));
    Variables.T_STOP = Double.parseDouble(System.getProperty("t_stop"));
    Variables.T_ITERATIONS = Integer.parseInt(System.getProperty("t_iterations"));
    Variables.ALPHA = Double.parseDouble(System.getProperty("alpha"));

    Variables.PCR = Integer.parseInt(System.getProperty("pcr"));
    Variables.PSR = Integer.parseInt(System.getProperty("psr"));
    Variables.PSHA = Integer.parseInt(System.getProperty("psha"));
    Variables.PSWA = 100 - Variables.PCR - Variables.PSR - Variables.PSHA;

    Variables.SWAP_LOOPS = Integer.parseInt(System.getProperty("swap_loops"));
    Variables.TIME_LIMIT = 20000;
    if(Variables.INSTANCE.contains("dept4")) Variables.TIME_LIMIT *= 2;
    else if(Variables.INSTANCE.contains("dept6")) Variables.TIME_LIMIT *= 3;
    if(Variables.INSTANCE.contains("long")) Variables.TIME_LIMIT *= 2;


    XMLParser xmlParser = new XMLParser();
    xmlParser.buildDateConverter();
    xmlParser.buildDepartmentList();
    xmlParser.buildPatientList();

    Solver solver = new Solver();

    solver.preProcessing();
    solver.initSchedule();
    solver.optimizePatientCost();
//    xmlParser.writeSolution(solver);
    solver.printCost();

//    CSVParser csvParser = new CSVParser(solver);
//    csvParser.buildMoveInfoCSV();
//    csvParser.buildScheduleCSV();
//
//    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
//        "./solutions/xml/" + instance + "_sol.xml");
  }
}
