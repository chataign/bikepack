package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import Queries.CreateRouteQuery;
import Queries.DeleteRouteQuery;

public class WaypointListViewModel extends AndroidViewModel
{
    private AppDatabase database;
    private int routeId;
    private LiveData< List<Waypoint> > waypoints;

    WaypointListViewModel(Application application )
    {
        super(application);
        database = AppDatabase.getInstance(application);
    }

    void init( int routeId )
    {
        routeId = routeId;
        waypoints = database.waypoints().getByRouteId(routeId);
    }

    LiveData< List<Waypoint> > getWaypoints() { return waypoints; }

    void createWaypoint( NamedGlobalPosition waypointData )
    {
        database.waypoints().insert( new Waypoint( routeId, waypointData ) );
    }
}
