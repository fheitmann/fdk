package no.dcat.portal.webapp;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class PortalRestController {

    private final static Logger logger = LoggerFactory.getLogger(PortalRestController.class);

    @Value("${application.fusekiService}")
    private String fusekiService;

    protected String getFusekiService() {
        return fusekiService;
    }

    /**
     * API to find dataset based on id from .../dataset?id={id}
     *
     * @param id           of dataset to find
     * @param acceptHeader accepted format
     * @return Formatted response based on acceptHeader {@link SupportedFormat}
     */
    @CrossOrigin
    @RequestMapping(value = "/catalogs", params={"id","format"},
            method = GET,
            consumes = MediaType.ALL_VALUE,
            produces = {"text/turtle", "application/ld+json", "application/rdf+xml"})
    public ResponseEntity<String> getCatalogDcat(
            @RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "format", required = false) String format,
            @RequestHeader(value = "Accept") String acceptHeader) {

        final String queryFile = "sparql/catalog.sparql";

        ResponseEntity<String> responseBody = invokeFusekiQuery(id, format, acceptHeader, queryFile);

        if (responseBody != null) return responseBody;

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @CrossOrigin
    @RequestMapping(value = "/catalogs",
        method = GET,
        produces = "text/html")
    public String getCatalogs(@RequestParam(value= "x", defaultValue="x") String x,
                              @RequestHeader(value = "Accept", defaultValue = "*/*") String acceptHeader) {
        final String queryFile = "sparql/allcatalogs.sparql";

        try {
            Resource resource = new ClassPathResource(queryFile);
            String queryString = read(resource.getInputStream());

            Query query = getQuery(queryString);

            try (QueryExecution qe = getQueryExecution(query)) {
                ResultSet resultset = qe.execSelect();

                StringBuilder builder = new StringBuilder();

                while (resultset.hasNext()) {
                    QuerySolution qs = resultset.next();
                    String catalogName = qs.get("cname").asLiteral().getString();
                    String catalogUri = qs.get("catalog").asResource().getURI();
                    String publisherName = qs.get("pname").asLiteral().getString();
                    String publisherUri = qs.get("publisher").asResource().getURI();

                    builder.append("<a href='"+ "http://localhost:8080/catalogs?id=" + catalogUri + "&format=ttl'>" + catalogName + "</a>\n" );
                }

                return builder.toString();
            }


        } catch (IOException e) {

        }

        return null;
    }

    /**
     * API to find dataset based on id from .../dataset?id={id}
     *
     * @param id           of dataset to find
     * @param acceptHeader accepted format
     * @return Formatted response based on acceptHeader {@link SupportedFormat}
     */
    @CrossOrigin
    @RequestMapping(value = "/dataset",
            method = GET,
            consumes = MediaType.ALL_VALUE,
            produces = {"text/turtle", "application/ld+json", "application/rdf+xml"})
    public ResponseEntity<String> getDatasetDcat(
            @RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "format", required = false) String format,
            @RequestHeader(value = "Accept", defaultValue = "*/*") String acceptHeader) {

        final String queryFile = "sparql/dataset.sparql";

        ResponseEntity<String> responseBody = invokeFusekiQuery(id, format, acceptHeader, queryFile);

        if (responseBody != null) return responseBody;

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Invokes a query to Fuseki and returns the reslut as a response entity
     *
     * @param id           the resource to look for
     * @param format       the format to return (can be null)
     * @param acceptHeader the format to return (can be null)
     * @param queryFile    the sparql query to perform
     * @return a responseEntity with the RDF formated according to the format.
     */
    ResponseEntity<String> invokeFusekiQuery(String id, String format, String acceptHeader, String queryFile) {
        try {
            logger.info("Export {}", id);

            Resource resource = new ClassPathResource(queryFile);
            String query = read(resource.getInputStream());

            String returnFormat = getReturnFormat(acceptHeader, format);

            if (returnFormat == null) {
                return new ResponseEntity<>("Unknown format " + format + " and/or Accept-header: " + acceptHeader,
                        HttpStatus.NOT_ACCEPTABLE);
            }

            logger.info("Prepare export of {}", returnFormat);

            String responseBody = findResourceById(id, query, returnFormat);
            if (responseBody == null) {
                return new ResponseEntity<>("Unable to find " + id, HttpStatus.NOT_FOUND);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", returnFormat + "; charset=UTF-8");

            return new ResponseEntity<>(responseBody, headers, OK);

        } catch (IOException e) {
            logger.error("Unable to open sparql-file {}", queryFile, e);
        } catch (NoSuchElementException nsee) {
            logger.info("ID {} not found {}", id, nsee.getMessage(), nsee);
            return new ResponseEntity<>("Unable to find " + id, HttpStatus.NOT_FOUND);
        }

        return null;
    }

    /**
     * Convert inputstream to String (java 8)
     *
     * @param input the inputstream
     * @return the string
     * @throws IOException if anything is wrong
     */
    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Returns the iana format to return
     *
     * @param acceptFormat if Accept has a format we use it
     * @param fileFormat   if ?format=ttl has a format we use it
     * @return the returned format.
     */
    private String getReturnFormat(String acceptFormat, String fileFormat) {
        String modelFormat = null;

        if (acceptFormat != null) {
            if (acceptFormat.contains("json")) {
                modelFormat = "application/ld+json";
            } else if (acceptFormat.contains("rdf")) {
                modelFormat = "application/rdf+xml";
            } else if (acceptFormat.contains("turtle")) {
                modelFormat = "text/turtle";
            }
        }

        if (modelFormat == null && fileFormat != null) {
            if (fileFormat.toLowerCase().contains("xml") || fileFormat.toLowerCase().contains("rdf")) {
                modelFormat = "application/rdf+xml";
            } else if (fileFormat.toLowerCase().contains("json")) {
                modelFormat = "application/ld+json";
            } else if (fileFormat.toLowerCase().contains("ttl")) {
                modelFormat = "text/turtle";
            }
        }

        return modelFormat;
    }

    /**
     * Asks fuseki to do query and return the wanted format
     *
     * @param id          the ide of the resource to find
     * @param queryString the query which describe the result
     * @param format      the wanted format.
     * @return RDF formated string according to DCAT and format, if null the query returned empty
     */
    String findResourceById(String id, String queryString, String format) {
       //Model model = getModel();

        try {
            id = URLDecoder.decode(id, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            // Should in principle newer be thrown
            logger.error("URI syntax error ", id, e);
            return null;
        }

        Query query = getQuery(String.format(queryString, id));

        try ( QueryExecution qe = getQueryExecution(query))/*(QueryExecution qe = QueryExecutionFactory.create(query, model))*/ {

            logger.debug(query.toString());

            Model submodel = qe.execDescribe();

            if (submodel.isEmpty()) {
                return null;
            }
            ModelFormatter modelFormatter = new ModelFormatter(submodel);

            return modelFormatter.format(format);
        } catch (Exception e) {
            logger.error("FusekiQuery error",e);
        }

        return null;
    }

    Query getQuery(String query) {
        return QueryFactory.create(query);
    }

    QueryExecution getQueryExecution(Query query) {
        return new QueryEngineHTTP(getFusekiService() + "/dcat", query);
    }


}
