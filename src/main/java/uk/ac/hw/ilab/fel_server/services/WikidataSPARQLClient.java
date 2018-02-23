package uk.ac.hw.ilab.fel_server.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.jena.query.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WikidataSPARQLClient {
    private static final String RETRIEVE_WIKIPEDIA_PAGE = "PREFIX schema: <http://schema.org/>\n" +
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "\n" +
            "SELECT ?article WHERE {\n" +
            "    ?article schema:about <%s> .\n" +
            "    ?article schema:inLanguage \"en\" .\n" +
            "    FILTER(REGEX(str(?article), \"^https://en.wikipedia.org/wiki/.*\"))\n" +
            "} ";

    private static final String RETRIEVE_WIKIDATA_URI = "PREFIX schema: <http://schema.org/>\n" +
            "\n" +
            "SELECT ?wikidata_uri WHERE {\n" +
            "    ?article schema:about ?wikidata_uri .\n" +
            "    ?article schema:inLanguage \"en\" .\n" +
            "    VALUES ?article {<%s>}\n" +
            "} ";
    private static final String RETRIEVE_ENTITY_TYPES = " PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "SELECT ?type WHERE {\n" +
            "    <%s> wdt:P31 ?type\n" +
            "}";

    private static final String RETRIEVE_ENTITY_PROPS = "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "SELECT ?p ?o WHERE {\n" +
            "    <%s> ?p ?o\n" +
            " VALUES ?p { %s }\n" +
            "}";

    @Value("${fel_server.wikidata_endpoint}")
    private String wikidataEndpoint;

    public String getWikidataURI(String wikipediaId) throws UnsupportedEncodingException {
        String wikipediaURI = String.format("https://en.wikipedia.org/wiki/%s", wikipediaId);
        wikipediaURI = java.net.URLDecoder.decode(wikipediaURI, "UTF-8");
        Query query = QueryFactory.create(
                String.format(RETRIEVE_WIKIDATA_URI, wikipediaURI)
        );

        String wikidataURI = null;

        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(wikidataEndpoint, query)) {
            ResultSet resultSet = qExec.execSelect();

            while (resultSet.hasNext()) {
                wikidataURI = resultSet.nextSolution().getResource("wikidata_uri").getURI();
            }
        }

        return wikidataURI;
    }

    public List<String> getEntityTypes(String entityURI) throws UnsupportedEncodingException {
        Query query = QueryFactory.create(
                String.format(RETRIEVE_ENTITY_TYPES, entityURI)
        );

        List<String> entityTypes = new ArrayList<>();

        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(wikidataEndpoint, query)) {
            ResultSet resultSet = qExec.execSelect();
            String type;

            while (resultSet.hasNext()) {
                type = resultSet.nextSolution().getResource("type").getURI();
                entityTypes.add(type);
            }
        }

        return entityTypes;
    }

    public Multimap<String, String> getEntityProps(String entityURI, Collection<String> properties) {
        String propertiesList = String.join(" ", properties.stream().map(p -> String.format("<%s>", p)).collect(Collectors.toList()));
        Query query = QueryFactory.create(
                String.format(
                        RETRIEVE_ENTITY_PROPS,
                        entityURI,
                        propertiesList
                )
        );

        Multimap<String, String> entityProps = HashMultimap.create();

        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(wikidataEndpoint, query)) {
            ResultSet resultSet = qExec.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution querySolution = resultSet.nextSolution();

                String prop = querySolution.getResource("p").getURI(),
                        obj = querySolution.getResource("o").getURI();

                entityProps.put(prop, obj);
            }
        }

        return entityProps;
    }
}
