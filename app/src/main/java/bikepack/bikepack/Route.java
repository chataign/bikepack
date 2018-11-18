package bikepack.bikepack;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.ABORT;

@Entity
class Route implements Parcelable
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

    @Override
    public String toString()
    {
        return String.format( "id=%d name='%s' author='%s'", routeId, routeName, authorName );
    }

    public Route( String routeName,
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
                  int numWaypoints )
    {
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
    }

    public Route( Route route )
    {
        this.routeName = route.routeName;
        this.authorName = route.authorName;
        this.authorLink = route.authorLink;
        this.dateCreated = route.dateCreated;
        this.dateAdded = route.dateAdded;
        this.totalDistance = route.totalDistance;
        this.totalAscent = route.totalAscent;
        this.totalDescent = route.totalDescent;
        this.highestElevation = route.highestElevation;
        this.lowestElevation = route.lowestElevation;
        this.numTrackpoints = route.numTrackpoints;
        this.numWaypoints = route.numWaypoints;
    }

    private Route( Parcel parcel )
    {
        this.routeId = parcel.readInt();
        this.routeName = parcel.readString();
        this.authorName = parcel.readString();
        this.authorLink = parcel.readString();
        this.dateCreated = new Date( parcel.readLong() );
        this.dateAdded = new Date( parcel.readLong() );
        this.totalDistance = parcel.readFloat();
        this.totalAscent = parcel.readFloat();
        this.totalDescent = parcel.readFloat();
        this.highestElevation = parcel.readFloat();
        this.lowestElevation = parcel.readFloat();
        this.numTrackpoints = parcel.readInt();
        this.numWaypoints = parcel.readInt();
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
}
