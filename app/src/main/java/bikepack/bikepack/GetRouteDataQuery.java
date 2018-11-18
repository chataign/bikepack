package bikepack.bikepack;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import bikepack.bikepack.AppDatabase;
import bikepack.bikepack.Trackpoint;
import bikepack.bikepack.Waypoint;

class GetRouteDataQuery extends AsyncTask< Void, Void, Void >
{
    interface Listener
    {
        void onRouteDataReceived(List<Trackpoint> trackpoints, List<Waypoint> waypoints );
        void onRouteDataError( String errorMessage );
    }

    static private final String LOG_TAG = "GetRouteDataQuery";

    private final AppDatabase database;
    private final int routeId;
    private final Listener listener;

    private Exception error=null;
    private List<Trackpoint> trackpoints=null;
    private List<Waypoint> waypoints=null;

    GetRouteDataQuery(AppDatabase database, int routeId, Listener listener )
    {
        this.database = database;
        this.routeId = routeId;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground( Void... nothing )
    {
        long startTime = System.currentTimeMillis();

        try
        {
            trackpoints = database.trackpoints().getByRouteId(routeId);
            waypoints = database.waypoints().getByRouteId(routeId);
        }
        catch ( Exception error )
        {
            Log.e( LOG_TAG, error.getMessage() );
            this.error = error;
        }

        long endTime = System.currentTimeMillis();
        Log.i( LOG_TAG, String.format( "executed in %dms", endTime-startTime ) );

        return null;
    }

    @Override
    protected void onPostExecute( Void v )
    {
        if ( error != null ) listener.onRouteDataError( error.getMessage() );
        else listener.onRouteDataReceived( trackpoints, waypoints );
    }
}