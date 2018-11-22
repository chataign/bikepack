package bikepack.bikepack;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.ABORT;

@Entity
public class Route implements Parcelable
{
    @PrimaryKey(autoGenerate = true)
    int routeId;
    final String routeName;
    final String authorName;
    final String authorLink;
    final Date dateCreated;
    final Date dateAdded;
    final float totalDistance;
    final float totalAscent;
    final float totalDescent;
    final float highestElevation;
    final float lowestElevation;
    final int numTrackpoints;
    final int numWaypoints;
    final double latitudeNE;
    final double longitudeNE;
    final double latitudeSW;
    final double longitudeSW;

    @Ignore
    final LatLngBounds bounds;

    @Override
    public String toString()
    {
        return String.format( "id=%d name='%s' author='%s'", routeId, routeName, authorName );
    }

    public Route( int routeId,
              String routeName,
              String authorName,
              String authorLink,
              Date dateCreated,
              Date dateAdded,
              float totalDistance,
              float totalAscent,
              float totalDescent,
              float highestElevation,
              float lowestElevation,
              int numTrackpoints,
              int numWaypoints,
              double latitudeNE,
              double longitudeNE,
              double latitudeSW,
              double longitudeSW )
    {
        this.routeId = routeId;
        this.routeName = routeName;
        this.authorName = authorName;
        this.authorLink = authorLink;
        this.dateCreated = dateCreated;
        this.dateAdded = dateAdded;
        this.totalDistance = totalDistance;
        this.totalAscent = totalAscent;
        this.totalDescent = totalDescent;
        this.highestElevation = highestElevation;
        this.lowestElevation = lowestElevation;
        this.numTrackpoints = numTrackpoints;
        this.numWaypoints = numWaypoints;
        this.latitudeNE = latitudeNE;
        this.longitudeNE = longitudeNE;
        this.latitudeSW = latitudeSW;
        this.longitudeSW = longitudeSW;
        this.bounds = new LatLngBounds.Builder()
            .include( new LatLng(latitudeNE,longitudeNE) )
            .include( new LatLng(latitudeSW,longitudeSW) )
            .build();
    }

    public Route( Route route )
    {
        this( route.routeId,
            route.routeName,
            route.authorName,
            route.authorLink,
            route.dateCreated,
            route.dateAdded,
            route.totalDistance,
            route.totalAscent,
            route.totalDescent,
            route.highestElevation,
            route.lowestElevation,
            route.numTrackpoints,
            route.numWaypoints,
            route.latitudeNE,
            route.longitudeNE,
            route.latitudeSW,
            route.longitudeSW);
    }

    private Route( Parcel parcel )
    {
        this( parcel.readInt(), // routeId
            parcel.readString(), // routeName
            parcel.readString(), // authorname
            parcel.readString(), // authorLink
            new Date( parcel.readLong() ), // dateCreated
            new Date( parcel.readLong() ), // dateAdded
            parcel.readFloat(), // totalDistance
            parcel.readFloat(), // totalAscent
            parcel.readFloat(), // totalDescent
            parcel.readFloat(), // highestElevation
            parcel.readFloat(), // lowestElevation
            parcel.readInt(), // numTrackpoints
            parcel.readInt(), // numWaypoints
            parcel.readDouble(), // latitudeNE
            parcel.readDouble(), // longitudeNE
            parcel.readDouble(), // latitudeSW
            parcel.readDouble() // longitudeSW
        );
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel parcel, int flags )
    {
        parcel.writeInt(routeId);
        parcel.writeString(routeName);
        parcel.writeString(authorName);
        parcel.writeString(authorLink);
        parcel.writeLong(dateCreated.getTime());
        parcel.writeLong(dateAdded.getTime());
        parcel.writeFloat(totalDistance);
        parcel.writeFloat(totalAscent);
        parcel.writeFloat(totalDescent);
        parcel.writeFloat(highestElevation);
        parcel.writeFloat(lowestElevation);
        parcel.writeInt(numTrackpoints);
        parcel.writeInt(numWaypoints);
        parcel.writeDouble(latitudeNE);
        parcel.writeDouble(longitudeNE);
        parcel.writeDouble(latitudeSW);
        parcel.writeDouble(longitudeSW);
    }

    static final Parcelable.Creator<Route> CREATOR = new Parcelable.Creator<Route>()
    {
        public Route createFromParcel(Parcel parcel) { return new Route(parcel); }
        public Route[] newArray(int size) { return new Route[size]; }
    };

    @Dao
    interface DatabaseAccess
    {
        @Query("SELECT * FROM Route")
        List<Route> getAll();

        @Query("SELECT * FROM Route WHERE routeId=:routeId")
        Route find( int routeId );

        @Query("SELECT * FROM Route WHERE routeName=:routeName AND authorName=:authorName")
        Route find( String routeName, String authorName );

        @Query("UPDATE Route SET routeName=:routeName, authorName=:authorName, authorLink=:authorLink WHERE routeId=:routeId")
        void update( long routeId, String routeName, String authorName, String authorLink );

        @Insert(onConflict = ABORT)
        long insert(Route route);

        @Delete
        void delete(Route route);
    }

    static Route buildFromData( Metadata metadata, List<GlobalPosition> trackpoints, List<NamedGlobalPosition> waypoints )
    {
        float totalDistance=0, totalAscent=0, totalDescent=0, highestElevation=0, lowestElevation=1e6f;
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for ( int i=1; i< trackpoints.size(); ++i )
        {
            GlobalPosition prev = trackpoints.get(i-1);
            GlobalPosition curr = trackpoints.get(i);

            lowestElevation = Math.min( lowestElevation, curr.elevation );
            highestElevation = Math.max( highestElevation, curr.elevation );

            boundsBuilder.include(curr.position);

            double elevationDiff = curr.elevation - prev.elevation;
            if ( elevationDiff > 0 ) totalAscent += elevationDiff;
            else totalDescent += Math.abs(elevationDiff);

            totalDistance += GlobalPosition.distanceBetween( prev, curr );
        }

        Date dateAdded = Calendar.getInstance().getTime();
        LatLngBounds bounds = boundsBuilder.build();

        return new Route( 0,
            metadata.routeName,
            metadata.authorName,
            metadata.authorLink,
            metadata.dateCreated,
            dateAdded,
            totalDistance,
            totalAscent,
            totalDescent,
            highestElevation,
            lowestElevation,
            trackpoints.size(),
            waypoints.size(),
            bounds.northeast.latitude,
            bounds.northeast.longitude,
            bounds.southwest.latitude,
            bounds.southwest.longitude );
    }
}
