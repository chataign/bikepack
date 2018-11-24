package bikepack.bikepack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class OfflineTileProvider implements TileProvider
{
    private static final String LOG_TAG = "OfflineTileProvider";
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final float TILE_STORAGE_MAX_MB = 10; // per map

    private final Context context;
    private final String mapDirectoryName;
    private final String baseUrl;
    private final FileStorageManager storageManager;

    public OfflineTileProvider( Context context, String mapDirectoryName, String baseUrl )
    {
        this.context = context;
        this.mapDirectoryName = mapDirectoryName;
        this.baseUrl = baseUrl;

        List<File> mapFiles = new ArrayList<>();

        for ( String filename : context.fileList() )
        {
            if ( filename.startsWith(mapDirectoryName) )
                mapFiles.add( new File( context.getFilesDir(), filename ) );
        }

        this.storageManager = new FileStorageManager(TILE_STORAGE_MAX_MB, mapFiles );

        Log.i( LOG_TAG, String.format( "map='%s' files=%d size=%.2fMb",
                mapDirectoryName,
                storageManager.getFileCount(),
                storageManager.getSizeMb() ) );
    }

    @Override
    public Tile getTile(int x, int y, int zoom)
    {
        try
        {
            String tileFilename = String.format("%s-%d-%d-%d.png", mapDirectoryName, zoom, x, y );

            Bitmap tileBitmap = null;
            File tileFile = context.getFileStreamPath(tileFilename);

            if ( tileFile.exists() )
            {
                Log.i( LOG_TAG, "loading existing tile="+tileFilename );
                FileInputStream fileStream = context.openFileInput(tileFilename);
                tileBitmap = BitmapFactory.decodeStream(fileStream);
            }
            else // download tile
            {
                String tileUrl = baseUrl
                    .replace("{zoom}", ""+zoom)
                    .replace("{x}",""+x)
                    .replace("{y}",""+y);

                Log.i( LOG_TAG, "retrieving tile="+tileUrl );
                tileBitmap = Picasso.get().load(tileUrl).get();

                Log.i( LOG_TAG, "saving file="+tileFilename );
                FileOutputStream fileStream = context.openFileOutput( tileFilename, Context.MODE_PRIVATE );
                tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream);
                storageManager.add(tileFile);
            }

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return new Tile(TILE_WIDTH, TILE_HEIGHT, byteStream.toByteArray() );
        }
        catch ( Exception e )
        {
            Log.e( LOG_TAG, e.getMessage() );
            return null;
        }
    }
}