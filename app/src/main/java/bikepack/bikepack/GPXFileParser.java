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

public class GPXFileParser extends AsyncTask< Void, Void, Void >
{
    interface Listener
    {
        void onGpxFileRead( Metadata metadata, List<GlobalPosition> trackpoints );
        void onGpxReadError( String errorMessage );
        void onGpxReadCancelled();
    }

    private static final String LOG_TAG = "GPXFileParser";

    private final ContentResolver contentResolver;
    private final Uri fileUri;
    private final Listener listener;

    private Metadata metadata = null;
    private List<GlobalPosition> trackpoints = new ArrayList<>();
    private Exception error = null;

    GPXFileParser( @NonNull ContentResolver contentResolver, @NonNull Uri fileUri, @NonNull Listener listener )
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
                        metadata = Metadata.buildFromXml(xmlObject);
                    }
                    else if ( currentTag.compareTo(Trackpoint.GPX_TAG) == 0 )
                    {
                        XmlUtils.XmlObject xmlObject = readNext(xml);
                        trackpoints.add( GlobalPosition.buildFromXml(xmlObject) );
                    }
                    else if ( currentTag.compareTo(Waypoint.GPX_TAG) == 0 )
                    {
                        // TODO
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
    protected void onPreExecute()
    {
    }

    @Override
    protected void onPostExecute( Void nothing )
    {
        if ( error != null ) listener.onGpxReadError( error.getMessage() );
        else if ( metadata != null ) listener.onGpxFileRead( metadata, trackpoints );
    }

    @Override
    protected void onCancelled()
    {
        Log.w( LOG_TAG, "task cancelled" );
        listener.onGpxReadCancelled();
    }
}
