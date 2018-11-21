package bikepack.bikepack;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = { Route.class, Trackpoint.class, Waypoint.class }, version = 2 )
@TypeConverters({Converters.class})

public abstract class AppDatabase extends RoomDatabase
{
    static final String DB_NAME = "app_db";

    public abstract Route.DatabaseAccess routes();
    public abstract Trackpoint.DatabaseAccess trackpoints();
    public abstract Waypoint.DatabaseAccess waypoints();
}