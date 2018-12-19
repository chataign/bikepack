package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import java.util.List;

public class TrackpointListViewModel extends AndroidViewModel
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
            return (T) new TrackpointListViewModel( application, routeId );
        }
    }

    private LiveData< List<Trackpoint> > trackpoints;

    public TrackpointListViewModel(Application application, int routeId )
    {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        trackpoints = database.trackpoints().getByRouteId(routeId);
    }

    public LiveData< List<Trackpoint> > getTrackpoints() { return trackpoints; }
}
