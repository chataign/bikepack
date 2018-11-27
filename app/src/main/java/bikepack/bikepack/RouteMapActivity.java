package bikepack.bikepack;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.RouteMapActivityBinding;

public class RouteMapActivity extends AppCompatActivity
    implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GetRouteDataQuery.Listener
{
    private static final String LOG_TAG = "MapActivity";
    private static final String MAP_VIEW_BUNDLE_KEY = "MAP_VIEW_BUNDLE_KEY";
    private static final int BOUNDS_PADDING_PX = 100;

    private Route route=null;
    private List<Trackpoint> trackpoints=null;

    private GoogleMap googleMap =null;
    private Marker mapTouchedMarker=null;
    private List<Marker> waypointMarkers=new ArrayList<>();

    private AppDatabase database;
    private RouteMapActivityBinding binding;
    private GetRouteDataQuery routeDataTask=null;
    private MapLayerManager mapLayerManager=null;
    private WaypointListViewModel waypointsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i( LOG_TAG, "onCreate" );

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        route = intent.getParcelableExtra( getString(R.string.route_extra) );
        if ( route == null ) { Log.e( LOG_TAG, "no route data" ); this.finish(); return; }

        this.database = Room.databaseBuilder(
                this, AppDatabase.class, AppDatabase.DB_NAME).build();

        Bundle mapViewBundle = null;

        if (savedInstanceState != null)
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);

        binding = DataBindingUtil.setContentView(this, R.layout.route_map_activity);
        binding.layout.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.mapView.onCreate(mapViewBundle);
        binding.mapView.getMapAsync(this);

        waypointsViewModel = ViewModelProviders.of(this).get(WaypointListViewModel.class);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Log.i( LOG_TAG, "onMapReady" );

        googleMap.setOnMapLoadedCallback(this);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.moveCamera( CameraUpdateFactory.newLatLngBounds(route.bounds,BOUNDS_PADDING_PX ) );

        this.googleMap = googleMap;

        mapLayerManager = new MapLayerManager( this );
        mapLayerManager.setMapLayer( googleMap, mapLayerManager.getMapLayer() );

        Drawable touchMarkerDrawable = getResources().getDrawable(R.drawable.map_marker);
        BitmapDescriptor touchMarkerIcon = getMarkerIconFromDrawable(touchMarkerDrawable);

        mapTouchedMarker = googleMap.addMarker( new MarkerOptions()
                .position( new LatLng(0,0) )
                .icon(touchMarkerIcon)
                .anchor(0.5f,0.5f)
                .draggable(false)
                .visible(false));

        waypointsViewModel.getWaypoints().observe(this, new Observer<List<Waypoint>>() {
            @Override
            public void onChanged(@Nullable List<Waypoint> waypoints) {
                for ( Marker marker : waypointMarkers ) marker.remove();
                waypointMarkers.clear();
                for ( Waypoint waypoint : waypoints )
                {
                    Marker waypointMarker = RouteMapActivity.this.googleMap.addMarker( new MarkerOptions()
                        .position(waypoint.latlng)
                        .title(waypoint.name)
                        .snippet(waypoint.description) );

                    waypointMarkers.add(waypointMarker);
                }
            }
        } );

        routeDataTask = new GetRouteDataQuery( database, route.routeId, this );
        routeDataTask.execute();
    }

    @Override
    protected void onResume()
    {
        Log.i( LOG_TAG, "onResume" );
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onStart()
    {
        Log.i( LOG_TAG, "onStart" );
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onStop()
    {
        Log.i( LOG_TAG, "onStop" );
        super.onStop();
        binding.mapView.onStop();

        if ( routeDataTask != null )
            routeDataTask.cancel(true);
    }

    @Override
    public void onMapLoaded()
    {
        Log.i( LOG_TAG, "onMapLoaded" );
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
        Log.i( LOG_TAG, "onRouteDataReceived trackpoints=" + trackpoints.size() );

        if ( googleMap == null )
        {
            Log.e( LOG_TAG, "onRouteDataReceived but googleMap is null" );
            this.finish();
            return;
        }

        this.trackpoints = trackpoints;

        PolylineOptions polyline = new PolylineOptions()
            .color(Color.RED);

        for ( Trackpoint trackpoint : trackpoints )
            polyline.add(trackpoint.latlng);

        googleMap.addPolyline(polyline);

        binding.elevationView.setTrackpoints(trackpoints); // doesnt block
        binding.layout.setEnabled(true);
        binding.progressBar.setVisibility(View.GONE);
        binding.elevationView.getTrackpoint().observe(this, new Observer<Trackpoint>() {
            @Override
            public void onChanged(@Nullable Trackpoint trackpoint) {
                if ( trackpoint != null ) {
                    mapTouchedMarker.setVisible(true);
                    mapTouchedMarker.setPosition( trackpoint.latlng );
                }
                else mapTouchedMarker.setVisible(false);
            }
        });
    }

    @Override
    public void onRouteDataError( String errorMessage )
    {
        Log.e( LOG_TAG, "route data error=" + errorMessage );
        this.finish();
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
                downloadMap();
                return true;

            case R.id.action_select_layer:
                if ( mapLayerManager != null ) mapLayerManager.selectLayerDialog( googleMap );
                return true;

            case R.id.action_add_waypoint:
                ActionMode.Callback callback = new WaypointCreator(this, googleMap, waypointsViewModel );
                startSupportActionMode(callback).setTitle(R.string.waypoint_create_action_mode_title);
                return true;

            case R.id.action_map_storage:
                File files[] = getFilesDir().listFiles();
                float totalSizeMb=0;
                for ( File file : files ) totalSizeMb += file.length() / 1e6;
                Log.i( LOG_TAG, String.format( "files=%d size=%s", +
                        files.length, StringFormatter.formatFileSize(totalSizeMb) ) );
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void downloadMap() {

        String mapName = mapLayerManager.getMapLayer();

        LatLngBounds.Builder mapBoundsBuilder = new LatLngBounds.Builder();
        for (Trackpoint trackpoint : trackpoints) mapBoundsBuilder.include(trackpoint.latlng);
        final LatLngBounds mapBounds = mapBoundsBuilder.build();

        Intent intent = new Intent(RouteMapActivity.this, DownloadMapActivity.class);
        intent.putExtra(DownloadMapActivity.MAP_NAME_EXTRA, mapName);
        intent.putExtra(DownloadMapActivity.MAP_BOUNDS_EXTRA, mapBounds);

        startActivity(intent);
    }
}
