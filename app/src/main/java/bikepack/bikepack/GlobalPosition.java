package bikepack.bikepack;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

public class GlobalPosition
{
    final double latitude;
    final double longitude;
    final float elevation;
    final LatLng position;

    GlobalPosition( double latitude, double longitude, float elevation )
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.position = new LatLng(latitude,longitude);
    }

    GlobalPosition( GlobalPosition position )
    {
        this( position.latitude, position.longitude, position.elevation );
    }

    static GlobalPosition buildFromGpx( XmlUtils.XmlObject xml )
            throws NoSuchFieldException, NumberFormatException
    {
        double latitude = Double.parseDouble( xml.getAttribute("lat").value );
        double longitude = Double.parseDouble( xml.getAttribute("lon").value );
        float elevation = Float.parseFloat( xml.getChild("ele").value );

        return new GlobalPosition( latitude, longitude, elevation );
    }

    static double distanceBetween( GlobalPosition pos1, GlobalPosition pos2 )
    {
        return SphericalUtil.computeDistanceBetween( pos1.position, pos2.position );
    }
}
