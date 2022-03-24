package util;

import java.util.*;

public class DateConverter {
  private static int numDays, extend;
  private static List<String> dateStrings;
  private static Map<String, Integer> dateIndices;

  public DateConverter(String startDay, int numDays, int extend) {
    DateConverter.numDays = numDays;
    DateConverter.extend = extend;
    dateStrings = new ArrayList<>();
    dateIndices = new HashMap<>();
    String[] d = startDay.split("-");
    GregorianCalendar date = new GregorianCalendar(
        Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2])
    );

    for (int i = 0; i < numDays + extend; i++) {
      String dateString = buildDateString(date);
      dateStrings.add(dateString);
      dateIndices.put(dateString, i);
      date.add(Calendar.DAY_OF_YEAR, 1);
    }

  }

  public String buildDateString(GregorianCalendar date) {
    StringBuilder sb = new StringBuilder();
    String day = String.valueOf(date.get(Calendar.DATE));
    if (day.length() == 1) day = "0" + day;
    String month = String.valueOf(date.get(Calendar.MONTH) + 1);
    if (month.length() == 1) month = "0" + month;
    String year = String.valueOf(date.get(Calendar.YEAR));
    sb.append(year);
    sb.append("-");
    sb.append(month);
    sb.append("-");
    sb.append(day);
    return sb.toString();
  }

  public String getDateString(int i){
    return dateStrings.get(i);
  }

  public int getDateIndex(String date){
    return dateIndices.get(date);
  }
}
