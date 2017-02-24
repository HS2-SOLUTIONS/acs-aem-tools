package com.adobe.acs.tools.csv_property_importer;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;

import java.io.IOException;

public class Parameters extends com.adobe.acs.tools.csv.impl.Parameters {

    private String DEFAULT_PATH = "/content";
    private String DEFAULT_NEW_NODE_NAME = "newNode";
    private String DEFAULT_NEW_NODE_TYPE = "nt:unstructured";
    private String DEFAULT_DUPLICATE_RES = "Overwrite";

    private String path = DEFAULT_PATH;
    private boolean createNewNode = false;
    private String newNodeName = DEFAULT_NEW_NODE_NAME;
    private String newNodeType = DEFAULT_NEW_NODE_TYPE;
    private String duplicateResolution = DEFAULT_DUPLICATE_RES;

    public Parameters(SlingHttpServletRequest request) throws IOException {
        super(request);

        final RequestParameter pathParam = request.getRequestParameter("path");
        final RequestParameter createNewNodeParam = request.getRequestParameter("createNewNode");
        final RequestParameter newNodeNameParam = request.getRequestParameter("newNodeName");
        final RequestParameter newNodeTypeParam = request.getRequestParameter("newNodeType");
        final RequestParameter duplicateResolutionParam = request.getRequestParameter("duplicateResolution");

        this.path = DEFAULT_PATH;
        if (pathParam != null) {
            this.path = StringUtils.defaultIfEmpty(pathParam.toString(), DEFAULT_PATH);
        }

        createNewNode = BooleanUtils.toBoolean(createNewNodeParam.toString());

        this.newNodeName = DEFAULT_NEW_NODE_NAME;
        if (newNodeNameParam != null) {
            this.newNodeName = StringUtils.defaultIfEmpty(newNodeNameParam.toString(), DEFAULT_NEW_NODE_NAME);
        }

        this.newNodeType = DEFAULT_NEW_NODE_TYPE;
        if (newNodeTypeParam != null) {
            this.newNodeType = StringUtils.defaultIfEmpty(newNodeTypeParam.toString(), DEFAULT_NEW_NODE_TYPE);
        }

        this.duplicateResolution = DEFAULT_DUPLICATE_RES;
        if (duplicateResolutionParam != null) {
            this.duplicateResolution = StringUtils.defaultIfEmpty(duplicateResolutionParam.toString(), DEFAULT_DUPLICATE_RES);
        }

    }

    public final String getPath() {
        return path;
    }

    public boolean isCreateNewNode() {
        return createNewNode;
    }

    public String getNewNodeName() {
        return newNodeName;
    }

    public String getNewNodeType() {
        return newNodeType;
    }

    public String getDuplicateResolution() {
        return duplicateResolution;
    }
}
