package ai.commerceiq.schemamanagement.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceLoggingUtils {

  public static String getStackTrace(Exception exception) {
    StringWriter writer = new StringWriter();
    exception.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
