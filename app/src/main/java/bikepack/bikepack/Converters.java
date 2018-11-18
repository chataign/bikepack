package bikepack.bikepack;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

public class Converters
{
    @TypeConverter
    public static Date fromTimestamp(Long value)
        { return new Date(value); }

    @TypeConverter
    public static Long toTimestamp(Date date)
        { return date.getTime(); }
}