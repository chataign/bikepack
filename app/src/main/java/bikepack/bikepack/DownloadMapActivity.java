package bikepack.bikepack;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.model.LatLngBounds;

import bikepack.bikepack.databinding.DownloadMapActivityBinding;

public class DownloadMapActivity extends AppCompatActivity
    implements DownloadMap.Listener
{
    private static final String LOG_TAG = "DownloadMapActivity";

    static final String MAP_NAME_EXTRA = "map_name";
    static final String MAP_BOUNDS_EXTRA = "map_bounds";

    private DownloadMapActivityBinding dialog=null;
    private String mapName, baseUrl;
    private LatLngBounds mapBounds;
    private DownloadMap task=null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mapName = getIntent().getStringExtra(MAP_NAME_EXTRA);
        mapBounds = getIntent().getParcelableExtra(MAP_BOUNDS_EXTRA);

        if ( mapName == null || mapBounds == null )
        {
            Log.e( LOG_TAG, "invalid intent extras" );
            finish();
            return;
        }

        if ( getString(R.string.map_type_osm_street).equals(mapName) )
            baseUrl = getString(R.string.osm_street_base_url);
        else if ( getString(R.string.map_type_osm_cycle).equals(mapName) )
            baseUrl = getString(R.string.osm_cycle_base_url);
        else { Log.e(LOG_TAG, "can't download map=" + mapName); finish(); return; }

        dialog = DataBindingUtil.setContentView(this, R.layout.download_map_activity);
        setTitle( "Downloading: " + mapName );

        dialog.cancelButton.setOnClickListener( new Button.OnClickListener()
            { public void onClick( View view ) { finish(); } } );
    }

    @Override
    protected void onStart()
    {
        Log.i( LOG_TAG, "onStart" );
        task = new DownloadMap( getFilesDir(), mapName, baseUrl, mapBounds, this );
        task.execute();
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        Log.i( LOG_TAG, "onStop" );
        task.cancel(true);
        super.onStop();
    }

    @Override
    public void onMapDownloaded()
    {
        finish();
    }

    @Override
    public void onProgressUpdate( DownloadProgress progress )
    {
        progress.populateDialog(dialog);
    }

    @Override
    public void onDownloadError(String errorMessage)
    {
        Log.e( LOG_TAG, errorMessage );
        finish();
    }
}
