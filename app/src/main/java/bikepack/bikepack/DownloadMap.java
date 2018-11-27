package bikepack.bikepack;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

class DownloadMap extends AsyncTask< Void, DownloadProgress, Void >
{
    interface Listener
    {
        void onMapDownloaded();
        void onProgressUpdate( DownloadProgress progress );
        void onDownloadError(String errorMessage);
    }

    static private final String LOG_TAG = "DownloadMap";

    private final File targetDirectory;
    private final String mapName;
    private final String baseUrl;
    private final LatLngBounds mapBounds;
    private final Listener listener;

    private Exception error = null;

    DownloadMap( File targetDirectory, String mapName, String baseUrl, LatLngBounds mapBounds, Listener listener )
    {
        this.targetDirectory = targetDirectory;
        this.mapName = mapName;
        this.baseUrl = baseUrl;
        this.mapBounds = mapBounds;
        this.listener = listener;
    }

    protected Void doInBackground(Void... nothing)
    {
        long startTime = System.currentTimeMillis();

        try
        {
            if ( !targetDirectory.exists() || !targetDirectory.isDirectory() )
                throw new Exception("invalid target directory=" + targetDirectory.getAbsolutePath() );

            final List<TileCoordinates> tileCoordinates = TileCoordinates.getTiles( mapBounds, 1, 12 );
            DownloadProgress progress = new DownloadProgress( tileCoordinates.size() );

            Log.i( LOG_TAG, String.format( "Downloading %d tiles...", tileCoordinates.size() ) );

            for ( int i=0; i< tileCoordinates.size(); ++i )
            {
                    if ( isCancelled() ) break;

                    TileCoordinates tile = tileCoordinates.get(i);

                    String tileFilename = String.format("%s-%d-%d-%d.png", mapName, tile.zoom, tile.x, tile.y );
                    File tileFile = new File( targetDirectory, tileFilename );

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

                        FileOutputStream fileStream = new FileOutputStream(tileFile); // TODO = activity.openFileOutput( tileFilename, Context.MODE_PRIVATE );
                        tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream);
                        long time2 = System.currentTimeMillis();

                        progress.numTilesDowloaded = i;
                        progress.downloadTimeMs += (time1-time0);
                        progress.writeTimeMs += (time2-time1);
                        progress.totalTimeMs += (time2-time0);
                    }

                    double fileSizeMb = tileFile.length() / 1e6;
                    progress.downloadedSizeMb += fileSizeMb;
                    publishProgress( progress );

                    Log.i( LOG_TAG, String.format( "saved=%s size=%.2fMb total=%.1fMb",
                            tileFilename, fileSizeMb, progress.downloadedSizeMb ) );
            }
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
    protected void onProgressUpdate( DownloadProgress... progresses )
    {
        if ( listener != null )
            listener.onProgressUpdate( progresses[0] );
    }

    @Override
    protected void onCancelled ( Void nothing )
    {
        Log.w( LOG_TAG, "onCancelled" );
    }

    @Override
    protected void onPostExecute( Void nothing )
    {
        Log.w( LOG_TAG, "onPostExecute" );
        if ( error != null ) listener.onDownloadError( error.getMessage() );
        else if ( listener != null ) listener.onMapDownloaded();
    }
}