package util;

public class Variables {
  public static boolean EXHAUSTIVE;
  public static boolean RESET;
  public static int EXTEND;
  public static int SWAP_LOOPS;
  public static int TIMEOUT_SOLUTION;
  public static int TIMEOUT_INSTANCE;
  public static double TRADEOFF;
  public static double PC_START_TEMP, PC_STOP_TEMP, PC_ITERATIONS, PC_ALPHA;
  public static double EC_START_TEMP, EC_STOP_TEMP, EC_ITERATIONS, EC_ALPHA;

  public Variables(boolean exh, boolean res, int ext, int sl, int ts, int ti, double to){
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
