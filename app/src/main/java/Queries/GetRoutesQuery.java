package Queries;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import bikepack.bikepack.AppDatabase;
import bikepack.bikepack.Route;

public class GetRoutesQuery extends AsyncTask< Void, Void, List<Route> >
{
    public interface Listener
    {
        void onRoutesReceived( List<Route> routes );
        void onGetRoutesError( String errorMessage );
    }

    static private final String LOG_TAG = "GetRoutesQuery";

    private final AppDatabase database;
    private final Listener listener;
    private Exception error = null;

    public GetRoutesQuery(AppDatabase database, Listener listener )
    {
        this.database = database;
        this.listener = listener;
    }

    protected List<Route> doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            return database.routes().getAll().getValue(); // TODO
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
    protected void onPostExecute( List<Route> routes )
    {
        if ( error != null ) listener.onGetRoutesError( error.getMessage() );
        else listener.onRoutesReceived(routes);
    }
}