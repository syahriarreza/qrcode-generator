package com.indesso.falcon.qrcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.springframework.extensions.webscripts.Cache;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class GetTicket extends DeclarativeWebScript {
    private static final Logger logger = LoggerFactory.getLogger(GetTicket.class);
	private PermissionService permissionService;
	private ContentService contentService;
	private Repository repository;
	private FileFolderService fileFolderService;
	
	private String hostname;
	private String username;
	private String password;

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		String ticket = "";
		try {
			ticket = getTicket(username, password);
		} catch (ProtocolException ex) {
			java.util.logging.Logger.getLogger(GetTicket.class.getName()).log(Level.SEVERE, null, ex);
			System.out.println(ex.toString());
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(GetTicket.class.getName()).log(Level.SEVERE, null, ex);
			System.out.println(ex.toString());
		} catch (JSONException ex) {
			java.util.logging.Logger.getLogger(GetTicket.class.getName()).log(Level.SEVERE, null, ex);
			System.out.println(ex.toString());
		}
		
		Map<String, Object> model = new HashMap<String, Object>();
        model.put("hostname", hostname);
        model.put("username", username);
        model.put("ticket", ticket);
        return model;
    }
	
	public String getTicket(String username, String password) throws MalformedURLException, ProtocolException, IOException, JSONException {
		String ticket = "";
		URL urlForGetRequest = new URL(hostname+"/alfresco/s/api/login?u="+username+"&pw="+password);
		HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
		conection.setRequestMethod("GET");
		int responseCode = conection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader buffReader = new BufferedReader(new InputStreamReader(conection.getInputStream()));
			String jsonStr = org.apache.commons.io.IOUtils.toString(buffReader);
			JSONObject json = new JSONObject(jsonStr);
			JSONObject data = json.getJSONObject("data");
			ticket = data.getString("ticket");
			buffReader.close();
		} else {
			System.out.println("ERROR Get Ticket for user: \""+username+"\" | ResponseCode: "+responseCode);
		}
		
		return ticket;
	}
	
	// ### Setters ###
	
	public void setContentService(final ContentService contentService) {
		this.contentService = contentService;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}