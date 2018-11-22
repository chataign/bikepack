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
    long waypointId;
    final long routeId;
    final double latitude;
    final double longitude;
    final String name;
    final String description;
    @Ignore
    final LatLng latlng;
    @Ignore
    final NamedGlobalPosition namedPosition;

    Waypoint( long routeId, double latitude, double longitude, String name, String description )
    {
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.description = description;
        this.latlng = new LatLng(latitude,longitude);
        this.namedPosition = new NamedGlobalPosition(
                new GlobalPosition(latitude,longitude,0),
                name, description );
    }

    Waypoint( long routeId, GlobalPosition position, String name, String description )
    {
        this( routeId, position.latitude, position.longitude, name, description );
    }

    Waypoint( long routeId , NamedGlobalPosition position )
    {
        this( routeId, position.latitude, position.longitude, position.name, position.description );
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

    static Waypoint buildFromXml( long routeId, XmlUtils.XmlObject xml )
            throws NoSuchFieldException, NumberFormatException
    {
        GlobalPosition position = GlobalPosition.buildFromGpx(xml);
        String name = xml.getChild("name").value;
        String description = xml.getChild("desc").value;

        return new Waypoint( routeId, position, name, description );
    }

    @Dao
    interface DatabaseAccess
    {
        @Query("SELECT * FROM Waypoint WHERE routeId=:routeId")
        List<Waypoint> getByRouteId( long routeId );

        @Insert
        long insert(Waypoint waypoint);

        @Insert
        void insert(Waypoint... waypoints);
    }
}
