package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

public class RouteViewModel extends AndroidViewModel
{
    private static final String LOG_TAG = "RouteViewModel";

    private final AppDatabase database;
    private LiveData<Route> route;

    public RouteViewModel(Application application )
    {
        super(application);
        this.database = AppDatabase.getInstance(application);
    }

    public void init( int routeId )
    {
        route = database.routes().find(routeId);
    }

    public LiveData<Route> getRoute() { return route; }

    public void updateRoute( String routeName, String authorName, String authorLink )
    {
        new UpdateRouteQuery(database, route.getValue().routeId, routeName, authorName, authorLink, null ).execute();
    }

    public void deleteRoute()
    {
        new DeleteRouteQuery(database, route.getValue(), null ).execute();
    }
}
