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

  public Variables(boolean exh, boolean res, int ext, int sl, int ts, int ti, double to){
    EXHAUSTIVE = exh;
    RESET = res;
    EXTEND = ext;
    SWAP_LOOPS = sl;
    TIMEOUT_SOLUTION = ts;
    TIMEOUT_INSTANCE = ti;
    TRADEOFF = to;

    PC_START_TEMP = 155;
    PC_STOP_TEMP = 1.55;
    PC_ITERATIONS = 100;
    PC_ALPHA = 0.999;
  }
}
