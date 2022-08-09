import model.*;
import util.*;

public class Main {
  public static void main(String[] args) {
    String[] path_and_file = System.getProperty("instance").split("/Instances/");
    Variables.PATH = path_and_file[0];
    Variables.INSTANCE = path_and_file[1].split("\\.")[0];
    Variables.INSTANCE_SCALE = 1;
    if (Variables.INSTANCE.contains("dept4")) Variables.INSTANCE_SCALE *= 2;
    else if (Variables.INSTANCE.contains("dept6")) Variables.INSTANCE_SCALE *= 3;
    if (Variables.INSTANCE.contains("long")) Variables.INSTANCE_SCALE *= 2;

    Variables.START_TIME = System.currentTimeMillis();
    Variables.EXTEND = 2;
    Variables.TIME_LIMIT = -1;

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

    Variables.T_START = 533;
    Variables.T_STOP = 0.71;
    Variables.ALPHA = 0.99;
    Variables.INITIAL_TOTAL_ITERATIONS = (int) Math.pow(10, 7) * Variables.INSTANCE_SCALE;
    Variables.SUBPROBLEM_TOTAL_ITERATIONS = (int) Math.pow(10, 5) * Variables.INSTANCE_SCALE;
    Variables.REPAIR_TOTAL_ITERATIONS = (int) Math.pow(10, 5) * Variables.INSTANCE_SCALE;
    Variables.RND_ITERATIONS = (int) Math.pow(10, 2) * Variables.INSTANCE_SCALE;

    double s = Math.log10(Variables.ALPHA) / Math.log10(Variables.T_STOP / Variables.T_START);
    Variables.INIT_ITERATIONS = (int) (s * Variables.INITIAL_TOTAL_ITERATIONS);
    Variables.SUB_ITERATIONS = (int) (s * Variables.SUBPROBLEM_TOTAL_ITERATIONS);
    Variables.REP_ITERATIONS = (int) (s * Variables.REPAIR_TOTAL_ITERATIONS);

    Variables.EXHAUSTIVE = false;
    Variables.SWAP_LOOPS = 25;
    Variables.PCR = 28;
    Variables.PSR = 28;
    Variables.PSHA = 14;
    Variables.PSWA = 100 - Variables.PCR - Variables.PSR - Variables.PSHA;

    Variables.WE_MIN = 0;
    Variables.DELTA = 5;
    Variables.PENALTY_COEFFICIENT = 1;
    Variables.PENALTY_ADJUSTMENT = 0.95;
    Variables.TRADEOFF = 2;

    Solver solver = new Solver();
    solver.preProcessing();
    solver.hbs();

//    System.out.println("Validator: ./or_pas_validator Instances/" + instance + ".xml ." +
//        "./solutions/xml/" + instance + "_sol.xml");
  }
}