package bikepack.bikepack;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;

class TileCoordinates
{
    private static final String LOG_TAG = "TileCoordinates";

    final long x;
    final long y;
    final int zoom;

    TileCoordinates( long x, long y, int zoom )
    {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
    }

    static TileCoordinates build(LatLng location, int zoom )
    {
        // https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames

        double latitudeRad = Math.toRadians(location.latitude);

        long n = (long) Math.pow( 2, zoom );
        long x = (long)( n * (location.longitude + 180.0) / 360.0 );
        long y = (long)( n * (1 - ( Math.log( Math.tan(latitudeRad) + 1/Math.cos(latitudeRad)) / PI ) ) / 2 );

        return new TileCoordinates(x,y,zoom);
    }

    static List<TileCoordinates> getTiles(LatLngBounds mapBounds, int minZoomLevel, int maxZoomLevel )
    {
        List<TileCoordinates> tiles = new ArrayList<>();

        for ( int zoomLevel = minZoomLevel; zoomLevel <= maxZoomLevel; ++zoomLevel )
        {
            TileCoordinates southwestCoords = TileCoordinates.build( mapBounds.southwest, zoomLevel );
            TileCoordinates northeastCoords = TileCoordinates.build( mapBounds.northeast, zoomLevel );

            long numTiles=0;

            for ( long x = southwestCoords.x; x <= northeastCoords.x; ++x ) // x is west to east
            {
                for ( long y = northeastCoords.y; y <= southwestCoords.y; ++y ) // y is north to south
                {
                    tiles.add( new TileCoordinates( x, y, zoomLevel) );
                    ++numTiles;
                }
            }

            Log.i( LOG_TAG, String.format("zoom=%d southwest=(%d,%d) northeast=(%d,%d) tiles=%d",
                    zoomLevel,
                    southwestCoords.x, southwestCoords.y,
                    northeastCoords.x, northeastCoords.y,
                    numTiles ) );
        }

        return tiles;
    }
}