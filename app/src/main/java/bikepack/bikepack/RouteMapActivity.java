package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import java.util.List;

import bikepack.bikepack.databinding.RouteMapActivityBinding;

public class RouteMapActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private static final String LOG_TAG = "MapActivity";
    private static final String MAP_VIEW_BUNDLE_KEY = "MAP_VIEW_BUNDLE_KEY";

    private RouteMapActivityBinding binding;
    private GoogleMap googleMap = null;

    // view models
    private MapTypeViewModel mapTypeViewModel=null;
    private TrackpointListViewModel trackpointsViewModel=null;
    private WaypointListViewModel waypointsViewModel=null;

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

        mapTypeViewModel = ViewModelProviders.of(this).get(MapTypeViewModel.class);
        waypointsViewModel = ViewModelProviders.of(this).get(WaypointListViewModel.class);
        trackpointsViewModel = ViewModelProviders.of(this).get(TrackpointListViewModel.class);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Log.i( LOG_TAG, "onMapReady" );

        Intent intent = getIntent();
        Route route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) { Log.e( LOG_TAG, "no route data" ); this.finish(); return; }

        this.googleMap = googleMap;
        googleMap.moveCamera( CameraUpdateFactory.newLatLngBounds( route.bounds,100 ) );

        mapTypeViewModel.init( getPreferences(MODE_PRIVATE),
                getString(R.string.preferences_map_type),
                getString(R.string.map_type_google_map) );
        mapTypeViewModel.getMapType().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String mapType) { binding.mapView.setMapType( mapType, getResources() ); }
        });

        trackpointsViewModel.init(route.routeId);
        trackpointsViewModel.getTrackpoints().observe(this, new Observer<List<Trackpoint>>() {
            public void onChanged( List<Trackpoint> trackpoints) {
                binding.mapView.drawTrackpoints(trackpoints);
                binding.elevationView.drawTrackpoints(trackpoints);
            }
        });

        waypointsViewModel.init(route.routeId);
        waypointsViewModel.getWaypoints().observe(this, new Observer<List<Waypoint>>() {
            public void onChanged( List<Waypoint> waypoints) { binding.mapView.drawWaypoints(waypoints); }
            } );

        binding.elevationView.getTrackpoint().observe(this, new Observer<Trackpoint>() {
            public void onChanged( Trackpoint trackpoint) { binding.mapView.setTouchTrackpoint(trackpoint); }
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
                RadioGroup radioGroup = (RadioGroup) getLayoutInflater().inflate(R.layout.map_types, null );

                final AlertDialog layerTypeDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.map_layer_dialog_title)
                        .setView(radioGroup)
                        .create();

                final String[] mapTypes = getResources().getStringArray(R.array.map_type_names);
                String currentMapType = mapTypeViewModel.getMapType().getValue();

                for ( final String mapType : mapTypes )
                {
                    RadioButton radioButton = (RadioButton) getLayoutInflater().inflate(R.layout.map_types_button, null );
                    radioButton.setText(mapType);
                    radioButton.setChecked( mapType.equals(currentMapType) );
                    radioButton.setOnClickListener( new RadioButton.OnClickListener() {
                        public void onClick(View view)
                        {
                            mapTypeViewModel.setMapType(mapType);
                            layerTypeDialog.dismiss();
                        }
                    } );
                    radioGroup.addView(radioButton);
                }

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
