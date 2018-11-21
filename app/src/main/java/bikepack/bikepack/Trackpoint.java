package bikepack.bikepack;

import android.arch.persistence.room.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(
        entity = Route.class, parentColumns = "routeId",
        childColumns = "routeId", onDelete = CASCADE),
        indices = {@Index("routeId")} )
class Trackpoint
{
    static String GPX_TAG = "trkpt";

    @PrimaryKey(autoGenerate = true)
    long trackPointId;
    final long routeId;
    final double latitude;
    final double longitude;
    final float elevation;
    @Ignore
    final LatLng pos;

    Trackpoint( long routeId, double latitude, double longitude, float elevation )
    {
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.pos = new LatLng(latitude,longitude);
    }

    Trackpoint( GlobalPosition position, long routeId )
    {
        this( routeId, position.latitude, position.longitude, position.elevation );
    }

    @Dao
    interface DatabaseAccess
    {
        @Query("SELECT * FROM Trackpoint WHERE routeId = :routeId")
        List<Trackpoint> getByRouteId( long routeId );

        @Insert
        void insert(Trackpoint... trackpoints);
    }
}
