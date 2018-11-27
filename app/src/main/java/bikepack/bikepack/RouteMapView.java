package bikepack.bikepack;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.util.ArrayList;
import java.util.List;

public class RouteMapView extends MapView implements OnMapReadyCallback
{
    private static final String LOG_TAG = "RouteMapView";

    private OnMapReadyCallback listener = null;

    private GoogleMap googleMap;
    private TileOverlay tileOverlay = null;
    private Marker mapTouchedMarker = null;
    private Polyline routePolyline = null;
    private List<Marker> waypointMarkers = new ArrayList<>();

    private final boolean mapToolbarEnabled;
    private final boolean myLocationButtonEnabled;
    private final boolean zoomControlsEnabled;
    private final float mapBoundsPadding;
    private final int touchMarkerResId;

    RouteMapView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RouteMapView,0, 0);

        mapToolbarEnabled = attributes.getBoolean( R.styleable.RouteMapView_map_toolbar_enabled, false );
        myLocationButtonEnabled = attributes.getBoolean( R.styleable.RouteMapView_my_location_button_enabled, true );
        zoomControlsEnabled = attributes.getBoolean( R.styleable.RouteMapView_zoom_controls_enabled, true );
        mapBoundsPadding = attributes.getDimension( R.styleable.RouteMapView_map_bounds_padding, 100 );
        touchMarkerResId = attributes.getResourceId( R.styleable.RouteMapView_marker_drawable, R.drawable.map_marker );
    }

    @Override
    public void getMapAsync( OnMapReadyCallback listener )
    {
        this.listener = listener;
        super.getMapAsync(this);
    }

    @Override
    public void onMapReady( final GoogleMap newGoogleMap )
    {
        Log.i( LOG_TAG, "onMapReady" );

        this.googleMap = newGoogleMap;
        googleMap.getUiSettings().setMyLocationButtonEnabled(myLocationButtonEnabled);
        googleMap.getUiSettings().setZoomControlsEnabled(zoomControlsEnabled);
        googleMap.getUiSettings().setMapToolbarEnabled(mapToolbarEnabled);

        Drawable touchMarkerDrawable = getResources().getDrawable(touchMarkerResId);
        BitmapDescriptor touchMarkerIcon = getMarkerIconFromDrawable(touchMarkerDrawable);

        mapTouchedMarker = googleMap.addMarker( new MarkerOptions()
            .position( new LatLng(0,0) )
            .icon(touchMarkerIcon)
            .anchor(0.5f,0.5f)
            .draggable(false)
            .visible(false));

        if ( listener != null ) listener.onMapReady(googleMap);
    }

    void setTouchTrackpoint( final Trackpoint trackpoint )
    {
        if ( trackpoint == null )
        {
            mapTouchedMarker.setVisible(false);
        }
        else
        {
            mapTouchedMarker.setPosition(trackpoint.latlng);
            mapTouchedMarker.setVisible(true);
        }
    }

    void drawTrackpoints( final List<Trackpoint> trackpoints )
    {
        PolylineOptions polylineOptions = new PolylineOptions().color(Color.RED);
        for ( Trackpoint trackpoint : trackpoints ) polylineOptions.add(trackpoint.latlng);

        if ( routePolyline != null ) routePolyline.remove();
        routePolyline = googleMap.addPolyline(polylineOptions);
    }

    void drawWaypoints( final List<Waypoint> waypoints )
    {
        for ( Marker marker : waypointMarkers ) marker.remove();
        waypointMarkers.clear();

        for ( Waypoint waypoint : waypoints )
        {
            Marker waypointMarker = googleMap.addMarker( new MarkerOptions()
                    .position(waypoint.latlng)
                    .title(waypoint.name)
                    .snippet(waypoint.description) );

            waypointMarkers.add(waypointMarker);
        }
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

    void setMapType(String mapType, Resources resources)
    {
        Log.i( LOG_TAG, "setting map type=" + mapType );

        if ( googleMap == null )
        {
            Log.e( LOG_TAG, "setMapType: map is null");
            return;
        }

        if ( tileOverlay != null )
        {
            Log.i( LOG_TAG, "removing tile overlay" );
            tileOverlay.remove();
            tileOverlay = null;
        }

        if ( mapType.equals( resources.getString(R.string.map_type_google_map) ) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        }
        else if ( mapType.equals( resources.getString(R.string.map_type_google_hybrid) ) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        }
        else if ( mapType.equals( resources.getString(R.string.map_type_google_terrain) ) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_TERRAIN );
        }
        else if ( mapType.equals( resources.getString(R.string.map_type_google_satellite) ) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_SATELLITE );
        }
        else if ( mapType.equals( resources.getString(R.string.map_type_osm_street) ) )
        {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            TileProvider tileProvider = new OfflineTileProvider(
                    resources.getString(R.string.map_type_osm_street), resources.getString(R.string.osm_street_base_url), false );
            tileOverlay = googleMap.addTileOverlay( new TileOverlayOptions().tileProvider(tileProvider) );
        }
        else if ( mapType.equals( resources.getString(R.string.map_type_osm_cycle) ) )
        {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            TileProvider tileProvider = new OfflineTileProvider(
                    resources.getString(R.string.map_type_osm_cycle), resources.getString(R.string.osm_cycle_base_url), false );
            tileOverlay = googleMap.addTileOverlay( new TileOverlayOptions().tileProvider(tileProvider) );
        }
        else
        {
            Log.w( LOG_TAG, "cannot set unknown map type=" + mapType );
            return;
        }
    }
}