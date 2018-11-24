package bikepack.bikepack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.Layout;
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
    private final LatLngBounds mapBounds;
    private final int minZoomLevel;
    private final int maxZoomLevel;
    private final Listener listener;
    private Exception error = null;
    private AlertDialog dialog=null;
    DowloadProgressBinding binding;

    DownloadMap( Activity activity,
                String mapName, String baseUrl,
                LatLngBounds mapBounds, int minZoomLevel, int maxZoomLevel,
                Listener listener )
    {
        this.activity = activity;
        this.mapName = mapName;
        this.baseUrl = baseUrl;
        this.mapBounds = mapBounds;
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
        this.listener = listener;
    }

    protected Void doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();
        double totalSizeMb=0;
        double downloadTime=0;
        double writeTime=0;
        double totalTime=0;

        try
        {
            List<TileCoordinates> tileCoordinates = TileCoordinates.getTiles( mapBounds, 1, 13 );
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

                        downloadTime += (time1-time0);
                        writeTime += (time2-time1);
                        totalTime += (time2-time0);

                        publishProgress( new DownloadProgress( i, tileCoordinates.size(),
                                totalSizeMb, downloadTime, writeTime, totalTime ) );
                    }

                    double fileSizeMb = tileFile.length() / 1e6;
                    totalSizeMb += fileSizeMb;

                    Log.i( LOG_TAG, String.format( "saved=%s size=%.2fMb total=%.1fMb",
                            tileFilename, fileSizeMb, totalSizeMb ) );
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

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
            .setTitle("Downloading "+mapName)
            .setView(layout)
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DownloadMap.this.cancel(true);
                    }
                }
            );

        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onProgressUpdate( DownloadProgress... downloadProgresses)
    {
        try
        {
            DownloadProgress progress = downloadProgresses[0];

            int downloadTimePercent = (int)( 100*progress.downloadTime/(float)progress.totalTime );
            int writeTimePercent = (int)( 100*progress.writeTime/(float)progress.totalTime );

            binding.progressPercent.setText( String.format( "%d%%", progress.getPercent() ) );
            binding.progressPercentBar.setProgress( progress.getPercent() );
            binding.progressMessage.setText( String.format( "Downloaded %d/%d tiles",
                    progress.numTilesDowloaded, progress.numTilesTotal ) );

            binding.downloadLoadPercent.setText( String.format( "%d%%", downloadTimePercent ) );
            binding.downloadPercentBar.setProgress( downloadTimePercent);

            binding.writeLoadPercent.setText( String.format( "%d%%", writeTimePercent ) );
            binding.writeLoadBar.setProgress( writeTimePercent );
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
        Log.w( LOG_TAG, "cancelled" );
        dialog.dismiss();
    }

    @Override
    protected void onPostExecute( Void nothing )
    {
        dialog.dismiss();
        if ( error != null ) listener.onDownloadError( error.getMessage() );
        else if ( listener != null ) listener.onMapDownloaded();
    }
}