package bikepack.bikepack;

import android.support.annotation.NonNull;

import java.text.ParseException;
import java.util.Date;

class Metadata
{
    static final String GPX_TAG = "metadata";

    final String routeName;
    final String authorName;
    final String authorLink;
    final Date dateCreated;

    // TODO handle empty/null fields

    Metadata(@NonNull String routeName, @NonNull String authorName, String authorLink, @NonNull Date dateCreated )
    {
        this.routeName = routeName;
        this.authorName = authorName;
        this.authorLink = authorLink;
        this.dateCreated = dateCreated;
    }

    static Metadata buildFromXml( XmlUtils.XmlObject xml )
            throws NoSuchFieldException, NumberFormatException, ParseException
    {
        XmlUtils.XmlObject authorXml = xml.getChild("author");

        String routeName = xml.getChild("name").value;
        String authorName = authorXml.getChild("name").value;
        String authorLink = authorXml.getChild("link").getAttribute("href").value;
        Date dateCreated = XmlUtils.DATE_FORMAT.parse( xml.getChild("time").value );

        return new Metadata( routeName, authorName, authorLink, dateCreated );
    }
}
