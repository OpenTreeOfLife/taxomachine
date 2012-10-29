package org.neo4j.server.rest.repr;

import java.net.URI;

public class TNRSResultsSerializer extends MappingSerializer {

    TNRSResultsSerializer(MappingWriter writer, URI baseUri, ExtensionInjector extensions) {
        super(writer, baseUri, extensions);
    }

    public void putTNRSMatch( String key, MappingRepresentation value )
    {
        serialize( writer.newMapping( value.type, key ), value );
    }
}
