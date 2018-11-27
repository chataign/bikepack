package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import Queries.CreateRouteQuery;
import Queries.DeleteRouteQuery;

public class RouteListViewModel extends AndroidViewModel
{
    private AppDatabase database;
    private LiveData<List<Route>> routes;

    RouteListViewModel(Application application )
    {
        super(application);
        database = AppDatabase.getInstance(application);
        routes = database.routes().getAll();
    }

    LiveData< List<Route> > getRoutes() { return routes; }

    void createRoute( final Metadata metadata,
                      final List<GlobalPosition> trackpoints,
                      final List<NamedGlobalPosition> waypoints ) {
        new CreateRouteQuery( database, metadata, trackpoints, waypoints, null ).execute();
    }

    void deleteRoute( Route route )
    {
        new DeleteRouteQuery( database, route, null ).execute();
    }
}
