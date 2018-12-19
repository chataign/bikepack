package bikepack.bikepack;

import android.app.Application;
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

public class OfflineTileProvider implements TileProvider
{
    private static final String LOG_TAG = "OfflineTileProvider";
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final float TILE_STORAGE_MAX_MB = 10; // per map

    private final File mapTilesDir;
    private final String baseUrl;
    private final boolean downloadIfMissing;
    //private final FileStorageManager storageManager;

    public OfflineTileProvider( File tilesDir, String mapName, String baseUrl, boolean downloadIfMissing )
    {
        this.mapTilesDir = new File( tilesDir, mapName );
        this.mapTilesDir.mkdirs();
        this.baseUrl = baseUrl;
        this.downloadIfMissing = downloadIfMissing;
        //this.storageManager = new FileStorageManager(context.getFilesDir(), TILE_STORAGE_MAX_MB, mapName);
    }

    @Override
    public Tile getTile(int x, int y, int zoom)
    {
        try
        {
            Bitmap tileBitmap = null;
            File tileFile = new File( mapTilesDir, String.format("tile-%d-%d-%d.png", zoom, x, y ) );

            if ( tileFile.exists() )
            {
                FileInputStream fileStream = new FileInputStream(tileFile);
                tileBitmap = BitmapFactory.decodeStream(fileStream);
                Log.i( LOG_TAG, String.format("loading existing %dx%d tile=%s size=%dKb",
                        tileBitmap.getWidth(),
                        tileBitmap.getHeight(),
                        tileFile.getName(),
                        Math.round(tileFile.length()/1000) ) );
            }
            else if (downloadIfMissing) // download tile
            {
                String tileUrl = baseUrl
                    .replace("{zoom}", ""+zoom)
                    .replace("{x}",""+x)
                    .replace("{y}",""+y);

                Log.i( LOG_TAG, "retrieving="+tileUrl );
                tileBitmap = Picasso.get().load(tileUrl).get();

                Log.i( LOG_TAG, "saving="+tileFile.getName() );
                FileOutputStream fileStream = new FileOutputStream(tileFile);
                tileBitmap.compress(Bitmap.CompressFormat.PNG, 0/*ignored for PNGs*/, fileStream);

                //if ( storageManager == null ) Log.w( LOG_TAG, "storage manager is null" );
                //else storageManager.add(tileFile);
            }
            else return null;

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            tileBitmap.compress(Bitmap.CompressFormat.PNG, 0/*ignored for PNGs*/, byteStream); // TODO why compress again
            return new Tile(TILE_WIDTH, TILE_HEIGHT, byteStream.toByteArray() );
        }
        catch ( Exception e )
        {
            Log.e( LOG_TAG, e.getMessage() );
            return null;
        }
    }
}