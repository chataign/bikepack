package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

class InsertWaypointQuery extends AsyncTask< Void, Void, Void >
{
    interface Listener
    {
        void onWaypointInserted( Waypoint waypoint );
        void onInsertWaypointError(String errorMessage);
    }

    static private final String LOG_TAG = "InsertWaypointQuery";

    private final AppDatabase database;
    private final Waypoint waypoint;
    private final Listener listener;
    private Exception error = null;

    InsertWaypointQuery(@NonNull AppDatabase database,
                        @NonNull Waypoint waypoint,
                        @NonNull Listener listener )
    {
        this.database = database;
        this.waypoint = waypoint;
        this.listener = listener;
    }

    protected Void doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            if ( waypoint.name == null || waypoint.name.isEmpty() )
                throw new Exception("waypoint name cannot be empty");

            waypoint.waypointId = database.waypoints().insert( waypoint );
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
    protected void onPostExecute( Void nothing )
    {
        if ( error != null ) listener.onInsertWaypointError( error.getMessage() );
        else listener.onWaypointInserted(waypoint);
    }
}