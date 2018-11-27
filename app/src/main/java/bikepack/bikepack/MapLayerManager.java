package bikepack.bikepack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

public class MapLayerManager
{
    private static final String  LOG_TAG = "MapLayerManager";

    private final Activity activity;
    private final SharedPreferences preferences;

    private TileOverlay tileOverlay =null;

    private final String TYPE_MAP;
    private final String TYPE_TERRAIN;
    private final String TYPE_HYBRID;
    private final String TYPE_SATELLITE;
    private final String TYPE_OSM_STREET;
    private final String TYPE_OSM_CYCLE;
    private final String URL_OSM_STREET;
    private final String URL_OSM_CYCLE;
    private final String PREFERENCES_MAP_TYPE;

    MapLayerManager( Activity activity )
    {
        this.activity = activity;
        this.preferences = activity.getPreferences(Context.MODE_PRIVATE);

        TYPE_MAP = activity.getString(R.string.map_type_google_map);
        TYPE_TERRAIN = activity.getString(R.string.map_type_google_terrain);
        TYPE_HYBRID = activity.getString(R.string.map_type_google_hybrid);
        TYPE_SATELLITE = activity.getString(R.string.map_type_google_satellite);
        TYPE_OSM_STREET = activity.getString(R.string.map_type_osm_street);
        TYPE_OSM_CYCLE = activity.getString(R.string.map_type_osm_cycle);

        URL_OSM_STREET = activity.getString(R.string.osm_street_base_url);
        URL_OSM_CYCLE = activity.getString(R.string.osm_cycle_base_url);
        PREFERENCES_MAP_TYPE = activity.getString(R.string.preferences_map_type);
    }

    String getMapLayer()
    {
        return preferences.getString( PREFERENCES_MAP_TYPE, TYPE_MAP );
    }

    void setMapLayer( GoogleMap googleMap, String mapType )
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

        if ( mapType.equals(TYPE_MAP) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        }
        else if ( mapType.equals(TYPE_HYBRID) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        }
        else if ( mapType.equals(TYPE_TERRAIN) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_TERRAIN );
        }
        else if ( mapType.equals(TYPE_SATELLITE) )
        {
            googleMap.setMapType( GoogleMap.MAP_TYPE_SATELLITE );
        }
        else if ( mapType.equals(TYPE_OSM_STREET) )
        {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            TileProvider tileProvider = new OfflineTileProvider( TYPE_OSM_STREET, URL_OSM_STREET, false );
            tileOverlay = googleMap.addTileOverlay( new TileOverlayOptions().tileProvider(tileProvider) );
        }
        else if ( mapType.equals(TYPE_OSM_CYCLE) )
        {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            TileProvider tileProvider = new OfflineTileProvider( TYPE_OSM_CYCLE, URL_OSM_CYCLE, false );
            tileOverlay = googleMap.addTileOverlay( new TileOverlayOptions().tileProvider(tileProvider) );
        }
        else
        {
            Log.w( LOG_TAG, "cannot set unknown map type=" + mapType );
            return;
        }

        preferences.edit().putString( PREFERENCES_MAP_TYPE, mapType ).apply();
    }

    void selectLayerDialog( final GoogleMap googleMap )
    {
        RadioGroup radioGroup = (RadioGroup) activity.getLayoutInflater().inflate(R.layout.map_types, null );

        final AlertDialog layerTypeDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.map_layer_dialog_title)
                .setView(radioGroup)
                .create();

        final String[] mapTypes = activity.getResources().getStringArray(R.array.map_type_names);

        String currentMapType = getMapLayer();

        for ( final String mapType : mapTypes )
        {
            RadioButton radioButton = (RadioButton) activity.getLayoutInflater().inflate(R.layout.map_types_button, null );
            radioButton.setText(mapType);
            radioButton.setChecked( mapType.equals(currentMapType) );
            radioButton.setOnClickListener( new RadioButton.OnClickListener() {
                public void onClick(View view)
                {
                    setMapLayer( googleMap, mapType );
                    layerTypeDialog.dismiss();
                }
            } );
            radioGroup.addView(radioButton);
        }

        layerTypeDialog.show();
    }
}
