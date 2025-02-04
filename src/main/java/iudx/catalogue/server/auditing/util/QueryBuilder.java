package iudx.catalogue.server.auditing.util;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.auditing.util.Constants.*;


public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }

  public JsonObject buildMessageForRMQ(JsonObject request) {
    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    request.put(PRIMARY_KEY, primaryKey)
            .put(ORIGIN, ORIGIN_SERVER);

    LOGGER.debug("request " + request);
    return request;
  }

  public JsonObject buildReadQuery(JsonObject request) {

    ZonedDateTime startZDT, endZDT;
    String userId, startTime, endTime, method, api;
    long fromTime = 0;
    long toTime = 0;
    String databaseTableName= request.getString(DATABASE_TABLE_NAME);
    if(!request.containsKey(USER_ID)) {
      return new JsonObject().put(ERROR, USERID_NOT_FOUND);
    }
    userId = request.getString(USER_ID);

    if(request.containsKey(START_TIME)) {
      try {
        startZDT = ZonedDateTime.parse(request.getString(START_TIME));
        startTime = request.getString(START_TIME);
        LOGGER.debug("Parsed date-time: "+startZDT.toString());
      } catch (DateTimeParseException e) {
        LOGGER.error("Invalid date-time exception: "+e.getMessage());
        return new JsonObject().put(ERROR, INVALID_DATE_TIME);
      }

      if(request.containsKey(END_TIME)) {
        try {
          endZDT = ZonedDateTime.parse(request.getString(END_TIME));
          endTime = request.getString(END_TIME);
          LOGGER.debug("Parsed date-time: "+endZDT.toString());
        } catch (DateTimeParseException e) {
          LOGGER.error("Invalid date-time exception: "+e.getMessage());
          return new JsonObject().put(ERROR, INVALID_DATE_TIME);
        }
      } else {
        return new JsonObject().put(ERROR, MISSING_END_TIME);
      }

      if(startZDT.isAfter(endZDT)) {
        LOGGER.error("Invalid date-time exception: ");
        return new JsonObject().put(ERROR, INVALID_TIME);
      }

      fromTime = getEpochTime(startZDT);
      toTime = getEpochTime(endZDT);
      LOGGER.debug("Epoch fromTime: " + fromTime);
      LOGGER.debug("Epoch toTime: " + toTime);

    } else {
      if(request.containsKey(END_TIME)) {
        return new JsonObject().put(ERROR, MISSING_START_TIME);
      }
    }

    StringBuilder userIdQuery = new StringBuilder(READ_QUERY.replace("$0",databaseTableName).replace("$1", userId));
    LOGGER.debug("QUERY " + userIdQuery);

    if (request.containsKey(START_TIME) && request.containsKey(END_TIME)) {
      userIdQuery.append(START_TIME_QUERY.replace("$2", Long.toString(fromTime)));
      userIdQuery.append(END_TIME_QUERY.replace("$3", Long.toString(toTime)));
      LOGGER.debug("QUERY with start and end time" + userIdQuery);
    }
    if (request.containsKey(METHOD) && request.containsKey(ENDPOINT)) {
      method = request.getString(METHOD);
      api = request.getString(API);
      userIdQuery.append(API_QUERY.replace("$4", api));
      userIdQuery.append(METHOD_QUERY.replace("$5", method));
      LOGGER.debug("QUERY with method and endpoint " + userIdQuery);
      return new JsonObject().put(QUERY_KEY, userIdQuery);
    }
    return new JsonObject().put(QUERY_KEY, userIdQuery);
  }
}
