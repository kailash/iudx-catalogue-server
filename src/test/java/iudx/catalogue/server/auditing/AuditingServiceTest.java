package iudx.catalogue.server.auditing;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class})
public class AuditingServiceTest {


  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceTest.class);
  private static AuditingService auditingService;
  private static Vertx vertxObj;
  private static JsonObject dbConfig;
  private static String databaseIP;
  private static Integer databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static Integer databasePoolSize;

  private static String databaseTableName;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    dbConfig = Configuration.getConfiguration("./configs/config-test.json",4);
    databaseIP = dbConfig.getString("auditingDatabaseIP");
    databasePort = dbConfig.getInteger("auditingDatabasePort");
    databaseName = dbConfig.getString("auditingDatabaseName");
    databaseUserName = dbConfig.getString("auditingDatabaseUserName");
    databasePassword = dbConfig.getString("auditingDatabasePassword");
    databaseTableName = dbConfig.getString("auditingDatabaseTableName");
    databasePoolSize = dbConfig.getInteger("auditingPoolSize");
    auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    vertxTestContext.completeNow();
  }

  private JsonObject writeRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
      .put(USER_ROLE,"provider")
      .put(USER_ID,"test2.data@iudx.org")
      .put(IID,"/iid")
      .put(API,"/iudx/cat/v1/item")
      .put(METHOD,"POST")
      .put(IUDX_ID,"/iudxid");
    return jsonObject;
  }

  private JsonObject readRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
      .put(USER_ID,"test2.data@iudx.org")
      .put(METHOD,"POST")
      .put(API,"/iudx/cat/v1/item")
      .put(START_TIME,"1970-01-01T05:30:00+05:30[Asia/Kolkata]")
      .put(END_TIME,"2021-09-20T20:00:00+05:30[Asia/Kolkata]");
    return jsonObject;
  }

    @Test
    @DisplayName("Testing insertMeteringValuesInRMQ success")
    void writeDataSuccessful(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();

        request.put(USER_ROLE, "userRole");
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        request.put(IID, "dummy IID");
        request.put(IUDX_ID, "dummy IID");
        request.put(API, "dummy api");
        request.put(METHOD, "dummy metod");
        request.put(EPOCH_TIME, 0.00);
        request.put(PRIMARY_KEY, "dummy primary key");
        request.put(ORIGIN, ORIGIN_SERVER);
        AuditingServiceImpl auditingService = new AuditingServiceImpl(dbConfig, vertxObj);

        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        AuditingServiceImpl.rmqService = mock(DataBrokerService.class);

        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(
                new Answer<AsyncResult<JsonObject>>() {
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                        return null;
                    }
                })
                .when(auditingService.rmqService)
                .publishMessage(any(), anyString(), anyString(), any());

        auditingService.insertAuditngValuesInRMQ(
                request,
                handler -> {
                    if (handler.succeeded()) {
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow("Failed");
                    }
                });
        verify(auditingService.rmqService, times(1))
                .publishMessage(any(), anyString(), anyString(), any());
    }

  @Test
  @DisplayName("Failure-testing Read query for missing userId")
  void readForMissingUserId(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(USER_ID);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                USERID_NOT_FOUND,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for missing start time")
  void readForMissingStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(START_TIME);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                MISSING_START_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for missing start time")
  void readForMissingEndTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(END_TIME);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                MISSING_END_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query when end time is before start time")
  void readForEndTimeBeforeStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    String temp = request.getString(START_TIME);
    request
      .put(START_TIME, request.getString(END_TIME))
      .put(END_TIME, temp);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                INVALID_TIME, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for Invalid date time format")
  void readForInvalidDateTimeFormat(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(START_TIME, "1970-01-0105:30:00+05:30[Asia/Kolkata]");

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                INVALID_DATE_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for empty response")
  void readforEmptyResponse(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(END_TIME,"1970-01-01T05:30:00+05:30[Asia/Kolkata]");

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.debug(response);
              assertEquals(EMPTY_RESPONSE, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing Read Query")
  void readData(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    auditingService.executeReadQuery(
      request,
      vertxTestContext.succeeding(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.debug(response);
              assertEquals(SUCCESS, response.getString(TITLE));
              vertxTestContext.completeNow();
            })));
  }
}
