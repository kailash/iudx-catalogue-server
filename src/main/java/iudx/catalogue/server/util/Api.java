package iudx.catalogue.server.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.apiserver.util.Constants.*;


/**
 * This class is used to get complete endpoint by appending configurable base path with the APIs
 */
public class Api {

    private String dxApiBasePath;

    private StringBuilder routeItems;
    private StringBuilder routUpdateItems;
    private StringBuilder routeDeleteItems;
    private StringBuilder routeInstance;
    private StringBuilder routeRelationship;
    private StringBuilder routeSearch;
    private StringBuilder routeNlpSearch;
    private StringBuilder routeListItems;
    private StringBuilder routeGetItems;
    private StringBuilder routeCount;
    private StringBuilder routeRelSearch;
    private StringBuilder routeGeoCoordinates;
    private StringBuilder routeGeoReverse;
    private StringBuilder routeListResourceGroupRel;
    private StringBuilder routeMlayerInstance;


    private static final Logger LOG = LogManager.getLogger(Api.class);
    private static volatile Api apiInstance;

    private Api(String dxApiBasePath) {
        this.dxApiBasePath = dxApiBasePath;
        buildEndpoints();
    }

    public static Api getInstance(String dxApiBasePath)
    {
        if (apiInstance == null)
        {
            synchronized (Api.class)
            {
                if (apiInstance == null)
                {
                    apiInstance = new Api(dxApiBasePath);
                }
            }
        }
        return apiInstance;
    }



    public void buildEndpoints() {
        routeItems = new StringBuilder(dxApiBasePath).append(ROUTE_ITEMS);
        routUpdateItems = new StringBuilder(dxApiBasePath).append(ROUTE_UPDATE_ITEMS);
        routeDeleteItems = new StringBuilder(dxApiBasePath).append(ROUTE_DELETE_ITEMS);
        routeInstance = new StringBuilder(dxApiBasePath).append(ROUTE_INSTANCE);
        routeRelationship = new StringBuilder(dxApiBasePath).append(ROUTE_RELATIONSHIP);
        routeSearch = new StringBuilder(dxApiBasePath).append(ROUTE_SEARCH);
        routeNlpSearch = new StringBuilder(dxApiBasePath).append(ROUTE_NLP_SEARCH);
        routeListItems = new StringBuilder(dxApiBasePath).append(ROUTE_LIST_ITEMS);
        routeGetItems = new StringBuilder(dxApiBasePath).append(ROUTE_GET_ITEM);
        routeCount = new StringBuilder(dxApiBasePath).append(ROUTE_COUNT);
        routeRelSearch = new StringBuilder(dxApiBasePath).append(ROUTE_REL_SEARCH);
        routeGeoCoordinates = new StringBuilder(dxApiBasePath).append(ROUTE_GEO_COORDINATES);
        routeGeoReverse = new StringBuilder(dxApiBasePath).append(ROUTE_GEO_REVERSE);
        routeListResourceGroupRel = new StringBuilder(dxApiBasePath).append(ROUTE_LIST_RESOURCE_GROUP_REL);
        routeMlayerInstance = new StringBuilder(dxApiBasePath).append(ROUTE_MLAYER_INSTANCE);



    }


    public String getRouteItems() {
        return routeItems.toString();
    }

    public String getRoutUpdateItems() {
        return routUpdateItems.toString();
    }

    public String getRouteDeleteItems() {
        return routeDeleteItems.toString();
    }

    public String getRouteInstance() {
        return routeInstance.toString();
    }

    public String getRouteRelationship() {
        return routeRelationship.toString();
    }

    public String getRouteSearch() {
        return routeSearch.toString();
    }

    public String getRouteNlpSearch() {
        return routeNlpSearch.toString();
    }

    public String getRouteListItems() {
        return routeListItems.toString();
    }

    public String getRouteGetItems() {
        return routeGetItems.toString();
    }

    public String getRouteCount() {
        return routeCount.toString();
    }

    public String getRouteRelSearch() {
        return routeRelSearch.toString();
    }

    public String getRouteGeoCoordinates() {
        return routeGeoCoordinates.toString();
    }

    public String getRouteGeoReverse() {
        return routeGeoReverse.toString();
    }
    public String getRouteListResourceGroupRel(){
        return routeListResourceGroupRel.toString();
    }
    public String getRouteMlayerInstance() { return routeMlayerInstance.toString(); }




}


