package opentree.tnrs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import opentree.taxonomy.TaxonomyExplorer;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.adaptersupport.gnr.*;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Node;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
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

    public final TNRSAdapterGNR GNR = new TNRSAdapterGNR();
    public final TNRSAdapterPhylotastic TAXOSAURUS = new TNRSAdapterPhylotastic();    
    
    private String _graphName;
    private TaxonomyExplorer _taxonomy;
    private TNRSMatchSet _results;
    private HashSet<String> _unmatchedNames;
    
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
        _results = new TNRSMatchSet(_graphName);
        _unmatchedNames = new HashSet<String>();
        
        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];
            Node matchedNode = _taxonomy.findTaxNodeByName(thisName);
            
            // first: check the graph index (simple)
            if (matchedNode != null) {
                _results.addMatch(new TNRSDirectMatch(thisName, matchedNode));
                continue;
            }
            
            // second: direct matches to synonyms within the graph (need to interface with stephen)
            // third: fuzzy matching within the graph (could be fuzzy matches to junior synonyms or recognized taxa)
            
            // remember names we can't match
            if (_unmatchedNames.contains(thisName) == false)
                _unmatchedNames.add(thisName);
        }

        System.out.println("checkpoint 1");

        // fourth: call passed adapters for help with names we couldn't match
        if (_unmatchedNames.size() > 0) {
            for(int i=0; i < adapters.length; i++) {
                adapters[i].doQuery(_unmatchedNames);
            }
        }
        return _results;
    }
    
    public HashSet<String> getUnmatchedNames() {
        return _unmatchedNames;
    }
    
    private class TNRSAdapterGNR extends TNRSAdapter {
        
        String url = "http://resolver.globalnames.org/name_resolvers.json";
        
        public void doQuery(HashSet<String> searchStrings) {
            int id = 0; // just a placeholder

            System.out.println("gnr checkpoint 1");

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
    
            System.out.println(queryString);
            
            // set up the connection to GNR
            ClientConfig cc = new DefaultClientConfig();
            Client c = Client.create(cc);
            WebResource gnr = c.resource(url);

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
            
            System.out.println("gnr checkpoint 2");
            
            for (NameResult thisNameResult : response) {
                System.out.println(thisNameResult.supplied_name_string);
                for (GNRMatch thisMatch : thisNameResult) {
                    String matchedName = thisMatch.canonical_form;
                    boolean thisNameMatched = false;
                    if (matchedName != null) {
                        Node matchedNode = _taxonomy.findTaxNodeByName(thisMatch.canonical_form);
                        if (matchedNode != null) {
                            thisNameMatched = true;
                            thisMatch.matchedNode = matchedNode;
                            thisMatch.searchString = thisNameResult.supplied_name_string;
                            _results.addMatch(thisMatch);
                        } else {
                            // check if it matches a synonym
                        }
                    }
                    if (thisNameMatched) {
                        for (String s : _unmatchedNames) {
                            if (s.compareTo(matchedName) == 0) {
                            _unmatchedNames.remove(s);
                            }
                        }
                    }
                }
            }
        }
    }

    private class TNRSAdapterPhylotastic extends TNRSAdapter {
        String submiturl = "http://taxosaurus/tnrs/submit?query=Plantago";
        String retrieveurl = "http://api.phylotastic.org/tnrs/retrieve/";
        
        public void doQuery(HashSet<String> searchStrings) {

            System.out.println("taxosaurus waypoint 1");

/*            // build the query
            String queryString = "{\"query\":\"";
            boolean isFirst = true;
            for (String s : searchStrings) {
                if (isFirst)
                    isFirst = false;
                else
                    queryString += "\n";

                queryString += s;
            }
            queryString += "\"}";
    
            System.out.println(queryString); */
            
            // set up the connection to GNR
            ClientConfig cc = new DefaultClientConfig();
            Client c = Client.create(cc);
            c.setFollowRedirects(true);
            WebResource taxosaurus = c.resource(submiturl);

/*            ClientResponse response = taxosaurus.get(ClientResponse.class);
//            EntityTag e = response.getEntityTag();
            String entity = response.getEntity(String.class);
*/            
            String respUri = null;
            try {
                String dummy = taxosaurus.get(String.class);
            } catch (UniformInterfaceException ue) {
                ClientResponse resp = ue.getResponse();
                respUri = resp.toString().split("\\s")[1];
//                System.out.println(respUri);
            }

            WebResource taxosaurusResp = c.resource(respUri);

            // send the query (get the response)
            String tokenJSON = taxosaurusResp.accept(
                    MediaType.APPLICATION_JSON_TYPE).
//                    type(MediaType.APPLICATION_JSON_TYPE).
                    get(String.class);

            System.out.println(tokenJSON);
            
            // retrieve the results using a GET request on the token to retrieveurl
        }
    }
}
