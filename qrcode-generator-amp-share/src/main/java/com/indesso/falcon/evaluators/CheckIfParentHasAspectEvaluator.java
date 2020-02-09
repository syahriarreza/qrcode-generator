package com.indesso.falcon.evaluators;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.evaluator.BaseEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CheckIfParentHasAspectEvaluator extends BaseEvaluator {
    private static Log logger = LogFactory.getLog(CheckIfParentHasAspectEvaluator.class);
    private String customAspect;

    @Override
    public boolean evaluate(JSONObject jsonObject) {
        try {
            System.out.println("JSON:\n" + jsonObject);
            JSONObject parent = (JSONObject) jsonObject.get("parent");
            JSONArray parentAspects = (JSONArray) parent.get("aspects");
            boolean isContainer = (boolean) parent.get("isContainer");
            if (isContainer && parentAspects != null) {
                return parentAspects.contains(customAspect);
            } else {
                return false;
            }
        } catch (Exception err) {
            //throw new AlfrescoRuntimeException("JSONException whilst running CheckIfParentHasAspectEvaluator: " + err.getMessage());
			System.out.println("JSONException whilst running CheckIfParentHasAspectEvaluator: " + err.getMessage());
			return false;
        }
    }

    public void setCustomAspect(String customAspect) {
        this.customAspect = customAspect;
    }
}