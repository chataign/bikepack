package bikepack.bikepack;

import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class MapTileProvider extends UrlTileProvider
{
    private static final String LOG_TAG = "MapTileProvider";
    private final String baseUrl;

    public MapTileProvider( String baseUrl )
    {
        super(256, 256);
        this.baseUrl = baseUrl;
    }

    @Override
    public URL getTileUrl(int x, int y, int zoom)
    {
        try
        {
            URL url = new URL( baseUrl
                .replace("{zoom}", ""+zoom)
                .replace("{x}",""+x)
                .replace("{y}",""+y) );

            Log.i( LOG_TAG, "url="+url );
            return url;
        }
        catch ( MalformedURLException e )
        {
            e.printStackTrace();
        }

        return null;
    }
}
