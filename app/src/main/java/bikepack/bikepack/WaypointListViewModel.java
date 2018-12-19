package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import java.util.List;

public class WaypointListViewModel extends AndroidViewModel
{
    static public class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final int routeId;


        public Factory(Application application, int routeId) {
            this.application = application;
            this.routeId = routeId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new WaypointListViewModel( application, routeId );
        }
    }

    private final AppDatabase database;
    private final int routeId;
    private LiveData< List<Waypoint> > waypoints;

    public WaypointListViewModel(Application application, int routeId )
    {
        super(application);

        this.database = AppDatabase.getInstance(application);
        this.routeId = routeId;

        waypoints = database.waypoints().getByRouteId(routeId);
    }

    public LiveData< List<Waypoint> > getWaypoints() { return waypoints; }

    public void createWaypoint( NamedGlobalPosition waypointData )
    {
        new InsertWaypointQuery( database, new Waypoint( routeId, waypointData ), null ).execute();
    }
}
