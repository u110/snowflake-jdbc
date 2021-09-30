/*
 * Copyright (c) 2012-2019 Snowflake Computing Inc. All rights reserved.
 */
package net.snowflake.client.log;

import static net.snowflake.client.jdbc.SnowflakeUtil.systemGetProperty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import net.snowflake.client.jdbc.SnowflakeDriver;
import net.snowflake.client.util.SecretDetector;

/** SFFormatter */
public class SFFormatter extends Formatter {
  private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  static {
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static final String CLASS_NAME_PREFIX =
      SnowflakeDriver.class
          .getPackage()
          .getName()
          .substring(0, SnowflakeDriver.class.getPackage().getName().lastIndexOf('.'));

  public static final String INFORMATICA_V1_CLASS_NAME_PREFIX = "com.snowflake";

  public static final String SYS_PROPERTY_SF_FORMATTER_DUMP_STACKTRACE =
      "net.snowflake.jdbc.log.sfformatter.dump.stacktrace";

  @Override
  public String format(LogRecord record) {
    int lineNumber = -1;
    String className = record.getSourceClassName();
    final String methodName = record.getSourceMethodName();
    StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
    for (StackTraceElement ste : stackTraces) {
      if (className.equals(ste.getClassName()) && methodName.equals(ste.getMethodName())) {
        lineNumber = ste.getLineNumber();
        break;
      }
    }
    if (className.startsWith(CLASS_NAME_PREFIX)) {
      className = "n.s.c" + className.substring(CLASS_NAME_PREFIX.length());
    } else if (className.startsWith(INFORMATICA_V1_CLASS_NAME_PREFIX)) {
      className = "c.s" + className.substring(INFORMATICA_V1_CLASS_NAME_PREFIX.length());
    }

    String throwable = "";
    String dumpStack = systemGetProperty(SYS_PROPERTY_SF_FORMATTER_DUMP_STACKTRACE);
    if (dumpStack != null && dumpStack.equals("true") && record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = SecretDetector.maskSecrets(sw.toString());
    }

    StringBuilder builder = new StringBuilder(1000);
    builder.append(df.format(new Date(record.getMillis()))).append(" ");
    builder.append(className).append(" ");
    builder.append(record.getLevel()).append(" ");
    builder.append(methodName).append(":");
    builder.append(lineNumber).append(" - ");
    builder.append(formatMessage(record));
    builder.append(throwable);
    builder.append("\n");
    return builder.toString();
  }

  @Override
  public String getHead(Handler h) {
    return super.getHead(h);
  }

  @Override
  public String getTail(Handler h) {
    return super.getTail(h);
  }
}
