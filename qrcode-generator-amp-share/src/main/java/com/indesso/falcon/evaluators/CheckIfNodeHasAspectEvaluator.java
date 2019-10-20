package com.indesso.falcon.evaluators;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.evaluator.BaseEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CheckIfNodeHasAspectEvaluator extends BaseEvaluator {
    private static Log logger = LogFactory.getLog(CheckIfNodeHasAspectEvaluator.class);
    private String customAspect;

    @Override
    public boolean evaluate(JSONObject jsonObject) {
        try {
            JSONObject node = (JSONObject) jsonObject.get("node");
            JSONArray nodeAspects = (JSONArray) node.get("aspects");
            boolean isContainer = (boolean) node.get("isContainer");
            if (!isContainer && nodeAspects != null) {
                return nodeAspects.contains(customAspect);
            } else {
                return false;
            }
        } catch (Exception err) {
            throw new AlfrescoRuntimeException("JSONException whilst running CheckIfNodeHasAspectEvaluator: " + err.getMessage());
        }
    }
    
    public void setCustomAspect(String customAspect) {
        this.customAspect = customAspect;
    }
}