package iudx.catalogue.server.auditing;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.catalogue.server.auditing.util.QueryBuilder;
import iudx.catalogue.server.auditing.util.ResponseBuilder;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.ROUTING_KEY;
import static iudx.catalogue.server.util.Constants.BROKER_SERVICE_ADDRESS;

public class AuditingServiceImpl implements AuditingService {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  private final String METHOD_COLUMN_NAME;
  private final String TIME_COLUMN_NAME;
  private final String USERID_COLUMN_NAME;
  private final String BODY_COLUMN_NAME;
  private final String ENDPOINT_COLUMN_NAME;
  private final String API_COLUMN_NAME;
  private final String IID_COLUMN_NAME;
  private final String IUDX_COLUMN_NAME;
  private final String USERROLE_COLUMN_NAME;
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private JsonObject query = new JsonObject();
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;
  private ResponseBuilder responseBuilder;
  public static DataBrokerService rmqService;

  public AuditingServiceImpl(JsonObject propObj, Vertx vertxInstance) {
    if (propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString("auditingDatabaseIP");
      databasePort = propObj.getInteger("auditingDatabasePort");
      databaseName = propObj.getString("auditingDatabaseName");
      databaseUserName = propObj.getString("auditingDatabaseUserName");
      databasePassword = propObj.getString("auditingDatabasePassword");
      databaseTableName = propObj.getString("auditingDatabaseTableName");
      databasePoolSize = propObj.getInteger("auditingPoolSize");
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIP)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.rmqService = DataBrokerService.createProxy(vertxInstance,BROKER_SERVICE_ADDRESS);

    METHOD_COLUMN_NAME =
        _METHOD_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    TIME_COLUMN_NAME =
        _TIME_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    USERID_COLUMN_NAME =
        _USERID_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    BODY_COLUMN_NAME =
        _BODY_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();

    ENDPOINT_COLUMN_NAME =
        _ENDPOINT_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();

    API_COLUMN_NAME = _API_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
        .toString();
    IID_COLUMN_NAME = _IID_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
        .toString();
    IUDX_COLUMN_NAME =
        _IUDX_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    USERROLE_COLUMN_NAME =
        _USERROLE_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
  }

  @Override
  public AuditingService insertAuditngValuesInRMQ(
          JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    request.put(DATABASE_TABLE_NAME, databaseTableName);
    JsonObject rmqMessage = new JsonObject();

    rmqMessage = queryBuilder.buildMessageForRMQ(request);

    LOGGER.debug("audit rmq Message body: " + rmqMessage);
    rmqService.publishMessage(rmqMessage, EXCHANGE_NAME, ROUTING_KEY,
            rmqHandler -> {
              if (rmqHandler.succeeded()) {
                handler.handle(Future.succeededFuture());
                LOGGER.info("inserted into rmq");
              } else {
                LOGGER.debug("failed to insert into rmq");
                LOGGER.error(rmqHandler.cause());
                handler.handle(Future.failedFuture(rmqHandler.cause().getMessage()));
              }
            });
    return this;
  }

  @Override
  public AuditingService executeReadQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: Read Query" + request.toString());

    if (!request.containsKey(USER_ID)) {
      LOGGER.debug("Info: " + USERID_NOT_FOUND);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(USERID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    request.put(DATABASE_TABLE_NAME, databaseTableName);
    query = queryBuilder.buildReadQuery(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }
    LOGGER.debug("Info: Query constructed: " + query.getString(QUERY_KEY));

    Future<JsonObject> result = executeReadQuery(query);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            if (resultHandler.result().getString(TITLE).equals(FAILED)) {
              LOGGER.error("Read from DB failed:" + resultHandler.result());
              handler.handle(Future.failedFuture(resultHandler.result().toString()));
            } else {
              LOGGER.debug("Read from DB succeeded.");
              handler.handle(Future.succeededFuture(resultHandler.result()));
            }
          } else if (resultHandler.failed()) {
            LOGGER.error("Read from DB failed:" + resultHandler.cause());
            handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
          }
        });
    return this;
  }

  private Future<JsonObject> executeReadQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray jsonArray = new JsonArray();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(
            rows -> {
              RowSet<Row> result = rows.result();
              if (result == null) {
                responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
              } else {
                for (Row rs : result) {
                  jsonArray.add(getJsonObject(rs));
                }
                if (jsonArray.isEmpty()) {
                  responseBuilder =
                      new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
                } else {
                  responseBuilder =
                      new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setJsonArray(jsonArray);
                  LOGGER.debug("RESPONSE" + responseBuilder.getResponse().getString(RESULTS));
                }
              }
              promise.complete(responseBuilder.getResponse());
            });
    return promise.future();
  }

  private Object getJsonObject(Row rs) {
    JsonObject entries = new JsonObject();
    LOGGER.debug("API: " + rs.getString(API_COLUMN_NAME));
    LOGGER.debug("METHOD: " + rs.getString(METHOD_COLUMN_NAME));
    LOGGER.debug("USERID: " + rs.getString(USERID_COLUMN_NAME));
    LOGGER.debug("USERROLE: " + rs.getString(USERROLE_COLUMN_NAME));
    LOGGER.debug("IID: " + rs.getString(IID_COLUMN_NAME));
    LOGGER.debug("IUDX_ID: " + rs.getString(IUDX_COLUMN_NAME));
    LOGGER.debug("TIME: " + rs.getLong(TIME_COLUMN_NAME));

    entries
        .put(API, rs.getString(API_COLUMN_NAME))
        .put(METHOD, rs.getString(METHOD_COLUMN_NAME))
        .put(USER_ID, rs.getString(USERID_COLUMN_NAME))
        .put(USER_ROLE, rs.getString(USERROLE_COLUMN_NAME))
        .put(IID, rs.getString(IID_COLUMN_NAME))
        .put(IUDX_ID, rs.getString(IUDX_COLUMN_NAME))
        .put(TIME, rs.getLong(TIME_COLUMN_NAME));

    return entries;
  }
}
