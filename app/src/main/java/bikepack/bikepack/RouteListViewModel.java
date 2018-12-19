package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import java.util.List;

public class RouteListViewModel extends AndroidViewModel
{
    private static final String LOG_TAG = "RouteListViewModel";

    private AppDatabase database;
    private LiveData<List<Route>> routes;

    RouteListViewModel(Application application )
    {
        super(application);
        database = AppDatabase.getInstance(application);
        routes = database.routes().getAll();
    }

    public LiveData< List<Route> > getRoutes() { return routes; }

    public void createRoute( final Metadata metadata,
                      final List<GlobalPosition> trackpoints,
                      final List<NamedGlobalPosition> waypoints ) {
        new CreateRouteQuery( database, metadata, trackpoints, waypoints, null ).execute();
    }

    public void deleteRoute( Route route )
    {
        Log.i( LOG_TAG, "deleting route=" + route.routeName );
        new DeleteRouteQuery( database, route, null ).execute();
    }
}
