package bikepack.bikepack;

public class DistanceFormater
{
    private boolean force_meters=false;

    DistanceFormater forceMeters( boolean force_meters ) { this.force_meters = force_meters; return this; }

    String format( float distance )
    {
        if ( force_meters || distance < 1000 ) return String.format( "%d m", Math.round(distance) );
        if ( distance < 20 ) return String.format( "%.1f km", distance/1000 );
        return String.format( "%d km", Math.round(distance/1000) );
    }
}
