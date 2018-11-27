package bikepack.bikepack;

import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Metadata
{
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static final String GPX_TAG = "metadata";

    public final String routeName;
    public final String authorName;
    public final String authorLink;
    public final Date dateCreated;

    // TODO handle empty/null fields

    Metadata(@NonNull String routeName, @NonNull String authorName, String authorLink, @NonNull Date dateCreated )
    {
        this.routeName = routeName;
        this.authorName = authorName;
        this.authorLink = authorLink;
        this.dateCreated = dateCreated;
    }

    // <metadata>
    // <name>Wikiloc - Transpirinaica 2015 BTT</name>
    // <author>
    //      <name>Albert Milià</name>
    //      <link href="https://www.wikiloc.com/wikiloc/user.do?id=195645">
    //          <text>Albert Milià on Wikiloc</text>
    //      </link>
    // </author>
    // <link href="https://www.wikiloc.com/mountain-biking-trails/transpirinaica-2015-btt-10182904">
    //      <text>Transpirinaica 2015 BTT on Wikiloc</text>
    // </link><time>2015-07-03T13:10:28Z</time>
    // </metadata>

    static Metadata buildFromGpx( XmlUtils.XmlObject xml )
            throws Exception
    {
        if ( !xml.tag.equals(Metadata.GPX_TAG) )
            throw new Exception("invalid metadata tag="+xml.tag);

        XmlUtils.XmlObject authorXml = xml.getChild("author");

        String routeName = xml.getChild("name").value;
        String authorName = authorXml.getChild("name").value;
        String authorLink = authorXml.getChild("link").getAttribute("href").value;
        Date dateCreated = DATE_FORMAT.parse( xml.getChild("time").value );

        return new Metadata( routeName, authorName, authorLink, dateCreated );
    }
}
