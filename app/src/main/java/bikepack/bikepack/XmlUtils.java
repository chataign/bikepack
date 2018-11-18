package bikepack.bikepack;

import android.content.ContentResolver;
import android.net.Uri;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

class XmlUtils
{
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static class XmlAttribute
    {
        final String name;
        final String value;

        XmlAttribute( String name, String value )
        {
            this.name = name;
            this.value = value;
        }
    }

    static class XmlObject
    {
        final String tag;
        String value="";
        final List<XmlAttribute> attributes = new ArrayList<>();
        final List<XmlObject> children = new ArrayList<>();

        XmlObject( String tag ) { this.tag = tag; }

        XmlAttribute getAttribute( String name ) throws NoSuchFieldException
        {
            for ( XmlAttribute attribute : attributes )
                if ( attribute.name.compareTo(name) == 0 ) return attribute;

            throw new NoSuchFieldException("no XML attribute with name="+name);
        }

        XmlObject getChild( String tag ) throws NoSuchFieldException
        {
            for ( XmlObject child: children )
                if ( child.tag.compareTo(tag) == 0 ) return child;

            throw new NoSuchFieldException("no XML child with tag="+tag);
        }
    }

    static XmlObject readNext( XmlPullParser xml ) throws XmlPullParserException, IOException
    {
        XmlObject object = null;
        int eventType = xml.getEventType();

        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            String tag = xml.getName();

            if ( eventType == XmlPullParser.START_TAG && object == null )
            {
                object = new XmlObject(tag);

                for ( int i=0; i< xml.getAttributeCount(); ++i )
                    object.attributes.add( new XmlAttribute( xml.getAttributeName(i), xml.getAttributeValue(i) ) );
            }
            else if ( eventType == XmlPullParser.TEXT && object != null )
            {
                object.value = xml.getText();
            }
            else if ( eventType == XmlPullParser.END_TAG && object != null && tag.compareTo(object.tag) == 0 )
            {
                break;
            }
            else if ( eventType == XmlPullParser.START_TAG )
            {
                XmlObject child = readNext(xml);
                if ( child != null ) object.children.add(child);
            }

            eventType = xml.next();
        }

        return object;
    }

    static private XmlObject readNext( XmlPullParser xml, String tag )
            throws IOException, XmlPullParserException
    {
        int eventType = xml.getEventType();

        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            String current_tag = xml.getName();

            if ( eventType == XmlPullParser.START_TAG && current_tag.compareTo(tag) == 0 )
                return readNext(xml);

            eventType = xml.next();
        }

        return null;
    }

    static XmlObject readFirst( ContentResolver contentResolver, Uri uri, String tag )
            throws IOException, XmlPullParserException
    {
        XmlPullParser xml = XmlPullParserFactory
                .newInstance()
                .newPullParser();

        InputStream xmlStream = contentResolver.openInputStream(uri);
        xml.setInput( xmlStream, null);

        return readNext( xml, tag );
    }

    static List<XmlObject> readAll( ContentResolver content_resolver, Uri uri, String tag )
            throws XmlPullParserException, IOException
    {
        XmlPullParser xml = XmlPullParserFactory
                .newInstance()
                .newPullParser();
        
        InputStream xmlStream = content_resolver.openInputStream(uri);
        xml.setInput( xmlStream, null);

        List<XmlObject> objects = new ArrayList<>();
        int eventType = xml.getEventType();

        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            String currentTag = xml.getName();

            if ( eventType == XmlPullParser.START_TAG && currentTag.compareTo(tag) == 0 )
                objects.add( readNext(xml) );

            eventType = xml.next();
        }

        return objects;
    }
}
