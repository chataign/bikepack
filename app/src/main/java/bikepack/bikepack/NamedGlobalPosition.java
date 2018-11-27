package bikepack.bikepack;

public class NamedGlobalPosition extends GlobalPosition
{
    final String name;
    final String description;

    NamedGlobalPosition( GlobalPosition position, String name, String description )
    {
        super( position );
        this.name = name;
        this.description = description;
    }

    static NamedGlobalPosition buildFromGpx(XmlUtils.XmlObject xml )
            throws NoSuchFieldException, NumberFormatException
    {
        GlobalPosition position = GlobalPosition.buildFromGpx(xml);
        String name = xml.getChild("name").value;
        String description = xml.getChild("desc").value;

        return new NamedGlobalPosition( position, name, description );
    }
}
