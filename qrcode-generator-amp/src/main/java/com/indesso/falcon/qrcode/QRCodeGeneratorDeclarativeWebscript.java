package com.indesso.falcon.qrcode;

import java.util.HashMap;
import java.util.Map;
import org.alfresco.error.AlfrescoRuntimeException;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class QRCodeGeneratorDeclarativeWebscript extends DeclarativeWebScript {
    private String HOSTNAME = "localhost:8080";
    private String PUBLIC_LINK = "http://"+HOSTNAME+"/alfresco/d/d/workspace/SpacesStore/";
    private Repository repository;
    private NodeService nodeService;
    private PermissionService permissionService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private static Log logger = LogFactory.getLog(QRCodeGeneratorDeclarativeWebscript.class);

    // public void setRepository(Repository repository) {
    //     this.repository = repository;
    // }
    
    private byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray(); 
        return pngData;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        String nodeRefStr = req.getParameter("nodeRef");
        String fileName = req.getParameter("fileName");
        System.out.println("D> Generate QR Code | NR: "+nodeRefStr+" | NM: "+fileName);
        logger.debug("D> Generate QR Code | NR: "+nodeRefStr+" | NM: "+fileName);
        String publicLink = PUBLIC_LINK+"/"+nodeRefStr+"/"+fileName;
        
        //--Get Node and set Guest Permission
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        if (!nodeService.exists(nodeRef)) {
            throw new AlfrescoRuntimeException("Sorry, " + nodeRef + " doesn't exist");
        }
        permissionService.setPermission(nodeRef, PermissionService.GUEST_AUTHORITY, PermissionService.CONSUMER, true);
        
        //--Generate QR Code
        byte[] qrCodeByte;
        try {
            qrCodeByte = getQRCodeImage(publicLink, 350, 350);
        } catch (WriterException ex) {
            logger.error(ex);
            throw new AlfrescoRuntimeException("QRCodeGenerator whilst running getQRCodeImage: " + ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex);
            throw new AlfrescoRuntimeException("QRCodeGenerator whilst running getQRCodeImage: " + ex.getMessage());
        }
        
        //--Create QR Code Node
        NodeRef companyHomeNode = repository.getCompanyHome();
        NodeRef tempFolderNode = fileFolderService.create(companyHomeNode, "temp", ContentModel.TYPE_FOLDER).getNodeRef();
        
        QName qrCodeQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
        NodeRef qrCodeNode = fileFolderService.create(tempFolderNode, "qrcode-"+nodeRefStr.split("/")[3]+".png", qrCodeQName).getNodeRef();
        
        //--Fill QR Code node with QR Code content
        ContentWriter writer = contentService.getWriter(qrCodeNode, ContentModel.PROP_CONTENT, true);
        writer.putContent(new ByteArrayInputStream(qrCodeByte));

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("qrcodeNodeRef", qrCodeNode.toString()); //--sent to javascript controller
        return model;
    }
}