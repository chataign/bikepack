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
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.SphericalUtil;

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
    private static final int BOUNDS_PADDING_PX = 100;

    private GoogleMap map=null;
    private Route route=null;
    private Marker mapTouchedMarker =null;
    private Polyline selectionPolyline =null;
    private List<Marker> waypointMarkers=new ArrayList<>();
    private ActionMode actionMode=null;

    private List<Trackpoint> trackpoints=null;
    private List<Waypoint> waypoints=null;
    private List<Trackpoint> selectionTrackpoints=null;
    private List<Trackpoint> originalTrackpoints=null;

    private AppDatabase database;
    private RouteMapActivityBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i( LOG_TAG, "onCreate" );

        super.onCreate(savedInstanceState);

        this.database = Room.databaseBuilder(
                this, AppDatabase.class, AppDatabase.DB_NAME).build();

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

        String mapType = getPreferences(Context.MODE_PRIVATE).getString(
                getString(R.string.preferences_map_type),
                getString(R.string.map_type_google_map) );

        setMapType( map, mapType );

        Drawable touchMarkerDrawable = getResources().getDrawable(R.drawable.map_marker);
        BitmapDescriptor touchMarkerIcon = getMarkerIconFromDrawable(touchMarkerDrawable);

        mapTouchedMarker = map.addMarker( new MarkerOptions()
                .position( new LatLng(0,0) )
                .visible(false)
                .icon(touchMarkerIcon)
                .anchor(0.5f,0.5f)
                .draggable(false)
                .zIndex(MARKERS_MAP_DEPTH));

        selectionPolyline = map.addPolyline( new PolylineOptions()
                .visible(false)
                .color(Color.BLUE)
                .zIndex(MARKERS_MAP_DEPTH));

        this.map = map;

        Intent intent = getIntent();
        route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) this.finish();

        map.moveCamera( CameraUpdateFactory.newLatLngBounds(route.bounds,BOUNDS_PADDING_PX ) );

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
        for ( Waypoint waypoint : waypoints ) addWaypointMarker(waypoint);

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
    public void onSelectionCreated( final List<Trackpoint> selection, List<LatLng> points )
    {
        selectionTrackpoints = selection;
        selectionPolyline.setPoints( points );
        selectionPolyline.setVisible(true);

        actionMode = startSupportActionMode(new ActionMode.Callback() {
            int originalStatusBarColor;
            final int actionModeStatusBarColor = getResources().getColor(R.color.black);

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.route_selection_menu, menu);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    originalStatusBarColor = getWindow().getStatusBarColor();
                    getWindow().setStatusBarColor(actionModeStatusBarColor);
                }

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
                        if ( selectionTrackpoints != null ) saveRouteSelection(selectionTrackpoints);
                        mode.finish();
                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getWindow().setStatusBarColor(originalStatusBarColor);

                selectionPolyline.setVisible(false);
                selectionTrackpoints=null;

                trackpoints = originalTrackpoints;

                ui.elevationView.onExitSelection();
                ui.elevationView.setTrackpoints(originalTrackpoints,RouteMapActivity.this);

                originalTrackpoints=null;
            }
        });

        float selectionDistance = (float) SphericalUtil.computeLength(points);
        actionMode.setTitle( "Selection: " + new DistanceFormatter().format(selectionDistance) );
        actionMode.setSubtitle(R.string.route_save_selection_subtitle);
    }

    @Override
    public void onSelectionUpdated( final List<Trackpoint> selection, List<LatLng> points )
    {
        selectionTrackpoints = selection;
        selectionPolyline.setPoints( points );
        selectionPolyline.setVisible(true);

        float selectionDistance = (float) SphericalUtil.computeLength(points);
        actionMode.setTitle( "Selection: " + new DistanceFormatter().format(selectionDistance) );
    }

    @Override
    public void onSelectionDoubleClicked(final List<Trackpoint> selection, List<LatLng> points )
    {
        originalTrackpoints = trackpoints;
        trackpoints = selection;

        ui.elevationView.setTrackpoints(trackpoints,RouteMapActivity.this);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for ( LatLng point : points) boundsBuilder.include(point);

        map.moveCamera(CameraUpdateFactory.newLatLngBounds( boundsBuilder.build(), BOUNDS_PADDING_PX ));
    }

    private void saveRouteSelection( final List<Trackpoint> selection )
    {
        View routeEditorView = getLayoutInflater().inflate(R.layout.route_edit, null );
        final RouteEditBinding routeEditor = DataBindingUtil.bind(routeEditorView);

        routeEditor.name.setText(route.routeName);
        routeEditor.author.setText(route.authorName);
        routeEditor.authorLink.setText(route.authorLink);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RouteMapActivity.this)
            .setTitle(R.string.route_save_selection_title)
            .setView(routeEditorView)
            .setNegativeButton( R.string.dialog_cancel, null )
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

                List<GlobalPosition> trackpointsData= new ArrayList<>(selection.size());
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
                List<NamedGlobalPosition> waypointsData = new ArrayList<>(waypoints.size());

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
        });

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
                    String[] mapTypes = getResources().getStringArray(R.array.map_type_names);
                    setMapType( map, mapTypes[which] );
                    dialog.dismiss();
                }
            });

        AlertDialog layerDialog = builder.create();
        layerDialog.show();
    }

    private void setMapType( GoogleMap map, String mapType )
    {
        if ( map == null )
        {
            Log.e( LOG_TAG, "setMapType: map is null");
            return;
        }
        else if ( mapType.equals( getString(R.string.map_type_google_map) ) )
        {
            map.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        }
        else if ( mapType.equals( getString(R.string.map_type_google_hybrid) ) )
        {
            map.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        }
        else if ( mapType.equals( getString(R.string.map_type_google_terrain) ) )
        {
            map.setMapType( GoogleMap.MAP_TYPE_TERRAIN );
        }
        else if ( mapType.equals( getString(R.string.map_type_google_satellite) ) )
        {
            map.setMapType( GoogleMap.MAP_TYPE_SATELLITE );
        }
        else if ( mapType.equals( getString(R.string.map_type_osm_street) ) )
        {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
            map.addTileOverlay(
                    new TileOverlayOptions().tileProvider(
                            new MapTileProvider( getString(R.string.osm_street_base_url)) ) );
        }
        else if ( mapType.equals( getString(R.string.map_type_osm_cycle) ) )
        {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
            map.addTileOverlay(
                    new TileOverlayOptions().tileProvider(
                            new MapTileProvider( getString(R.string.osm_cycle_base_url)) ) );
        }

        getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString( getString(R.string.preferences_map_type), mapType )
                .apply();
    }

    private void saveWaypoint( final long routeId, final LatLng position )
    {
        View waypointEditView = getLayoutInflater().inflate(R.layout.waypoint_edit, null );
        final WaypointEditBinding waypointEditor = DataBindingUtil.bind(waypointEditView);

        waypointEditor.name.setText("");
        waypointEditor.description.setText("");

        AlertDialog.Builder builder = new AlertDialog.Builder(this )
            .setTitle(R.string.waypoint_create_dialog_title)
            .setView(waypointEditView)
            .setNegativeButton( R.string.dialog_cancel, null )
            .setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, int id)
            {
                String name  = waypointEditor.name.getText().toString();
                String description = waypointEditor.description.getText().toString();

                final Waypoint waypoint = new Waypoint( routeId,
                        new GlobalPosition( position.latitude, position.longitude, 0 ),
                        name, description );

                new InsertWaypointQuery(database, waypoint, new InsertWaypointQuery.Listener() {
                    @Override
                    public void onWaypointInserted(Waypoint waypoint) {
                        waypoints.add(waypoint);
                        addWaypointMarker(waypoint);
                        dialog.dismiss();
                        Toast toast = Toast.makeText( RouteMapActivity.this, R.string.waypoint_created_toast_msg, Toast.LENGTH_LONG );
                        toast.show();
                    }

                    @Override
                    public void onInsertWaypointError(String errorMessage) {
                        Toast toast = Toast.makeText( RouteMapActivity.this, R.string.waypoint_created_toast_msg, Toast.LENGTH_LONG );
                        toast.show();
                    }
                }).execute();
            }
        });

        AlertDialog editDialog = builder.create();
        editDialog.show();
    }

    private void addWaypointMarker( Waypoint waypoint )
    {
        Marker waypointMarker = map.addMarker( new MarkerOptions()
                .position(waypoint.latlng)
                .title(waypoint.name)
                .snippet(waypoint.description) );

        //map.setInfoWindowAdapter( new WaypointInfoAdapter( getLayoutInflater() ) );
        waypointMarkers.add(waypointMarker);
    }

    private void createWaypoint()
    {
        startSupportActionMode( new ActionMode.Callback() {
            private int originalStatusBarColor;
            private final int actionModeStatusBarColor = getResources().getColor(R.color.black);
            private Marker waypointMarker;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    // set the status bar color to black to match the action mode bar
                    originalStatusBarColor = getWindow().getStatusBarColor();
                    getWindow().setStatusBarColor(actionModeStatusBarColor);
                }

                MenuInflater menuInflater = getMenuInflater();
                menuInflater.inflate(R.menu.add_waypoint_menu, menu);

                waypointMarker = map.addMarker( new MarkerOptions()
                        .position( new LatLng(0,0) )
                        .visible(true)
                        .icon( BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) )
                        .anchor(0.5f,1.0f) // middle-bottom of pin marker
                        .draggable(false)
                        .zIndex(MARKERS_MAP_DEPTH)
                        .position( RouteMapActivity.this.map.getCameraPosition().target ) );

                map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() { // keep the marker at the center of the map
                        waypointMarker.setPosition( map.getCameraPosition().target );
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
                        saveWaypoint( route.routeId, waypointMarker.getPosition() );
                        mode.finish();
                        return true;
                }
                return false ;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode)
            {
                // set status bar back to its original color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getWindow().setStatusBarColor(originalStatusBarColor);

                // clear the map
                waypointMarker.remove();
                map.setOnCameraMoveListener(null);
            }
        }).setTitle(R.string.waypoint_create_action_mode_title);
    }
}
