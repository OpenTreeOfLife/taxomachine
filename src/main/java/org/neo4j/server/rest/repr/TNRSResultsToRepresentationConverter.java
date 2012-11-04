package org.neo4j.server.rest.repr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;

import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;

import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.MappingRepresentation;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.RepresentationType;
import org.neo4j.server.rest.repr.TNRSNameResultRepresentation;
import org.neo4j.server.rest.repr.ValueRepresentation;

public class TNRSResultsToRepresentationConverter {

    public static Representation convert(final TNRSResults results)
    {

        // if ( data instanceof Table ) { return new GremlinTableRepresentation( (Table) data ); }
        // if (data instanceof Iterable) {
        // return getListRepresentation((Iterable) data);
        // }
        // if (data instanceof Iterator) {
        // Iterator iterator = (Iterator) data;
        // return getIteratorRepresentation(iterator);
        // }

        return getListRepresentation(results);

        // if ( data instanceof Map ) { return getMapRepresentation( (Map) data ); }

        // return getSingleRepresentation(data);

        /*
         * HashMap<String, MappingRepresentation> testdataSet = new HashMap<String, MappingRepresentation>(); HashMap<String, String> testdataEntry = new
         * HashMap<String, String>();
         * 
         * testdataEntry.put("testkey0", "testval"); testdataEntry.put("testkey1", "testval"); testdataEntry.put("testkey2", "testval");
         * 
         * testdataSet.put("item0", getMapRepresentation(testdataEntry));
         */

        // return getMapRepresentation((Map) data);
        // can I create a class that extends mappingRepresentation?

    }

    public static MappingRepresentation getMapRepresentation(Map data) {
        return MappingRepresentation.stringMap(RepresentationType.MAP.toString(), data);
    }

    public static MappingRepresentation getTNRSNameResultRepresentation(Map<String,Object> data) {
      return TNRSNameResultRepresentation.map(data);
    }
    
/*    public static MappingRepresentation getTNRSMatchRepresentation(TNRSMatch match) {
        return TNRSRepresentation.mapMatch(match);
    } */
      
    static Representation getIteratorRepresentation(Iterator data)
    {
        final FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
                new IteratorWrapper<Representation, Object>(data) {
                    @Override
                    protected Representation underlyingObjectToObject(Object value) {
                        if (value instanceof Iterable)
                        {
                            FirstItemIterable<Representation> nested = convertValuesToRepresentations((Iterable) value);
                            return new ListRepresentation(getType(nested), nested);
                        } else {
                            return getSingleRepresentation(value);
                        }
                    }
                }
                );
        return new ListRepresentation(getType(results), results);
    }

    public static ListRepresentation getListRepresentation(Iterable data)
    {
        final FirstItemIterable<Representation> results = convertValuesToRepresentations(data);
        return new ListRepresentation(getType(results), results);
    }

    static FirstItemIterable<Representation> convertValuesToRepresentations(Iterable data)
    {
        /*
         * if ( data instanceof Table ) { return new FirstItemIterable<Representation>(Collections.<Representation>singleton(new GremlinTableRepresentation(
         * (Table) data ))); }
         */
        return new FirstItemIterable<Representation>(
                new IterableWrapper<Representation, Object>(data) {
                    @Override
                    protected Representation underlyingObjectToObject(Object value) {
                        if (value instanceof Iterable) {
                            final FirstItemIterable<Representation> nested = convertValuesToRepresentations((Iterable) value);
                            return new ListRepresentation(getType(nested), nested);
                        }
                        else {
                            return getSingleRepresentation(value);
                        }
                    }
                });
    }

    static RepresentationType getType(FirstItemIterable<Representation> representations)
    {
        Representation representation = representations.getFirst();
        if (representation == null)
            return RepresentationType.STRING;
        return representation.getRepresentationType();
    }

    /*
     * static Representation getTNRSRepresentation(Object data) {
     * 
     * Representation repr; return repr; }
     */

    static Representation getSingleRepresentation(Object result)
    {
        if (result == null)
            return ValueRepresentation.string("null");
        /*
         * if ( result instanceof Neo4jVertex ) { return new NodeRepresentation( ( (Neo4jVertex) result ).getRawVertex() ); } else if ( result instanceof
         * Neo4jEdge ) { return new RelationshipRepresentation( ( (Neo4jEdge) result ).getRawEdge() ); } else if ( result instanceof GraphDatabaseService ) {
         * return new DatabaseRepresentation( ( (GraphDatabaseService) result ) ); } else if ( result instanceof Node ) { return new NodeRepresentation( (Node)
         * result ); } else if ( result instanceof Relationship ) { return new RelationshipRepresentation( (Relationship) result ); } else if ( result
         * instanceof Neo4jGraph ) { return ValueRepresentation.string( ( (Neo4jGraph) result ).getRawGraph().toString() ); }
         */
        else if (result instanceof Double || result instanceof Float) {
            return ValueRepresentation.number(((Number) result).doubleValue());
        }
        else if (result instanceof Long) {
            return ValueRepresentation.number(((Long) result).longValue());
        } else if (result instanceof Integer) {
            return ValueRepresentation.number(((Integer) result).intValue());
        } else if (result instanceof TNRSNameResult) {
            HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
            nameResultMap.put("queried_name", ((TNRSNameResult) result).getQueriedName());
            nameResultMap.put("matches", ((TNRSNameResult) result).getMatches());
            return getTNRSNameResultRepresentation(nameResultMap);
        } else {
            return ValueRepresentation.string(result.toString());
        }
    }
}