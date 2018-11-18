package bikepack.bikepack;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Route.class, parentColumns = "routeId",
        childColumns = "routeId", onDelete = CASCADE),
        indices = {@Index("routeId")})
class Waypoint
{
    static String GPX_TAG = "wpt";

    @PrimaryKey(autoGenerate = true)
    int waypointId;
    final long routeId;
    final double latitude;
    final double longitude;
    final String name;
    final String description;
    @Ignore
    final LatLng pos;

    Waypoint( long routeId, double latitude, double longitude, String name, String description )
    {
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.description = description;
        this.pos = new LatLng(latitude,longitude);
    }

    /*
    <wpt lat="57.895556" lon="-5.159948">
        <ele>.0</ele>
        <time>2017-03-13T17:38:08Z</time>
        <name>Chippy</name>
        <cmt></cmt>
        <desc></desc>
    </wpt>
    */

    static Waypoint build( XmlUtils.XmlObject xml, long routeId )
            throws NoSuchFieldException, NumberFormatException
    {
        double latitude = Double.parseDouble( xml.getAttribute("lat").value );
        double longitude = Double.parseDouble( xml.getAttribute("lon").value );
        String name = xml.getChild("name").value;
        String description = xml.getChild("desc").value;

        return new Waypoint( routeId, latitude, longitude, name, description );
    }

    @Dao
    interface DatabaseAccess
    {
        @Query("SELECT * FROM Waypoint")
        List<Waypoint> getAll();

        @Query("SELECT * FROM Waypoint WHERE routeId=:routeId")
        List<Waypoint> getByRouteId( long routeId );

        @Insert
        void insert(Waypoint... waypoints);
    }
}
