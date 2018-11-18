package bikepack.bikepack;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AppRepository
{
    private static final String LOG_TAG = "AppRepository";

    interface CreateRouteListener
    {
        void onRouteCreated( Route route );
        void onError( String errorMessage );
    }

    interface RouteDataListener
    {
        void onRouteDataReceived(List<Trackpoint> trackpoints, List<Waypoint> waypoints );
    }

    private AppDatabase database;

    AppRepository( AppDatabase database ) { this.database = database; }

    List<Route> getRoutes()
    {
        Callable< List<Route> > callable = new Callable< List<Route> >()
        {
            @Override
            public List<Route> call()
            { return database.routes().getAll(); }
        };

        Future< List<Route> > future = Executors.newSingleThreadExecutor().submit(callable);

        try
        {
            return future.get();
        }
        catch( Exception e )
        {
            Log.e( "getRoutes", e.getMessage() );
            return new ArrayList<>();
        }
    }

    Route updateRoute(final long routeId, final String routeName, final String authorName, final String authorLink )
    {
        Log.i( "updateRoute", routeName );
        Callable<Route> callable = new Callable<Route>()
        {
            @Override
            public Route call() {
                database.routes().update( routeId, routeName, authorName, authorLink );
                return database.routes().find((int)routeId); }
        };

        try { return Executors.newSingleThreadExecutor().submit(callable).get(); }
        catch( Exception e ) { Log.e( "updateRoute", e.getMessage() ); return null; }
    }

    static private class CreateRouteTask extends AsyncTask< Void, Void, Route >
    {
        private final AppDatabase database;
        private final CreateRouteListener listener;
        private final Metadata metadata;
        private final List<GlobalPosition> positions;
        private String errorMessage =null;

        CreateRouteTask( AppDatabase database, Metadata metadata, List<GlobalPosition> positions, CreateRouteListener listener )
        {
            this.database = database;
            this.metadata = metadata;
            this.positions = positions;
            this.listener = listener;
        }

        protected Route doInBackground( Void... nothing )
        {
            final long startTime = System.currentTimeMillis();

            try
            {
                Route existing_route = database.routes().find( metadata.routeName, metadata.authorName );
                if ( existing_route != null ) throw new Exception("Route already exists");

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

                Date date_added = Calendar.getInstance().getTime();

                Route route = new Route(
                        metadata.routeName,
                        metadata.authorName,
                        metadata.authorLink,
                        metadata.dateCreated,
                        date_added,
                        totalDistance,
                        totalAscent,
                        totalDescent,
                        highestElevation,
                        lowestElevation,
                        positions.size(),
                        0 );

                route.routeId = (int) database.routes().insert(route);

                Trackpoint trackpoints[] = new Trackpoint[positions.size()];

                for (int i = 0; i < trackpoints.length; ++i)
                    trackpoints[i] = new Trackpoint( positions.get(i), route.routeId );

                Log.i("CreateRoute", String.format("inserting %d trackpoints...", positions.size()));
                database.trackpoints().insert(trackpoints);

                /*
                Waypoint waypoints[] = new Waypoint[xml_waypoints.size()];

                for (int i = 0; i < xml_waypoints.size(); ++i)
                    waypoints[i] = Waypoint.build(xml_waypoints.get(i), route.id);

                Log.i("CreateRoute", String.format("inserting %d waypoints...", xml_waypoints.size()));
                database.waypoints().insert(waypoints);
                 */

                final long endTime = System.currentTimeMillis();
                Log.i( LOG_TAG, String.format( "CreateRouteTask: executed in %dms", endTime-startTime ) );

                return route;
            }
            catch( Exception e )
            {
                Log.e( LOG_TAG, e.getMessage() );
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute( Route route )
        {
            if ( listener == null ) return;
            if ( errorMessage != null ) listener.onError(errorMessage);
            else if ( route != null ) listener.onRouteCreated(route);
        }
    }

    void createRoute( Metadata metadata, List<GlobalPosition> trackpoints, CreateRouteListener listener )
    {
        CreateRouteTask task = new CreateRouteTask( database, metadata, trackpoints, listener );
        task.execute();
    }

    void deleteRoute(final Route route)
    {
        Log.i( "deleteRoute", route.toString() );

        Callable<Void> callable = new Callable<Void>()
        {
            @Override
            public Void call()
            {
                database.routes().delete(route);
                return null;
            }
        };

        Future<Void> future = Executors.newSingleThreadExecutor().submit(callable);

        try
        {
            future.get();
        }
        catch( Exception e )
        {
            Log.e( "deleteRoute", e.getMessage() );
        }
    }

    static private class GetRouteDataTask extends AsyncTask< Void, Void, Void >
    {
        private final AppDatabase database;
        private final int routeId;
        private final RouteDataListener listener;

        private List<Trackpoint> trackpoints=null;
        private List<Waypoint> waypoints=null;

        GetRouteDataTask( AppDatabase database, int routeId, RouteDataListener listener )
        {
            this.database = database;
            this.routeId = routeId;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground( Void... nothing )
        {
            long startTime = System.currentTimeMillis();

            trackpoints = database.trackpoints().getByRouteId(routeId);
            waypoints = database.waypoints().getByRouteId(routeId);

            long endTime = System.currentTimeMillis();
            Log.i( LOG_TAG, String.format( "GetRouteDataTask: executed in %dms", endTime-startTime ) );

            return null;
        }

        @Override
        protected void onPostExecute( Void v )
        {
            listener.onRouteDataReceived( trackpoints, waypoints );
        }
    }

    void getRouteData( int routeId, @NonNull RouteDataListener listener )
    {
        GetRouteDataTask task = new GetRouteDataTask( database, routeId, listener );
        task.execute();
    }
}
