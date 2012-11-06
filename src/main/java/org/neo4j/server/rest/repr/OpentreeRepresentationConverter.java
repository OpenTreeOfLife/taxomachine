package org.neo4j.server.rest.repr;

import java.util.Iterator;
import java.util.Map;

import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;

import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;

import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.MappingRepresentation;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.RepresentationType;
import org.neo4j.server.rest.repr.ValueRepresentation;

public class OpentreeRepresentationConverter {

    public static Representation convert(final Object data)
    {

        // if ( data instanceof Table ) { return new GremlinTableRepresentation( (Table) data ); }

        // TNRSResults are iterable, but need special attention, so only check for iterable if it isn't TNRSResults
        if (data instanceof TNRSResults) {
            return getTNRSResultsRepresentation((TNRSResults) data);

        } else if (data instanceof Iterable) {
                return getListRepresentation((Iterable) data);
        
        }
        
        if (data instanceof Iterator) {
            Iterator iterator = (Iterator) data;
            return getIteratorRepresentation(iterator);
        }
        
        if (data instanceof Map) {
            return getMapRepresentation( (Map) data );
        }

        return getSingleRepresentation(data);

    }

    public static MappingRepresentation getMapRepresentation(Map data) {
        return MappingRepresentation.stringMap(RepresentationType.MAP.toString(), data);
    }

    public static MappingRepresentation getTNRSNameResultRepresentation(TNRSNameResult nameResult) {
      return TNRSResultsRepresentation.getNameResultRepresentation(nameResult);
    }

    public static Representation getTNRSResultsRepresentation(TNRSResults results) {
        return TNRSResultsRepresentation.getResultsRepresentation(results);
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

                        if (value instanceof TNRSNameResult) {
                            return getTNRSNameResultRepresentation((TNRSNameResult)value);
 
                        } else if (value instanceof Iterable) {
                            final FirstItemIterable<Representation> nested = convertValuesToRepresentations((Iterable) value);
                            return new ListRepresentation(getType(nested), nested);
                        
                        } else {
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

    static Representation getSingleRepresentation(Object data)
    {
        if (data == null) {
            return ValueRepresentation.string("null");
        /*
         * if ( result instanceof Neo4jVertex ) { return new NodeRepresentation( ( (Neo4jVertex) result ).getRawVertex() ); } else if ( result instanceof
         * Neo4jEdge ) { return new RelationshipRepresentation( ( (Neo4jEdge) result ).getRawEdge() ); } else if ( result instanceof GraphDatabaseService ) {
         * return new DatabaseRepresentation( ( (GraphDatabaseService) result ) ); } else if ( result instanceof Node ) { return new NodeRepresentation( (Node)
         * result ); } else if ( result instanceof Relationship ) { return new RelationshipRepresentation( (Relationship) result ); } else if ( result
         * instanceof Neo4jGraph ) { return ValueRepresentation.string( ( (Neo4jGraph) result ).getRawGraph().toString() ); }
         */
        
        } else if (data instanceof TNRSNameResult) {
            return getTNRSNameResultRepresentation((TNRSNameResult) data);
    
        } else if (data instanceof TNRSMatchSet) {
            return getListRepresentation((TNRSMatchSet) data);
        
        } else if (data instanceof Double || data instanceof Float) {
            return ValueRepresentation.number(((Number) data).doubleValue());
        
        } else if (data instanceof Long) {
            return ValueRepresentation.number(((Long) data).longValue());

        } else if (data instanceof Integer) {
            return ValueRepresentation.number(((Integer) data).intValue());
           
        } else {
            return ValueRepresentation.string(data.toString());
        }
    }
}