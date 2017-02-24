package com.adobe.acs.tools.csv_property_importer;

import com.adobe.acs.tools.csv.impl.CsvUtil;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@SlingServlet(
        label = "ACS AEM Tools - CSV Property Importer Servlet",
        methods = { "POST" },
        resourceTypes = { "acs-tools/components/csv-property-importer" },
        selectors = { "create" },
        extensions = { "json" }
)
public class CsvPropertyImporter extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(CsvPropertyImporter.class);
    
    private static final int DEFAULT_BATCH_SIZE = 1000;

    // 3 to account for Line Termination
    private static final int VALID_ROW_LENGTH = 3;
    
    @Override
    protected final void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException {
        log.info("Hello, starting property importer");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JSONObject jsonResponse = new JSONObject();
        final Parameters params = new Parameters(request);

        if (params.getFile() != null) {

            final long start = System.currentTimeMillis();
            final Iterator<String[]> rows = CsvUtil.getRowsFromCsv(params);

            try {
                request.getResourceResolver().adaptTo(Session.class).getWorkspace().getObservationManager().setUserData("acs-aem-tools.csv-resource-type-updater");

                final Result result = this.create(request.getResourceResolver(), params, rows);

                if (log.isInfoEnabled()) {
                    log.info("Updated as TOTAL of [ {} ] resources in {} ms", result.getSuccess().size(),
                            System.currentTimeMillis() - start);
                }

                try {
                    jsonResponse.put("success", result.getSuccess());
                    //jsonResponse.put("failure", result.getFailure());
                } catch (JSONException e) {
                    log.error("Could not serialized results into JSON", e);
                    this.addMessage(jsonResponse, "Could not serialized results into JSON");
                    response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e) {
                log.error("Could not process CSV type update replacement", e);
                this.addMessage(jsonResponse, "Could not process CSV type update. " + e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Could not find CSV file in request.");
            this.addMessage(jsonResponse, "CSV file is missing");
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().print(jsonResponse.toString());
    }
    
    
    //TODO: Write create method here!!!
    private Result create(final ResourceResolver resourceResolver, final Parameters params, final Iterator<String[]> rows) throws PersistenceException, RepositoryException {
        
        final Result result = new Result();

        log.info("Got the value of param: " + params.isCreateNewNode());
        
        Resource resource = resourceResolver.getResource(params.getPath());
        Node node = resource.adaptTo(Node.class);

        if (params.isCreateNewNode()) {
            Node childNode = createNewNode(node, params.getNewNodeName(), params.getNewNodeType());
            log.info("created new node: " + childNode.getPath());
            if (childNode == null) {
                result.addFailure("Unable to create new node with name " + params.getNewNodeName() + ". Import Failed.");
                return result;
            }
            this.save(resourceResolver, 0);
            node = childNode;
            result.addSuccess("New node '" + params.getNewNodeName() + "' Successfully Created");
        }

        int count = 0;
        String duplicateRes = params.getDuplicateResolution();
        while (rows.hasNext()) {
            String[] row = rows.next();

            log.info("length: " + row.length);
            if (row.length == VALID_ROW_LENGTH) {

                if (node.hasProperty(row[0])) {
                    if (duplicateRes.equals("Skip")) {
                        result.addWarning("Duplicate: skipping property with name: " + row[0]);
                    } else {
                        node.setProperty(row[0], row[1]);
                        result.addWarning("Duplicate: overwriting property with name: " + row[0]);
                        count++;
                    }
                } else {
                    node.setProperty(row[0], row[1]);
                    result.addSuccess("Imported key/value pair: " + row[0] + "/" + row[1]);
                    log.info("Imported  [ {} ] ~> [ {} ]", row[0], row[1]);
                    count++;
                }
            } else {
                log.warn("Row {} is malformed", Arrays.asList(row));
            }

            if (count == DEFAULT_BATCH_SIZE) {
                this.save(resourceResolver, count);
                count = 0;
            }
        }

        this.save(resourceResolver, count);
        return result;
    }


    private Node createNewNode(Node node, String name, String type) {
        try {
            return node.addNode(name, type);
        } catch (RepositoryException e) {
            log.error("Exception thrown while attempting to create new node", e);
        }
        return null;
    }

    private boolean checkProperty(Node node) {
        try {
            node.getProperty("value");
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    
    /**
     * Helper for saving changes to the JCR; contains timing logging.
     *
     * @param resourceResolver the resource resolver
     * @param size             the number of changes to save
     * @throws PersistenceException
     */
    private void save(final ResourceResolver resourceResolver, final int size) throws PersistenceException {
        if (resourceResolver.hasChanges()) {
            final long start = System.currentTimeMillis();
            resourceResolver.commit();
            if (log.isInfoEnabled()) {
                log.info("Imported a BATCH of [ {} ] assets in {} ms", size, System.currentTimeMillis() - start);
            }
        } else {
            log.debug("Nothing to save");
        }
    }
    
    /**
     * Helper method; adds a message to the JSON Response object.
     *
     * @param jsonObject the JSON object to add the message to
     * @param message    the message to add.
     */
    private void addMessage(final JSONObject jsonObject, final String message) {
        try {
            jsonObject.put("message", message);
        } catch (JSONException e) {
            log.error("Could not formulate JSON Response", e);
        }
    }

    /**
     * Result to expose success and failure paths to the JSON response.
     */
    private class Result {
        private List<String> success;
        private List<String> warning;
        private List<String> failure;

        public Result() {
            success = new ArrayList<String>();
            warning = new ArrayList<String>();
            failure = new ArrayList<String>();
        }

        public List<String> getSuccess() {
            return success;
        }

        public void addSuccess(String text) {
            this.success.add(text);
        }

        public List<String> getFailure() {
            return failure;
        }

        public void addFailure(String text) {
            this.failure.add(text);
        }

        public List<String> getWarning() {
            return warning;
        }

        public void addWarning(String text) {
            this.warning.add(text);
        }
    }
    
}
