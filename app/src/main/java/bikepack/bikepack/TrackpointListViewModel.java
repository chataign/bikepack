package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class TrackpointListViewModel extends AndroidViewModel
{
    private AppDatabase database;
    private int routeId;
    private LiveData< List<Trackpoint> > trackpoints;

    TrackpointListViewModel(Application application )
    {
        super(application);
        database = AppDatabase.getInstance(application);
    }

    void init( int routeId )
    {
        routeId = routeId;
        trackpoints = database.trackpoints().getByRouteId(routeId);
    }

    LiveData< List<Trackpoint> > getTrackpoints() { return trackpoints; }
}
