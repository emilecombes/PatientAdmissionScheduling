import model.*;
import util.*;

import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    Variables.START_TIME = System.currentTimeMillis();
    Variables.RESET = false;
    Variables.EXTEND = 14;
    Variables.EXHAUSTIVE = false;
    Variables.TIME_LIMIT = 20000;
    Variables.PC_MAX = (int) Math.pow(10, 4);
    Variables.WE_MIN = 0;
    Variables.DELTA = 1;

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
    Variables.SWAP_LOOPS = Integer.parseInt(System.getProperty("swap_loops"));
    Variables.ITERATIONS = Integer.parseInt(System.getProperty("iterations"));
    if (Variables.INSTANCE.contains("dept4")) {
      Variables.TIME_LIMIT *= 2;
      Variables.ITERATIONS *= 2;
    } else if (Variables.INSTANCE.contains("dept6")) {
      Variables.TIME_LIMIT *= 3;
      Variables.ITERATIONS *= 3;
    }
    if (Variables.INSTANCE.contains("long")) {
      Variables.TIME_LIMIT *= 2;
      Variables.ITERATIONS *= 2;
    }

    Variables.T_START = Integer.parseInt(System.getProperty("t_start"));
    Variables.T_STOP = Double.parseDouble(System.getProperty("t_stop"));
    Variables.ALPHA = Double.parseDouble(System.getProperty("alpha"));
    Variables.T_ITERATIONS = (int) (Variables.ITERATIONS * Math.log10(Variables.ALPHA) /
        Math.log10(Variables.T_STOP / Variables.T_START));

    Variables.PCR = Integer.parseInt(System.getProperty("pcr"));
    Variables.PSR = Integer.parseInt(System.getProperty("psr"));
    Variables.PSHA = Integer.parseInt(System.getProperty("psha"));
    Variables.PSWA = 100 - Variables.PCR - Variables.PSR - Variables.PSHA;


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