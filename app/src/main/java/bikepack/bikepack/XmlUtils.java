package bikepack.bikepack;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

class XmlUtils
{
    static class XmlAttribute
    {
        final String name;
        final String value;

        XmlAttribute( String name, String value )
        {
            this.name = name;
            this.value = value;
        }

        XmlAttribute( XmlAttribute attribute )
        {
            this( attribute.name, attribute.value );
        }

        @Override
        public  String toString()
        {
            return String.format("{ XmlAttribute[%s]=%s }", name, value );
        }
    }

    static class XmlObject
    {
        final String tag;
        String value=null;

        private final HashMap<String,XmlAttribute> attributes = new HashMap<>();
        private final HashMap<String,XmlObject> children = new HashMap<>();

        XmlObject( String tag )
        {
            this.tag = tag;
        }

        void addAttribute( XmlAttribute attribute )
        {
            if ( attribute == null ) return;
            attributes.put( attribute.name, new XmlAttribute(attribute) );
        }

        XmlAttribute getAttribute( String name ) throws NoSuchFieldException
        {
            XmlAttribute attribute = attributes.get(name);
            if ( attribute != null ) return attribute;
            throw new NoSuchFieldException("XML object="+tag+" has no attribute named="+name);
        }

        void addChild( XmlObject child )
        {
            if ( child == null ) return;
            children.put( child.tag, child );
        }

        XmlObject getChild( String tag ) throws NoSuchFieldException
        {
            XmlObject child = children.get(tag);
            if ( child != null ) return child;
            throw new NoSuchFieldException("XML object="+this.tag+" has no child with tag="+tag);
        }

        String toString( int numTabs )
        {
            String str = "\n";
            for ( int i=0; i< numTabs; ++i ) str += "> ";
            str += String.format("XmlObject[%s]=%s", tag, value );
            for ( XmlAttribute attribute : attributes.values() ) str += attribute.toString();
            for ( XmlObject child : children.values() ) str += child.toString(numTabs+1);
            return str;
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
                    object.addAttribute( new XmlAttribute( xml.getAttributeName(i), xml.getAttributeValue(i) ) );
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
                if ( child != null ) object.addChild(child);
            }

            eventType = xml.next();
        }

        return object;
    }
}
