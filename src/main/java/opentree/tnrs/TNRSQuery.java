package opentree.tnrs;

import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import opentree.taxonomy.TaxonomyExplorer;
import opentree.tnrs.adaptersupport.gnr.*;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Node;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * @author Cody Hinchliff
 * 
 * Provides methods for performing TNRS queries given a search string according
 * to various options.
 */

public class TNRSQuery {

    public final TNRSAdapterGNR GNR_ADAPTER = new TNRSAdapterGNR();
    
    private String _graphName;
    private TaxonomyExplorer _taxonomy;
    
    public TNRSQuery(String graphName) {
        _graphName = graphName;
        _taxonomy = new TaxonomyExplorer(_graphName);
    }
    
    /**
     * @param searchString
     * @param adapters
     * @return results
     * 
     * Performs a TNRS query on 'searchString', and loads the results of the query
     * into a TNRSMatchSet object called 'results', which is returned. 'adapters'
     * is an array of TNRSAdapter objects designating the sources which should be
     * queried. If no adapters are passed, then the query is just performed against
     * the local taxonomy graph.
     */
    public TNRSMatchSet getMatches(String[] searchStrings, TNRSAdapter...adapters) {
        TNRSMatchSet results = new TNRSMatchSet(_graphName);
        ArrayList<String> unmatchedNames = new ArrayList<String>();
        
        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];
            Node matchedNode = _taxonomy.findTaxNodeByName(thisName);
            
            // first: check the graph index (simple)
            if (matchedNode != null) {
                results.addDirectMatch(thisName, matchedNode);
                continue;
            }
            
            // second: direct matches to synonyms within the graph (need to interface with stephen)
            // third: fuzzy matching within the graph (could be fuzzy matches to junior synonyms or recognized taxa)
            
            // if we can't find a match in the graph, then look elsewhere
            unmatchedNames.add(thisName);
        }
        
        // fourth: whatever adapters are desired
        
        for(int i=0; i < adapters.length; i++) {
            adapters[i].doQuery(unmatchedNames);
        }
        
        return results;
    }
    
    public class TNRSAdapterGNR extends TNRSAdapter {
        
        public void doQuery(ArrayList<String> searchStrings) {
            int id = 0; // just a placeholder

            // build the query
            String queryString = "{\"data\":\"";
            boolean isFirst = true;
            for (String s : searchStrings) {
                if (isFirst)
                    isFirst = false;
                else
                    queryString += "\n";

                queryString += id + "|" + s;
            }
            queryString += "\"}";
    
//            System.out.println(queryString);
            
            // set up the connection to GNR
            ClientConfig cc = new DefaultClientConfig();
            Client c = Client.create(cc);
            WebResource gnr = c.resource("http://resolver.globalnames.org/name_resolvers.json");

            // send the query (get the response)
            String respJSON = gnr.accept(
                    MediaType.APPLICATION_JSON_TYPE).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    post(String.class, queryString);
            
//            System.out.println(respJSON);

            // parse the JSON response
            GNRResponse response = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                response = mapper.readValue(respJSON, GNRResponse.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            for (NameResult thisNameResult : response) {
                System.out.println(thisNameResult.supplied_name_string);
                for (GNRMatch match : thisNameResult) {
                    Node matchedNode = _taxonomy.findTaxNodeByName(match.canonical_form);
                    if (matchedNode != null) {
                        
                    }
                }
            }

        }
    }
}
