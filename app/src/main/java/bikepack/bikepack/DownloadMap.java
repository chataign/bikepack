package bikepack.bikepack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import bikepack.bikepack.databinding.DowloadProgressBinding;

class DownloadMap extends AsyncTask< Void, DownloadProgress, Void >
{
    interface Listener
    {
        void onMapDownloaded();
        void onDownloadError(String errorMessage);
    }

    static private final String LOG_TAG = "DownloadMap";

    private final Activity activity;
    private final String mapName;
    private final String baseUrl;
    private final List<TileCoordinates> tileCoordinates;
    private final Listener listener;
    private Exception error = null;
    private AlertDialog downloadDialog=null;
    private DowloadProgressBinding binding;

    DownloadMap( Activity activity,
                String mapName, String baseUrl,
                List<TileCoordinates> tileCoordinates,
                Listener listener )
    {
        this.activity = activity;
        this.mapName = mapName;
        this.baseUrl = baseUrl;
        this.tileCoordinates = tileCoordinates;
        this.listener = listener;
    }

    protected Void doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();
        float downloadedSizeMb=0;
        long downloadTimeMs=0;
        long writeTimeMs=0;
        long totalTimeMs=0;

        try
        {
            Log.i( LOG_TAG, String.format( "Downloading %d tiles...", tileCoordinates.size() ) );

            for ( int i=0; i< tileCoordinates.size(); ++i )
            {
                    if ( isCancelled() ) break;

                    TileCoordinates tile = tileCoordinates.get(i);

                    String tileFilename = String.format("%s-%d-%d-%d.png", mapName, tile.zoom, tile.x, tile.y );
                    File tileFile = activity.getFileStreamPath(tileFilename);

                    if ( !tileFile.exists() )
                    {
                        long time0 = System.currentTimeMillis();

                        String tileUrl = baseUrl
                                .replace("{zoom}", ""+tile.zoom)
                                .replace("{x}",""+tile.x)
                                .replace("{y}",""+tile.y);

                        Log.i( LOG_TAG, "dowloading="+tileUrl );
                        Bitmap tileBitmap = Picasso.get().load(tileUrl).get();
                        long time1 = System.currentTimeMillis();

                        if ( isCancelled() ) break;

                        FileOutputStream fileStream = activity.openFileOutput( tileFilename, Context.MODE_PRIVATE );
                        tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream);
                        long time2 = System.currentTimeMillis();

                        downloadTimeMs += (time1-time0);
                        writeTimeMs += (time2-time1);
                        totalTimeMs += (time2-time0);

                        publishProgress( new DownloadProgress( i, tileCoordinates.size(),
                                downloadedSizeMb, downloadTimeMs, writeTimeMs, totalTimeMs ) );
                    }

                    double fileSizeMb = tileFile.length() / 1e6;
                    downloadedSizeMb += fileSizeMb;

                    Log.i( LOG_TAG, String.format( "saved=%s size=%.2fMb total=%.1fMb",
                            tileFilename, fileSizeMb, downloadedSizeMb ) );
            }

            if ( listener != null ) listener.onMapDownloaded();
        }
        catch( Exception error )
        {
            Log.e( LOG_TAG, error.getMessage() );
            this.error = error;
        }

        long endTime = System.currentTimeMillis();
        Log.i( LOG_TAG, String.format( "executed in %dms", endTime-startTime ) );

        return null;
    }

    @Override
    protected void onPreExecute( )
    {
        View layout = activity.getLayoutInflater().inflate( R.layout.dowload_progress, null );
        binding = DataBindingUtil.bind(layout);

        AlertDialog.Builder downloadDialogBuilder = new AlertDialog.Builder(activity)
                .setTitle("Downloading "+mapName)
                .setView(layout)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DownloadMap.this.cancel(true);
                        }
                    }
                );

        downloadDialog = downloadDialogBuilder.create();
        downloadDialog.show();
    }

    @Override
    protected void onProgressUpdate( DownloadProgress... downloadProgresses)
    {
        try
        {
            DownloadProgress progress = downloadProgresses[0];
            progress.populateDialog(binding);
        }
        catch ( Exception e )
        {
            Log.e( LOG_TAG, e.getMessage() );
            this.cancel(true);
        }
    }

    @Override
    protected void onCancelled ( Void nothing )
    {
        Log.w( LOG_TAG, "onCancelled" );
        if ( downloadDialog!=null ) downloadDialog.dismiss();
    }

    @Override
    protected void onPostExecute( Void nothing )
    {
        Log.w( LOG_TAG, "onPostExecute" );
        if ( downloadDialog!=null ) downloadDialog.dismiss();
        if ( error != null ) listener.onDownloadError( error.getMessage() );
        else if ( listener != null ) listener.onMapDownloaded();
    }
}