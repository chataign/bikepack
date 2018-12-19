package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

public class CreateRouteQuery extends AsyncTask< Void, Void, Route>
{
    public interface Listener
    {
        void onRouteCreated( Route route );
        void onCreateRouteError(String errorMessage);
    }

    static private final String LOG_TAG = "CreateRouteQuery";

    private final AppDatabase database;
    private final Metadata metadata;
    private final List<GlobalPosition> trackpoints;
    private final List<NamedGlobalPosition> waypoints;
    private final Listener listener;
    private Exception error = null;

    public CreateRouteQuery(@NonNull AppDatabase database,
                     @NonNull Metadata metadata,
                     @NonNull List<GlobalPosition> trackpoints,
                     List<NamedGlobalPosition> waypoints,
                     Listener listener )
    {
        this.database = database;
        this.metadata = metadata;
        this.trackpoints = trackpoints;
        this.waypoints = waypoints;
        this.listener = listener;
    }

    protected Route doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            Route existingRoute = database.routes().find( metadata.routeName, metadata.authorName );
            if ( existingRoute != null ) throw new Exception("Route already exists");
            if ( trackpoints == null ) throw new Exception("null trackpoints");

            Route route = Route.buildFromData(metadata,trackpoints,waypoints);
            route.routeId = (int) database.routes().insert(route);

            Trackpoint dbTrackpoints[] = new Trackpoint[trackpoints.size()];
            for (int i = 0; i < dbTrackpoints.length; ++i)
                dbTrackpoints[i] = new Trackpoint( trackpoints.get(i), route.routeId );

            Log.i("CreateRouteQuery", String.format("inserting %d trackpoints...", trackpoints.size()));
            database.trackpoints().insert(dbTrackpoints);

            if ( waypoints != null )
            {
                Waypoint dbWaypoints[] = new Waypoint[waypoints.size()];
                for (int i = 0; i < dbWaypoints.length; ++i)
                    dbWaypoints[i] = new Waypoint( route.routeId, waypoints.get(i) );

                Log.i("CreateRouteQuery", String.format("inserting %d waypoints...", waypoints.size()));
                database.waypoints().insert(dbWaypoints);
            }

            final long endTime = System.currentTimeMillis();
            Log.i( LOG_TAG, String.format( "CreateRouteTask: executed in %dms", endTime-startTime ) );

            return route;
        }
        catch( Exception error )
        {
            Log.w( LOG_TAG, error.getMessage() );
            this.error = error;
        }

        long endTime = System.currentTimeMillis();
        Log.i( LOG_TAG, String.format( "executed in %dms", endTime-startTime ) );

        return null;
    }

    @Override
    protected void onPostExecute( Route route )
    {
        if ( listener != null )
        {
            if ( error != null ) listener.onCreateRouteError( error.getMessage() );
            else if ( route != null ) listener.onRouteCreated(route);
        }
    }
}