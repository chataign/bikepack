package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

class CreateRouteQuery extends AsyncTask< Void, Void, Route >
{
    interface Listener
    {
        void onRouteCreated( Route route );
        void onCreateRouteError(String errorMessage);
    }

    static private final String LOG_TAG = "CreateRouteQuery";

    private final AppDatabase database;
    private final Metadata metadata;
    private final List<GlobalPosition> positions;
    private final Listener listener;
    private Exception error = null;

    CreateRouteQuery(@NonNull AppDatabase database,
                     @NonNull Metadata metadata,
                     @NonNull List<GlobalPosition> positions,
                     @NonNull Listener listener )
    {
        this.database = database;
        this.metadata = metadata;
        this.positions = positions;
        this.listener = listener;
    }

    protected Route doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            Route existingRoute = database.routes().find( metadata.routeName, metadata.authorName );
            if ( existingRoute != null ) throw new Exception("Route already exists");

            //List<XmlUtils.XmlObject> xml_waypoints = XmlUtils.readAll( content_resolver, route_uri, Waypoint.GPX_TAG);

            float totalDistance=0, totalAscent=0, totalDescent=0, highestElevation=0, lowestElevation=1e6f;

            for ( int i=1; i< positions.size(); ++i )
            {
                GlobalPosition prev = positions.get(i-1);
                GlobalPosition curr = positions.get(i);

                lowestElevation = Math.min( lowestElevation, curr.elevation );
                highestElevation = Math.max( highestElevation, curr.elevation );

                double elevationDiff = curr.elevation - prev.elevation;
                if ( elevationDiff > 0 ) totalAscent += elevationDiff;
                else totalDescent += Math.abs(elevationDiff);

                totalDistance += GlobalPosition.distance( prev, curr );
            }

            Date dateAdded = Calendar.getInstance().getTime();

            Route route = new Route(
                    metadata.routeName,
                    metadata.authorName,
                    metadata.authorLink,
                    metadata.dateCreated,
                    dateAdded,
                    totalDistance,
                    totalAscent,
                    totalDescent,
                    highestElevation,
                    lowestElevation,
                    positions.size(),
                    0 );

            if ( route == null ) throw new Exception("Failed to create route");
            route.routeId = (int) database.routes().insert(route);

            Trackpoint trackpoints[] = new Trackpoint[positions.size()];

            for (int i = 0; i < trackpoints.length; ++i)
                trackpoints[i] = new Trackpoint( positions.get(i), route.routeId );

            Log.i("CreateRouteQuery", String.format("inserting %d trackpoints...", positions.size()));
            database.trackpoints().insert(trackpoints);

                /*
                Waypoint waypoints[] = new Waypoint[xml_waypoints.size()];

                for (int i = 0; i < xml_waypoints.size(); ++i)
                    waypoints[i] = Waypoint.build(xml_waypoints.get(i), route.id);

                Log.i("CreateRouteQuery", String.format("inserting %d waypoints...", xml_waypoints.size()));
                database.waypoints().insert(waypoints);
                 */

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
        if ( error != null ) listener.onCreateRouteError( error.getMessage() );
        else if ( route != null ) listener.onRouteCreated(route);
    }
}