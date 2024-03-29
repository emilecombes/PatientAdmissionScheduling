import model.*;
import util.*;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    String[] path_and_file = System.getProperty("instance").split("/Instances/");
    Variables.PATH = path_and_file[0];
    Variables.INSTANCE = path_and_file[1].split("\\.")[0];
    Variables.INSTANCE_SCALE = 1;
    if (Variables.INSTANCE.contains("test")) Variables.INSTANCE_SCALE /= 10;
    if (Variables.INSTANCE.contains("dept4")) Variables.INSTANCE_SCALE *= 2;
    else if (Variables.INSTANCE.contains("dept6")) Variables.INSTANCE_SCALE *= 3;
    if (Variables.INSTANCE.contains("long")) Variables.INSTANCE_SCALE *= 2;

    Variables.START_TIME = System.currentTimeMillis();
    Variables.EXTEND = 2;
    Variables.TIME_LIMIT = -1;
    Variables.UPDATE_ITERATIONS = 100;

    Variables.ROOM_PROP_PEN = 20;
    Variables.PREF_CAP_PEN = 10;
    Variables.SPECIALITY_PEN = 20;
    Variables.GENDER_PEN = 50;
    Variables.DELAY_PEN = 5;
    Variables.TRANSFER_PEN = 100;
    Variables.CAP_VIOL_PEN = 1000;

    XMLParser xmlParser = new XMLParser();
    xmlParser.buildDateConverter();
    xmlParser.buildDepartmentList();
    xmlParser.buildPatientList();

    Variables.T_START = 531;
    Variables.T_STOP = 0.71;
    Variables.ALPHA = 0.99;
    Variables.INITIAL_TOTAL_ITERATIONS = (int) (5 * Math.pow(10, 6) * Variables.INSTANCE_SCALE);
    Variables.SUBPROBLEM_TOTAL_ITERATIONS = (int) (Math.pow(10, 6) * Variables.INSTANCE_SCALE);
    Variables.REPAIR_TOTAL_ITERATIONS = (int) (Math.pow(10, 3) * Variables.INSTANCE_SCALE);
    Variables.RND_ITERATIONS = (int) (Math.pow(10, 3) * Variables.INSTANCE_SCALE);

    double s = Math.log10(Variables.ALPHA) / Math.log10(Variables.T_STOP / Variables.T_START);
    Variables.INIT_ITERATIONS = (int) (s * Variables.INITIAL_TOTAL_ITERATIONS);
    Variables.SUB_ITERATIONS = (int) (s * Variables.SUBPROBLEM_TOTAL_ITERATIONS);
    Variables.REP_ITERATIONS = (int) (s * Variables.REPAIR_TOTAL_ITERATIONS);

    Variables.EXHAUSTIVE = false;
    Variables.SWAP_LOOPS = 25;
    Variables.PCR = 38;
    Variables.PSR = 28;
    Variables.PSHA = 14;
    Variables.PSWA = 100;

    Variables.WE_MIN = 0;
    Variables.PC_MAX = Integer.MAX_VALUE;
    Variables.DELTA = 5;
    Variables.PENALTY_COEFFICIENT = 1;
    Variables.CONSTANT_PENALTY_ZONE = 0.05;
    Variables.PENALTY_UPDATE_OFFSET = 0.05;
    Variables.PENALTY_INCREASE = 1.2;
    Variables.PENALTY_DECREASE = 0.85;
    Variables.REPAIR_INCREASE = 10;
    Variables.TRADEOFF = 2;
    Variables.MAX_HBS_ITERATIONS = 25;

    Solver solver = new Solver();
    solver.preProcessing();
    List<Solution> solutions = solver.hbs();
    for(Solution sol : solutions) xmlParser.writeSolution(sol);

//    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
//        "./solutions/xml/" + instance + "_sol.xml");
  }
}