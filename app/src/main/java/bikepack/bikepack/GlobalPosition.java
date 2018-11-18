package bikepack.bikepack;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

public class GlobalPosition
{
    final LatLng latlng;
    final float elevation;

    GlobalPosition( double latitude, double longitude, float elevation )
    {
        this.latlng = new LatLng(latitude,longitude);
        this.elevation = elevation;
    }

    static GlobalPosition buildFromXml( XmlUtils.XmlObject xml )
            throws NoSuchFieldException, NumberFormatException
    {
        double latitude = Double.parseDouble( xml.getAttribute("lat").value );
        double longitude = Double.parseDouble( xml.getAttribute("lon").value );
        float elevation = Float.parseFloat( xml.getChild("ele").value );

        return new GlobalPosition( latitude, longitude, elevation );
    }

    static double distance( GlobalPosition pos1, GlobalPosition pos2 )
    {
        return SphericalUtil.computeDistanceBetween( pos1.latlng, pos2.latlng );
    }
}
