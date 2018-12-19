package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.Arrays;
import java.util.List;

import bikepack.bikepack.databinding.RouteMapActivityBinding;

public class RouteMapActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private static final String LOG_TAG = "MapActivity";
    private static final String MAP_VIEW_BUNDLE_KEY = "MAP_VIEW_BUNDLE_KEY";

    private static final RouteMapView.MapLayer ROUTE_LAYER = new RouteMapView.MapLayer(1, Color.RED,1);
    private static final RouteMapView.MapLayer SELECTION_LAYER = new RouteMapView.MapLayer(2, Color.BLUE,2);

    private RouteMapActivityBinding binding;
    private GoogleMap googleMap = null;

    // view models
    private MapTypeViewModel mapTypeViewModel=null;
    private TrackpointListViewModel trackpointsViewModel=null;
    private WaypointListViewModel waypointsViewModel=null;
    private ActionMode selectionActionMode=null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i( LOG_TAG, "onCreate" );
        super.onCreate(savedInstanceState);

        Bundle mapViewBundle = null;

        if (savedInstanceState != null)
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);

        binding = DataBindingUtil.setContentView(this, R.layout.route_map_activity);
        binding.mapView.onCreate(mapViewBundle);
        binding.mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Log.i( LOG_TAG, "onMapReady" );

        Intent intent = getIntent();
        final Route route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) { Log.e( LOG_TAG, "no route data" ); this.finish(); return; }

        this.googleMap = googleMap;
        googleMap.moveCamera( CameraUpdateFactory.newLatLngBounds( route.bounds,100 ) );

        mapTypeViewModel = ViewModelProviders.of(this).get(MapTypeViewModel.class);
        mapTypeViewModel.init( getPreferences(MODE_PRIVATE),
                getString(R.string.preferences_map_type),
                getString(R.string.map_type_google_map) );
        mapTypeViewModel.getMapType().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String mapType) { binding.mapView.setMapType( mapType, getResources() ); }
        });

        ViewModelProvider.Factory trackpointModelFactory = new TrackpointListViewModel.Factory( this.getApplication(), route.routeId );
        trackpointsViewModel = ViewModelProviders.of(this, trackpointModelFactory ).get(TrackpointListViewModel.class);
        trackpointsViewModel.getTrackpoints().observe(this, new Observer<List<Trackpoint>>() {
            public void onChanged( List<Trackpoint> trackpoints) {
                binding.mapView.clearLayer( ROUTE_LAYER );
                binding.mapView.drawTrackpoints( ROUTE_LAYER, trackpoints, Color.RED );
                binding.elevationView.setTrackpoints(trackpoints);
            }
        });

        ViewModelProvider.Factory waypointModelFactory = new WaypointListViewModel.Factory( this.getApplication(), route.routeId );
        waypointsViewModel = ViewModelProviders.of(this, waypointModelFactory).get(WaypointListViewModel.class);
        waypointsViewModel.getWaypoints().observe(this, new Observer<List<Waypoint>>() {
            public void onChanged( List<Waypoint> waypoints) { binding.mapView.drawWaypoints(waypoints); }
            } );

        binding.elevationView.getTrackpoint().observe(this, new Observer<Trackpoint>() {
            public void onChanged( Trackpoint trackpoint) { binding.mapView.setTouchTrackpoint(trackpoint); }
        });

        binding.elevationView.getSelection().observe(this, new Observer< List<Trackpoint> >() {
            public void onChanged( List<Trackpoint> trackpoints) {
                if ( trackpoints == null )
                {
                    if ( selectionActionMode != null ) { selectionActionMode.finish(); selectionActionMode=null; }
                    binding.mapView.clearLayer( SELECTION_LAYER );
                }
                else
                {
                    if ( selectionActionMode == null ) selectionActionMode = startSupportActionMode(
                            new RouteSelectionActionMode(RouteMapActivity.this, route,
                                waypointsViewModel.getWaypoints(),
                                binding.elevationView.getSelection(),
                                new RouteSelectionActionMode.OnDestroyListener() {
                                    public void onDestroyActionMode() { binding.elevationView.clearSelection(); }
                                }) );

                    binding.mapView.drawTrackpoints( SELECTION_LAYER, trackpoints, Color.BLUE ); }
                }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        binding.mapView.onStop();
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
                switch ( binding.elevationView.getVisibility() )
                {
                    case View.VISIBLE:
                        binding.elevationView.setVisibility( View.GONE );
                        break;
                    case View.INVISIBLE:
                    case View.GONE:
                        binding.elevationView.setVisibility( View.VISIBLE );
                        break;
                }
                return true;

            case R.id.action_map_download:
                return true;

            case R.id.action_select_layer:
                final String[] mapTypeNames = getResources().getStringArray(R.array.map_type_names);
                final String currentMapType = mapTypeViewModel.getMapType().getValue();
                final int checkedItem = Arrays.asList(mapTypeNames).indexOf(currentMapType);

                final AlertDialog layerTypeDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.map_layer_dialog_title)
                        .setSingleChoiceItems(R.array.map_type_names, checkedItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapTypeViewModel.setMapType( mapTypeNames[which] );
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapTypeViewModel.setMapType( currentMapType );
                            }
                        })
                        .setPositiveButton(R.string.dialog_ok, null)
                        .create();

                layerTypeDialog.show();
                return true;

            case R.id.action_add_waypoint:
                if ( googleMap == null ) { Log.e( LOG_TAG, "action_add_waypoint: map object is null" ); }
                else startSupportActionMode( new AddWaypointActionMode( this, googleMap, waypointsViewModel ) );
                return true;

            case R.id.action_map_storage:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
