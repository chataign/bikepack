package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

public class DeleteRouteQuery extends AsyncTask< Void, Void, Void >
{
    public interface Listener
    {
        void onRouteDeleted();
        void onDeleteRouteError(String errorMessage);
    }

    static private final String LOG_TAG = "DeleteRouteQuery";

    private final AppDatabase database;
    private final Route route;
    private final Listener listener;
    private Exception error = null;

    public DeleteRouteQuery(@NonNull AppDatabase database,
                     @NonNull Route route,
                     @NonNull Listener listener )
    {
        this.database = database;
        this.route = route;
        this.listener = listener;
    }

    protected Void doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            database.routes().delete(route);
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
        if ( error != null ) listener.onDeleteRouteError( error.getMessage() );
        else if ( listener != null )listener.onRouteDeleted();
    }
}