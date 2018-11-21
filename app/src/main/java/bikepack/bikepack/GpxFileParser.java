package bikepack.bikepack;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static bikepack.bikepack.XmlUtils.readNext;

public class GpxFileParser extends AsyncTask< Void, Void, Void >
{
    interface Listener
    {
        void onGpxFileRead( Metadata metadata, List<GlobalPosition> trackpoints, List<NamedGlobalPosition> waypoints );
        void onGpxReadError( String errorMessage );
        void onGpxReadCancelled();
    }

    private static final String LOG_TAG = "GpxFileParser";

    private final ContentResolver contentResolver;
    private final Uri fileUri;
    private final Listener listener;

    private Metadata metadata = null;
    private List<GlobalPosition> trackpoints = new ArrayList<>();
    private List<NamedGlobalPosition> waypoints = new ArrayList<>();
    private Exception error = null;

    GpxFileParser(@NonNull ContentResolver contentResolver, @NonNull Uri fileUri, @NonNull Listener listener )
    {
        this.contentResolver = contentResolver;
        this.fileUri = fileUri;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground( Void... nothing )
    {
        long startTime = System.currentTimeMillis();

        try
        {
            XmlPullParser xml = XmlPullParserFactory
                    .newInstance()
                    .newPullParser();

            InputStream xmlStream = contentResolver.openInputStream(fileUri);
            xml.setInput( xmlStream, null);

            int eventType = xml.getEventType();

            while ( !isCancelled() && eventType != XmlPullParser.END_DOCUMENT )
            {
                String currentTag = xml.getName();

                if ( currentTag != null && eventType == XmlPullParser.START_TAG )
                {
                    if ( currentTag.compareTo(Metadata.GPX_TAG) == 0 )
                    {
                        XmlUtils.XmlObject xmlObject = readNext(xml);
                        metadata = Metadata.buildFromGpx(xmlObject);
                    }
                    else if ( currentTag.compareTo(Trackpoint.GPX_TAG) == 0 )
                    {
                        XmlUtils.XmlObject xmlObject = readNext(xml);
                        trackpoints.add( GlobalPosition.buildFromGpx(xmlObject) );
                    }
                    else if ( currentTag.compareTo(Waypoint.GPX_TAG) == 0 )
                    {
                        XmlUtils.XmlObject xmlObject = readNext(xml);
                        waypoints.add( NamedGlobalPosition.buildFromGpx(xmlObject) );
                    }
                }

                eventType = xml.next();
            }
        }
        catch( Exception error )
        {
            this.error = error;
        }

        long endTime = System.currentTimeMillis();

        Log.i( LOG_TAG, String.format("read %d trackpoints in %dms",
                trackpoints.size(), endTime-startTime ) );

        return null;
    }

    @Override
    protected void onPostExecute( Void nothing )
    {
        if ( error != null ) listener.onGpxReadError( error.getMessage() );
        else if ( metadata != null ) listener.onGpxFileRead( metadata, trackpoints, waypoints );
    }

    @Override
    protected void onCancelled()
    {
        Log.w( LOG_TAG, "task cancelled" );
        listener.onGpxReadCancelled();
    }
}
