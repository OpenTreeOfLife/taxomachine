package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IteratorWrapper;

import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;

public class TNRSResultsRepresentation extends MappingRepresentation {

    public TNRSResultsRepresentation(RepresentationType type) {
        super(type);
    }

    TNRSResultsRepresentation(String type) {
        super(type);
    }
    
    @Override
    String serialize( RepresentationFormat format, URI baseUri, ExtensionInjector extensions ) {
        MappingWriter writer = format.serializeMapping( type );
        Serializer.injectExtensions( writer, this, baseUri, extensions );
        serialize( new MappingSerializer( writer, baseUri, extensions ) );
        writer.done();
        return format.complete( writer );
    }
    
    @Override
    void addTo( ListSerializer serializer ) {
        serializer.addMapping( this );
    }

    @Override
    void putTo( MappingSerializer serializer, String key ) {
        serializer.putMapping( key, this );
    }

    @Override
    protected void serialize(MappingSerializer serializer) {        
    }
    
    public static TNRSResultsRepresentation map(final Map<String,Object> map) {
        return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {
            @Override
            protected void serialize( final MappingSerializer serializer ) {

                for(Map.Entry<String,Object> pair : map.entrySet()) {
                    String key = pair.getKey();
                    Object value = pair.getValue();
                    
                    if (value instanceof Boolean) {
                        serializer.putBoolean(key, (Boolean) value);
                    } else if (value instanceof String) {
                        serializer.putString(key, (String) value);
                    } else if (value instanceof Map) {
                        serializer.putMapping(key, (MappingRepresentation) value);
                    } else if (value instanceof List) {
                        serializer.putList(key, (ListRepresentation) value);
                    } else if (value instanceof Float || value instanceof Double || value instanceof Long || value instanceof Integer) {
                        serializer.putNumber(key, (Number) value);
                    } else if (value instanceof TNRSMatchSet) {
                        serializer.putList(key, getMatchSet((TNRSMatchSet)value));
                    } else if (value instanceof TNRSMatch) {
                        serializer.putMapping(key, mapMatch((TNRSMatch)value));
                    }
                }
            }
        };
    }

    public static ListRepresentation getMatchSet(final TNRSMatchSet matchSet) {

        FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
                new IteratorWrapper<Representation, Object>((Iterator)matchSet.iterator()) {
                    @Override
                    protected Representation underlyingObjectToObject(Object value) {
                        return mapMatch((TNRSMatch)value);
                    }
                }
                );
        return new ListRepresentation(RepresentationType.PROPERTIES, results);
    }
    
    public static MappingRepresentation mapMatch(final TNRSMatch match) {

        return new MappingRepresentation(RepresentationType.MAP.toString()) {
            @Override
            protected void serialize( final MappingSerializer serializer ) {
    
                // should also have matchedNodeUniqueId, but this is not yet available
                serializer.putNumber("matchedNodeId", match.getMatchedNode().getId());
                serializer.putString("matchedNodeName", match.getMatchedNode().getProperty("name").toString());
                serializer.putString("sourceName", match.getSource());
                serializer.putBoolean("isPerfectMatch", match.getIsPerfectMatch());
                serializer.putBoolean("isApprox", match.getIsApproximate());
                serializer.putBoolean("isSynonym", match.getIsSynonym());
                serializer.putBoolean("isHomonym", match.getIsHomonym());
                serializer.putString("searchString", match.getSearchString());
                serializer.putNumber("score", match.getScore());

                if (match.getIsSynonym()) {
                    serializer.putNumber("synonymNodeId", match.getSynonymNode().getId());
                    serializer.putString("synonymNodeName", match.getSynonymNode().getProperty("name").toString());
                }
            }
        };
    }
}
