package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import bikepack.bikepack.AppDatabase;
import bikepack.bikepack.Route;

class UpdateRouteQuery extends AsyncTask< Void, Void, Route>
{
    interface Listener
    {
        void onRouteUpdated( Route route );
        void onUpdateRouteError(String errorMessage);
    }

    static private final String LOG_TAG = "UpdateRouteQuery";

    private final AppDatabase database;
    private final int routeId;
    private final String routeName;
    private final String authorName;
    private final String authorLink;
    private final Listener listener;
    private Exception error = null;

    UpdateRouteQuery(@NonNull AppDatabase database,
                     int routeId, String routeName, String authorName, String authorLink,
                     @NonNull Listener listener )
    {
        this.database = database;
        this.routeId = routeId;
        this.routeName = routeName;
        this.authorName = authorName;
        this.authorLink = authorLink;
        this.listener = listener;
    }

    protected Route doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            database.routes().update( routeId, routeName, authorName, authorLink );
            return database.routes().find(routeId);
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
        if ( error != null ) listener.onUpdateRouteError( error.getMessage() );
        else if ( route != null ) listener.onRouteUpdated(route);
    }
}