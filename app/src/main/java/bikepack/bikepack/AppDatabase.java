package bikepack.bikepack;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(entities = { Route.class, Trackpoint.class, Waypoint.class }, version = 2 )
@TypeConverters({Converters.class})

public abstract class AppDatabase extends RoomDatabase
{
    static final String DB_NAME = "app_db";

    public abstract Route.DatabaseAccess routes();
    public abstract Trackpoint.DatabaseAccess trackpoints();
    public abstract Waypoint.DatabaseAccess waypoints();

    private static AppDatabase INSTANCE=null;

    static AppDatabase getInstance(Context context )
    {
        if ( INSTANCE == null )
        {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, AppDatabase.DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
        }

        return INSTANCE;
    }
}