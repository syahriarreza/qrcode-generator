package com.indesso.falcon.qrcode;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.web.scripts.content.StreamContent;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * The Class DownloadContentWebscript.
 *
 */
public class QRCodeGenerator extends StreamContent {
    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    private final String HOSTNAME = "localhost:8080";
    private final String PUBLIC_LINK = "http://"+HOSTNAME+"/alfresco/d/d/workspace/SpacesStore/";
    private final String TEMP_FOLDER_NAME = "temp";
    private final String QR_CODE_ASPECT = "qrcodepublic:inUse";

    private PermissionService permissionService;
    private ContentService contentService;
    private Repository repository;
    private FileFolderService fileFolderService;
    
    /* (non-Javadoc)
  * @see org.springframework.extensions.webscripts.WebScript#execute(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.WebScriptResponse)
     */
    @Override
    public void execute(final WebScriptRequest request, final WebScriptResponse response) throws IOException {
        try {
            final NodeRef nodeRef = getParameterAsNodeRef(request, "nodeRef");
            final boolean attach = Boolean.valueOf(request.getParameter("attach"));
            
            String docName = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            String publicLink = PUBLIC_LINK+"/"+nodeRef.getId()+"/"+docName;
            
            System.out.println("DSOUT> Generate QR Code | nodeRef: "+nodeRef.toString());
            logger.debug("DLOG> Generate QR Code | nodeRef: "+nodeRef.toString());
            
            permissionService.setPermission(nodeRef, PermissionService.GUEST_AUTHORITY, PermissionService.CONSUMER, true);
            
            //--Generate QR Code
            byte[] qrCodeByte;
            try {
                qrCodeByte = getQRCodeImage(publicLink, 350, 350);
            } catch (WriterException ex) {
                throw new AlfrescoRuntimeException("QRCodeGenerator whilst running getQRCodeImage: " + ex.getMessage());
            } catch (IOException ex) {
                throw new AlfrescoRuntimeException("QRCodeGenerator whilst running getQRCodeImage: " + ex.getMessage());
            }

            //--Create Temp Folder
            NodeRef companyHomeNode = repository.getCompanyHome();
            NodeRef tempFolderNode = fileFolderService.searchSimple(companyHomeNode, TEMP_FOLDER_NAME);
            if (tempFolderNode == null || !nodeService.exists(tempFolderNode)) {
                tempFolderNode = fileFolderService.create(companyHomeNode, TEMP_FOLDER_NAME, ContentModel.TYPE_FOLDER).getNodeRef();
            }

            //--Create QR Code Node
            String qrCodeName = "QR-" + FilenameUtils.removeExtension(docName) + ".png";
            NodeRef qrCodeNode = fileFolderService.searchSimple(tempFolderNode, qrCodeName);
            if (qrCodeNode != null && nodeService.exists(qrCodeNode)) {
                fileFolderService.delete(qrCodeNode);
            }
            qrCodeNode = fileFolderService.create(tempFolderNode, qrCodeName, ContentModel.TYPE_CONTENT).getNodeRef();

            //--Fill QR Code node with QR Code content
            ContentWriter writer = contentService.getWriter(qrCodeNode, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(mimetypeService.guessMimetype(qrCodeName));
            writer.putContent(new ByteArrayInputStream(qrCodeByte));

            //TODO: ngasi aspect ke nodeRef
            
            
            processDownload(request, response, qrCodeNode, attach);
            
        } catch (IOException | AlfrescoRuntimeException | InvalidNodeRefException excp) {
            logger.error("Exception occurred while downloading content", excp);
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, excp.getMessage(), excp);
        }
    }

    private byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray(); 
        return pngData;
    }

    /**
     * Process download will process the nodeRef given to streamContent.
     *
     * @param request the request
     * @param response the response
     * @param nodeRef the node ref
     * @param attach the attach
     * @throws IOException the IO exception
     */
    private void processDownload(final WebScriptRequest request, final WebScriptResponse response, final NodeRef nodeRef, final boolean attach) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        userAgent = StringUtils.isNotBlank(userAgent) ? userAgent.toLowerCase(Locale.ENGLISH) : StringUtils.EMPTY;
        final boolean isClientSupported = userAgent.contains("msie")|| userAgent.contains(" trident/")|| userAgent.contains(" chrome/")|| userAgent.contains(" firefox/");

        if (attach && isClientSupported) {
            String fileName = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            if (userAgent.contains("msie") || userAgent.contains(" trident/")) {
                final String mimeType = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getMimetype();
                if (!(this.mimetypeService.getMimetypes(FilenameUtils.getExtension(fileName)).contains(mimeType))) {
                    fileName = FilenameUtils.removeExtension(fileName) + FilenameUtils.EXTENSION_SEPARATOR_STR + this.mimetypeService.getExtension(mimeType);
                }
            }
            streamContent(request, response, nodeRef, ContentModel.PROP_CONTENT, attach, fileName, null);
        } else {
            streamContent(request, response, nodeRef, ContentModel.PROP_CONTENT, attach, null, null);
        }
    }

    /**
     * Create NodeRef instance from a WebScriptRequest parameter.
     *
     * @param req the req
     * @param paramName the param name
     * @return the parameter as node ref
     */
    private NodeRef getParameterAsNodeRef(final WebScriptRequest req, final String paramName) {
        final String nodeRefStr = StringUtils.trimToNull(req.getParameter(paramName));
        if (StringUtils.isBlank(nodeRefStr)) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing " + paramName + " parameter");
        }
        if (!NodeRef.isNodeRef(nodeRefStr)) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Incorrect format for " + paramName + " paramater");
        }
        final NodeRef nodeRef = new NodeRef(nodeRefStr);
        if (!nodeService.exists(nodeRef)) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, paramName + " not found");
        }
        return nodeRef;
    }

    /**
     * Sets the content service.
     *
     * @param contentService the content service
     */
    public void setContentService(final ContentService contentService) {
        this.contentService = contentService;
    }
    
    /**
     * Sets the repository.
     *
     * @param repository the repository
     */
    public void setRepository(Repository repository) {
         this.repository = repository;
    }
    
    /**
     * Sets the fileFolder service.
     *
     * @param fileFolderService the fileFolder Service
     */
    public void setFileFolderService(FileFolderService fileFolderService) {
         this.fileFolderService = fileFolderService;
    }
    
    /**
     * Sets the permission service.
     *
     * @param permissionService the permission Service
     */
    public void setPermissionService(PermissionService permissionService) {
         this.permissionService = permissionService;
    }
}
