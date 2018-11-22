package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.RouteEditBinding;
import bikepack.bikepack.databinding.RouteMapActivityBinding;
import bikepack.bikepack.databinding.WaypointEditBinding;

public class RouteMapActivity extends AppCompatActivity
    implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        ElevationView.Listener,
        GetRouteDataQuery.Listener
{
    private static final String LOG_TAG = "MapActivity";
    private static final String MAP_VIEW_BUNDLE_KEY = "MAP_VIEW_BUNDLE_KEY";
    private static final float ROUTE_MAP_DEPTH = 1.0f;
    private static final float MARKERS_MAP_DEPTH = 2.0f;

    private GoogleMap map=null;
    private Route route=null;
    private Marker mapTouchedMarker =null;
    private Marker waypointCreationMarker=null;
    private Polyline selectionPolyline =null;
    private List<Marker> waypointMarkers=new ArrayList<>();

    private List<Trackpoint> trackpoints=null;
    private List<Waypoint> waypoints=null;
    private SharedPreferences preferences;

    private AppDatabase database;
    private RouteMapActivityBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i( LOG_TAG, "onCreate" );

        super.onCreate(savedInstanceState);

        this.database = Room.databaseBuilder(
                this, AppDatabase.class, AppDatabase.DB_NAME).build();

        preferences = getPreferences(Context.MODE_PRIVATE);
        ui = DataBindingUtil.setContentView(this, R.layout.route_map_activity);

        Bundle map_view_bundle = null;

        if (savedInstanceState != null)
            map_view_bundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);

        ui.layout.setEnabled(false);
        ui.progressBar.setVisibility(View.VISIBLE);

        ui.mapView.onCreate(map_view_bundle);
        ui.mapView.getMapAsync(this);
    }

    static private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable)
    {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(GoogleMap map)
    {
        Log.i( LOG_TAG, "onMapReady" );

        map.setOnMapLoadedCallback(this);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType( preferences.getInt(getString(R.string.preferences_map_type), GoogleMap.MAP_TYPE_HYBRID ) );

        Drawable touchMarkerDrawable = getResources().getDrawable(R.drawable.map_marker);
        BitmapDescriptor touchMarkerIcon = getMarkerIconFromDrawable(touchMarkerDrawable);

        mapTouchedMarker = map.addMarker( new MarkerOptions()
                .position( new LatLng(0,0) )
                .visible(false)
                .icon(touchMarkerIcon)
                .anchor(0.5f,0.5f)
                .draggable(false)
                .zIndex(MARKERS_MAP_DEPTH));

        waypointCreationMarker = map.addMarker( new MarkerOptions()
                .position( new LatLng(0,0) )
                .visible(false)
                .icon( BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) )
                .anchor(0.5f,0.5f)
                .draggable(false)
                .zIndex(MARKERS_MAP_DEPTH));

        selectionPolyline = map.addPolyline( new PolylineOptions()
                .visible(true)
                .color(Color.BLUE)
                .zIndex(MARKERS_MAP_DEPTH));

        this.map = map;

        Intent intent = getIntent();
        route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) this.finish();

        map.moveCamera( CameraUpdateFactory.newLatLngBounds(route.bounds,10) );

        GetRouteDataQuery task = new GetRouteDataQuery( database, route.routeId, this );
        task.execute();
    }

    @Override
    public void onMapLoaded()
    {
        Log.i( LOG_TAG, "onMapLoaded" );
    }

    @Override
    public void onRouteDataReceived( List<Trackpoint> trackpoints, List<Waypoint> waypoints )
    {
        this.trackpoints = trackpoints;
        this.waypoints = waypoints;

        PolylineOptions polyline = new PolylineOptions()
                .color(Color.RED)
                .zIndex(ROUTE_MAP_DEPTH);

        for ( Trackpoint trackpoint : trackpoints )
            polyline.add(trackpoint.latlng);

        map.addPolyline(polyline);

        for ( Marker marker : waypointMarkers )
            marker.remove();

        waypointMarkers.clear();

        for ( Waypoint waypoint : waypoints)
        {
            Marker waypointMarker = map.addMarker( new MarkerOptions()
                .position(waypoint.latlng)
                .title(waypoint.name));

            waypointMarkers.add(waypointMarker);
        }

        ui.elevationView.setTrackpoints(trackpoints,this);

        ui.layout.setEnabled(true);
        ui.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onRouteDataError( String errorMessage )
    {
        Log.e( LOG_TAG, errorMessage );
    }

    @Override
    public void onValueTouched( Trackpoint trackpoint )
    {
        mapTouchedMarker.setVisible(true);
        mapTouchedMarker.setPosition( trackpoint.latlng );
    }

    @Override
    public void onValueReleased()
    {
        mapTouchedMarker.setVisible(false);
    }

    @Override
    public void onSelectionUpdated( List<Trackpoint> selection, List<LatLng> points )
    {
        if ( selectionPolyline != null )
        {
            selectionPolyline.setPoints( points );
            selectionPolyline.setVisible(true);
        }
    }

    @Override
    public void onSelectionEnd(final List<Trackpoint> selection, List<LatLng> points )
    {
        final List<Trackpoint> originalTrackpoints = trackpoints;
        trackpoints = selection;

        ui.elevationView.setTrackpoints(trackpoints,RouteMapActivity.this);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for ( LatLng point : points) boundsBuilder.include(point);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds( boundsBuilder.build(), 10));

        startSupportActionMode(new ActionMode.Callback() {
            int originalStatusBarColor;
            final int actionModeStatusBarColor = getResources().getColor(R.color.black);

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.route_selection_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu)
            {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId())
                {
                    case R.id.action_save:
                        saveRouteSelection(selection);
                        mode.finish();
                        break;
                }
                return true ;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getWindow().setStatusBarColor(originalStatusBarColor);

                selectionPolyline.remove();
                trackpoints = originalTrackpoints;
                ui.elevationView.setTrackpoints(originalTrackpoints,RouteMapActivity.this);
            }
        }).setTitle("Selection");
    }

    private void saveRouteSelection( final List<Trackpoint> selection )
    {
        View routeEditorView = getLayoutInflater().inflate(R.layout.route_edit, null );
        final RouteEditBinding routeEditor = DataBindingUtil.bind(routeEditorView);

        routeEditor.name.setText(route.routeName);
        routeEditor.author.setText(route.authorName);
        routeEditor.authorLink.setText(route.authorLink);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RouteMapActivity.this)
            .setTitle(R.string.route_edit_dialog_title)
            .setView(routeEditorView)
            .setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                Metadata metadata = new Metadata(
                        routeEditor.name.getText().toString(),
                        routeEditor.author.getText().toString(),
                        routeEditor.authorLink.getText().toString(),
                        route.dateCreated);

                List<GlobalPosition> trackpointsData= new ArrayList<>();
                LatLngBounds.Builder selectionBoundsBuilder = new LatLngBounds.Builder();

                for ( Trackpoint trackpoint : selection )
                {
                    selectionBoundsBuilder.include( trackpoint.latlng );

                    trackpointsData.add(
                            new GlobalPosition( trackpoint.latitude,
                                    trackpoint.longitude,
                                    trackpoint.elevation ) );
                }

                LatLngBounds selectionBounds = selectionBoundsBuilder.build();
                List<NamedGlobalPosition> waypointsData = new ArrayList<>();

                for ( Waypoint waypoint : waypoints )
                {
                    if ( selectionBounds.contains(waypoint.latlng) )
                        waypointsData.add( waypoint.namedPosition );
                }

                CreateRouteQuery createRoute = new CreateRouteQuery(
                        database, metadata, trackpointsData, waypointsData , new CreateRouteQuery.Listener() {

                    public void onRouteCreated(Route route) {
                        Toast toast = Toast.makeText( RouteMapActivity.this,
                                R.string.route_created, Toast.LENGTH_SHORT );
                        toast.show();
                    }
                    public void onCreateRouteError(String error_message) {
                        Toast toast = Toast.makeText( RouteMapActivity.this,
                                R.string.route_creation_failed, Toast.LENGTH_SHORT );
                        toast.show();
                    }
                });

                createRoute.execute();
            }
        })
            .setNegativeButton( R.string.dialog_cancel, null );

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    public void onSaveInstanceState(Bundle state)
    {
        super.onSaveInstanceState(state);

        Bundle mapViewBundle = state.getBundle(MAP_VIEW_BUNDLE_KEY);

        if (mapViewBundle == null)
        {
            mapViewBundle = new Bundle();
            state.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        ui.mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle state)
    {
        Log.i( LOG_TAG, "onRestoreInstanceState" );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_show_elevation:
                switch ( ui.elevationView.getVisibility() )
                {
                    case View.VISIBLE:
                        ui.elevationView.setVisibility( View.GONE );
                        break;
                    case View.INVISIBLE:
                    case View.GONE:
                        ui.elevationView.setVisibility( View.VISIBLE );
                        break;
                }
                return true;

            case R.id.action_select_layer:
                selectMapLayer();
                return true;

            case R.id.action_add_waypoint:
                createWaypoint();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        ui.mapView.onResume();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        ui.mapView.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        ui.mapView.onStop();
    }

    private void selectMapLayer()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.map_layer_dialog_title)
                .setItems(R.array.map_type_names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int[] map_types = getResources().getIntArray(R.array.map_types);
                        map.setMapType( map_types[which] );
                        preferences.edit()
                                .putInt( getString(R.string.preferences_map_type), map.getMapType() )
                                .apply();
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void saveWaypoint( final long routeId, final LatLng position )
    {
        View waypointEditView = getLayoutInflater().inflate(R.layout.waypoint_edit, null );
        final WaypointEditBinding waypointEditor = DataBindingUtil.bind(waypointEditView);

        waypointEditor.name.setText("");
        waypointEditor.description.setText("");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.waypoint_create_dialog_title);
        builder.setView(waypointEditView);
        builder.setNegativeButton( R.string.dialog_cancel, null );
        builder.setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                String name  = waypointEditor.name.getText().toString();
                String description = waypointEditor.description.getText().toString();

                if ( name.isEmpty() )
                {
                    waypointEditor.name.setError("name cannot be empty!");
                    return;
                }

                final Waypoint waypoint = new Waypoint( routeId,
                        new GlobalPosition( position.latitude, position.longitude, 0 ),
                        name, description );

                new AsyncTask<Void,Void,Void>() {
                    protected Void doInBackground(Void... nothing)
                    {
                        waypoint.waypointId = database.waypoints().insert(waypoint);
                        return null;
                    }

                    protected void onPostExecute( Void nothing )
                    {
                        addWaypoint(waypoint);
                        Toast toast = Toast.makeText( RouteMapActivity.this, "waypoint created", Toast.LENGTH_LONG );
                        toast.show();
                    }
                }.execute();
            }
        });

        builder.setNegativeButton( R.string.dialog_cancel, null );
        AlertDialog edit_dialog = builder.create();
        edit_dialog.show();
    }

    private void addWaypoint( Waypoint waypoint )
    {
        waypoints.add(waypoint);

        Marker waypointMarker = map.addMarker( new MarkerOptions()
                .position(waypoint.latlng)
                .title(waypoint.name));

        waypointMarkers.add(waypointMarker);
    }

    private void createWaypoint()
    {
        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.add_waypoint_menu, menu);
                waypointCreationMarker.setVisible(true);
                waypointCreationMarker.setPosition( RouteMapActivity.this.map.getCameraPosition().target );
                map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        waypointCreationMarker.setPosition( RouteMapActivity.this.map.getCameraPosition().target );
                    }
                });

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu)
            {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId())
                {
                    case R.id.action_save:
                        saveWaypoint( route.routeId, map.getCameraPosition().target );
                        mode.finish();
                        break;
                }
                return true ;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                waypointCreationMarker.setVisible(false);
                map.setOnCameraMoveListener(null);
                //ui.waypointMarker.setVisibility(View.GONE);
            }
        }).setTitle("Create waypoint");
    }
}
