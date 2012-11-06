package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IteratorWrapper;

import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;

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
    
    public static TNRSResultsRepresentation getResultsRepresentation(final TNRSResults results) {
        return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {
            
            @Override
            protected void serialize( final MappingSerializer serializer ) {

                HashMap<String, Object> tnrsResultsMap = new HashMap<String, Object>();
                
                tnrsResultsMap.put("governing_code", results.getGoverningCode());
                tnrsResultsMap.put("unambiguous_names", results.getUnambiguousNames());
                tnrsResultsMap.put("unmatched_names", results.getUnmatchedNames());
                tnrsResultsMap.put("matched_names", results.getMatchedNames());
                
                for(Map.Entry<String,Object> pair : tnrsResultsMap.entrySet()) {
                    String key = pair.getKey();
                    Object value = pair.getValue();
                    
                    if (value instanceof String) {
                        serializer.putString(key, (String) value);

                    } else if (value instanceof Set) {
                        serializer.putList(key, OpentreeRepresentationConverter.getListRepresentation((Set)value));
                    }
                }
 
                serializer.putList("results", OpentreeRepresentationConverter.getListRepresentation(results));
                
//                        HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
//                        nameResultMap.put("queried_name", ((TNRSNameResult) value).getQueriedName());
//                        nameResultMap.put("matches", ((TNRSNameResult) value).getMatches());
//                        serializer.putList(results, nameResults)(nameResultMap));
/*                    } else if (value instanceof TNRSMatchSet) {
                        serializer.putList(key, getMatchSet((TNRSMatchSet)value));
                    } else if (value instanceof TNRSMatch) {
                        serializer.putMapping(key, mapMatch((TNRSMatch)value)); */
//                    }
            }
        };
    }
    
    public static MappingRepresentation getNameResultRepresentation(TNRSNameResult r) {
        
        final HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
        nameResultMap.put("queried_name", r.getQueriedName());
        nameResultMap.put("matches", r.getMatches());
        
        return new MappingRepresentation (RepresentationType.MAP.toString()) {
            @Override
            protected void serialize( final MappingSerializer serializer ) {

                for(Map.Entry<String,Object> pair : nameResultMap.entrySet()) {
                    String key = pair.getKey();
                    Object value = pair.getValue();
                    
//                    if (value instanceof Boolean) {
//                        serializer.putBoolean(key, (Boolean) value);
                    if (value instanceof String) {
                        serializer.putString(key, (String) value);
//                    } else if (value instanceof Map) {
//                       serializer.putMapping(key, (MappingRepresentation) value);
//                    } else if (value instanceof List) {
//                        serializer.putList(key, (ListRepresentation) value);
//                    } else if (value instanceof Float || value instanceof Double || value instanceof Long || value instanceof Integer) {
//                        serializer.putNumber(key, (Number) value);
                    } else if (value instanceof TNRSMatchSet) {
                        serializer.putList(key, getMatchSetRepresentation((TNRSMatchSet)value));
                    } else if (value instanceof TNRSMatch) {
                        serializer.putMapping(key, getMatchRepresentation((TNRSMatch)value));
                    }
                }
            }
        };
    }

    public static ListRepresentation getMatchSetRepresentation(final TNRSMatchSet matchSet) {

        FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
                new IteratorWrapper<Representation, Object>((Iterator)matchSet.iterator()) {
                    @Override
                    protected Representation underlyingObjectToObject(Object value) {
                        return getMatchRepresentation((TNRSMatch)value);
                    }
                }
                );
        return new ListRepresentation(RepresentationType.PROPERTIES, results);
    }
    
    public static MappingRepresentation getMatchRepresentation(final TNRSMatch match) {

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
