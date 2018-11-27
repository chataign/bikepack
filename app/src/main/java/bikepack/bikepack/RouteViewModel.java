package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import Queries.DeleteRouteQuery;
import Queries.UpdateRouteQuery;

public class RouteViewModel extends AndroidViewModel
{
    private static final String LOG_TAG = "RouteViewModel";

    private final AppDatabase database;
    private LiveData<Route> route;

    RouteViewModel(Application application )
    {
        super(application);
        this.database = AppDatabase.getInstance(application);
    }

    void init( int routeId )
    {
        route = database.routes().find(routeId);
    }

    LiveData<Route> getRoute() { return route; }

    void updateRoute( String routeName, String authorName, String authorLink )
    {
        new UpdateRouteQuery(database, route.getValue().routeId, routeName, authorName, authorLink, null ).execute();
    }

    void deleteRoute()
    {
        new DeleteRouteQuery(database, route.getValue(), null ).execute();
    }
}
