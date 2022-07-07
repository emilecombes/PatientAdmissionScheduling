package util;

public class Variables {
  public static boolean EXHAUSTIVE, RESET;
  public static int EXTEND, SWAP_LOOPS, TIMEOUT_SOLUTION, TIMEOUT_INSTANCE;
  public static int ROOM_PROP_PEN, PREF_CAP_PEN, SPECIALITY_PEN, GENDER_PEN, TRANSFER_PEN,
      DELAY_PEN, CAP_VIOL_PEN ;
  public static double TRADEOFF;
  public static double PC_START_TEMP, PC_STOP_TEMP, PC_ITERATIONS, PC_ALPHA;
  public static double EC_START_TEMP, EC_STOP_TEMP, EC_ITERATIONS, EC_ALPHA;

  public Variables(boolean exh, boolean res, int ext, int sl, int ts, int ti, double to) {
    ROOM_PROP_PEN = 20;
    PREF_CAP_PEN = 10;
    SPECIALITY_PEN = 20;
    GENDER_PEN = 50;
    TRANSFER_PEN = 100;
    DELAY_PEN = 5;
    CAP_VIOL_PEN = 1000;

    EXHAUSTIVE = exh;
    RESET = res;
    EXTEND = ext;
    TIMEOUT_SOLUTION = ts;
    TIMEOUT_INSTANCE = ti;
    TRADEOFF = to;

    SWAP_LOOPS = sl;

    PC_START_TEMP = 155;
    PC_STOP_TEMP = 1.55;
    PC_ITERATIONS = 100;
    PC_ALPHA = 0.999;

    EC_START_TEMP = 155;
    EC_STOP_TEMP = 1.55;
    EC_ITERATIONS = 100;
    EC_ALPHA = 0.999;
  }
}
