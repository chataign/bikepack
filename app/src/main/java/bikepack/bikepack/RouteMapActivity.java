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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

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
    private Marker touchMarker =null;
    private Polyline selectionPolyline =null;

    private List<Trackpoint> trackpoints=null;
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
    public void onRouteDataReceived( List<Trackpoint> trackpoints, List<Waypoint> waypoints )
    {
        this.trackpoints = trackpoints;
        drawMap( map, trackpoints, waypoints );

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
    public void onMapReady(GoogleMap map)
    {
        Log.i( LOG_TAG, "onMapReady" );

        map.setOnMapLoadedCallback(this);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType( preferences.getInt(getString(R.string.preferences_map_type), GoogleMap.MAP_TYPE_HYBRID ) );

        Drawable touch_drawable = getResources().getDrawable(R.drawable.map_marker);
        BitmapDescriptor touch_icon = getMarkerIconFromDrawable(touch_drawable);

        touchMarker = map.addMarker( new MarkerOptions()
                .position( new LatLng(0,0) )
                .visible(false)
                .icon(touch_icon)
                .anchor(0.5f,0.5f)
                .draggable(false)
                .zIndex(MARKERS_MAP_DEPTH));

        this.map = map;
    }

    @Override
    public void onMapLoaded()
    {
        Log.i( LOG_TAG, "onMapLoaded" );

        Intent intent = getIntent();
        route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) this.finish();

        GetRouteDataQuery task = new GetRouteDataQuery( database, route.routeId, this );
        task.execute();
    }

    @Override
    public void onValueTouched( Trackpoint trackpoint )
    {
        touchMarker.setVisible(true);
        touchMarker.setPosition( trackpoint.pos );
    }

    @Override
    public void onValueReleased()
    {
        touchMarker.setVisible(false);
    }

    @Override
    public void onSelectionStart( Trackpoint trackpoint )
    {
        if ( selectionPolyline != null )
            selectionPolyline.remove();

        selectionPolyline = map.addPolyline( new PolylineOptions()
                .visible(true)
                .color(Color.BLUE)
                .zIndex(MARKERS_MAP_DEPTH)
                .add(trackpoint.pos));
    }

    @Override
    public void onSelectionUpdated( List<Trackpoint> selection, List<LatLng> points )
    {
        if ( selectionPolyline != null )
            selectionPolyline.setPoints( points );
    }

    @Override
    public void onSelectionEnd(final List<Trackpoint> selection, List<LatLng> points )
    {
        final List<Trackpoint> originalTrackpoints = trackpoints;
        trackpoints = selection;

        ui.elevationView.setTrackpoints(trackpoints,RouteMapActivity.this);

        LatLngBounds.Builder bounds_builder = new LatLngBounds.Builder();
        for ( LatLng point : points) bounds_builder.include(point);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds( bounds_builder.build(), 10));

        startSupportActionMode(new ActionMode.Callback() {
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

                List<GlobalPosition> positions = new ArrayList<>();
                for ( Trackpoint trackpoint : selection ) positions.add(
                        new GlobalPosition( trackpoint.latitude, trackpoint.longitude, trackpoint.elevation ) );

                /*
                AppRepository.CreateRouteTask createRouteTask = new AppRepository.CreateRouteTask( database, metadata, positions, new AppRepository.CreateRouteListener() {
                    public void onRouteCreated(Route route) {
                        Toast toast = Toast.makeText( RouteMapActivity.this,
                                R.string.route_created, Toast.LENGTH_SHORT );
                        toast.show();
                    }
                    public void onError(String error_message) {
                        Toast toast = Toast.makeText( RouteMapActivity.this,
                                R.string.route_creation_failed, Toast.LENGTH_SHORT );
                        toast.show();
                    }
                });

                createRouteTask.execute();
                 */
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

            case R.id.action_show_layers:
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

    static void drawMap( @NonNull GoogleMap map,
                  @NonNull List<Trackpoint> trackpoints,
                  @NonNull List<Waypoint> waypoints )
    {
        long before = System.currentTimeMillis();

        PolylineOptions polyline = new PolylineOptions()
                .color(Color.RED)
                .zIndex(ROUTE_MAP_DEPTH);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for ( Trackpoint trackpoint : trackpoints )
        {
            polyline.add(trackpoint.pos);
            boundsBuilder.include(trackpoint.pos);
        }

        map.addPolyline(polyline);

        for ( Waypoint waypoint : waypoints)
        {
            boundsBuilder.include(waypoint.pos);

            map.addMarker( new MarkerOptions()
                    .position(waypoint.pos)
                    .title(waypoint.name));
        }

        LatLngBounds bounds = boundsBuilder.build();
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));

        long after = System.currentTimeMillis();
        Log.i( LOG_TAG, String.format( "map drawn in %dms", after-before ) );
    }
}
