package com.indesso.falcon.qrcode;

import java.io.IOException;
import java.util.Locale;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.web.scripts.content.StreamContent;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
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

	//private final String HOSTNAME = "localhost:8080";
	//private final String USER_PUBLIC = "public";
	private final String TEMP_FOLDER_NAME = "temp";
	private final String QR_CODE_MODEL_URI = "qrcodepublic.custom.model";
	private final String QR_CODE_ASPECT_INUSE = "inUse"; //--invisible aspect
	private final String QR_CODE_ASPECT_PROPS = "props";
	private final String QR_CODE_PROP_TITLE = "publicTitle";
	private final Integer QR_CODE_WIDTH = 350;
	private final Integer QR_CODE_HEIGHT = 350;
	private final String QR_CODE_FONT = "Arial Black";
	private final Integer QR_CODE_FONT_SIZE = 17;

	private PermissionService permissionService;
	private ContentService contentService;
	private Repository repository;
	private FileFolderService fileFolderService;
	
	private String PUBLIC_LINK = "";
	private String PUBLIC_FOLDER_LINK = "";
	private String hostname;
	private String publicUserName;

	/* (non-Javadoc)
  * @see org.springframework.extensions.webscripts.WebScript#execute(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.WebScriptResponse)
	 */
	@Override
	public void execute(final WebScriptRequest request, final WebScriptResponse response) throws IOException {
		try {
			PUBLIC_LINK = "http://" + hostname + "/alfresco/d/d/workspace/SpacesStore/";
			PUBLIC_FOLDER_LINK = "http://" + hostname + "/share/page/repository#filter=path|";
			
			final NodeRef nodeRef = getParameterAsNodeRef(request, "nodeRef");
			final boolean attach = Boolean.valueOf(request.getParameter("attach"));

			String docName = this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
			String publicLink = PUBLIC_LINK + "/" + nodeRef.getId() + "/" + docName;
			if (fileFolderService.getFileInfo(nodeRef).isFolder()) {
				String path = "";
				List<FileInfo> fileInfos = fileFolderService.getNamePath(null, nodeRef);
				for (FileInfo fileInfo : fileInfos) {
					if (fileInfo.isFolder() && !fileInfo.getName().equalsIgnoreCase("company home")) {
						path += ("/" + fileInfo.getName());
					}
				}
				publicLink = PUBLIC_FOLDER_LINK + path + "|&page=1";
			}

			System.out.println("JD> Generate QR Code | " + nodeRef.toString() + " | " + docName);

			//--Add Aspect qrcodepublic:inUse to indicate the content is publicly shared and add Guest permission
			QName aspectInUse = QName.createQName(QR_CODE_MODEL_URI, QR_CODE_ASPECT_INUSE);
			Map<QName, Serializable> aspectValues = new HashMap<QName, Serializable>();
			nodeService.addAspect(nodeRef, aspectInUse, aspectValues);
			if (fileFolderService.getFileInfo(nodeRef).isFolder()) {
				permissionService.setPermission(nodeRef, publicUserName, PermissionService.CONSUMER, true);
			} else {
				permissionService.setPermission(nodeRef, PermissionService.GUEST_AUTHORITY, PermissionService.CONSUMER, true);
			}

			//--Define QR Code Title
			String qrCodeTitle = "";
			QName aspectProps = QName.createQName(QR_CODE_MODEL_URI, QR_CODE_ASPECT_PROPS);
			QName propTitleQ = QName.createQName(QR_CODE_MODEL_URI, QR_CODE_PROP_TITLE);
			Serializable propTitle = nodeService.getProperty(nodeRef, propTitleQ);
			if (nodeService.hasAspect(nodeRef, aspectProps) && propTitle != null && propTitle.toString() != "") {
				qrCodeTitle = propTitle.toString();
			}
			System.out.println("JD> QR Code Title: "+qrCodeTitle+" | has "+aspectProps.toString()+"? "+nodeService.hasAspect(nodeRef, aspectProps)+" | "+propTitleQ.toString()+": "+((propTitle == null) ? "" : propTitle.toString()));
			
			//--Generate QR Code
			byte[] qrCodeByte;
			try {
				qrCodeByte = getQRCodeImage(publicLink, qrCodeTitle, QR_CODE_WIDTH, QR_CODE_HEIGHT);
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

			processDownload(request, response, qrCodeNode, attach);

		} catch (IOException | AlfrescoRuntimeException | InvalidNodeRefException excp) {
			logger.error("Exception occurred while downloading content", excp);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, excp.getMessage(), excp);
		} catch (FileNotFoundException ex) {
			logger.error("FileNotFoundException", ex);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	private byte[] getQRCodeImage(String content, String title, int width, int height) throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

		ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
		byte[] pngData = pngOutputStream.toByteArray();

		ByteArrayInputStream bais = new ByteArrayInputStream(pngData);
		BufferedImage bufferedImage = ImageIO.read(bais);

		if (title != "") {
			drawCenteredString(
				bufferedImage.getGraphics(),
				title,
				new Rectangle(width, height),
				new Font(QR_CODE_FONT, Font.PLAIN, QR_CODE_FONT_SIZE)
			);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();

		return imageInByte;
	}

	/**
	 * Draw a String centered in the middle of a Rectangle.
	 *
	 * @param g The Graphics instance.
	 * @param text The String to draw.
	 * @param rect The Rectangle to center the text in.
	 */
	public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
		FontMetrics metrics = g.getFontMetrics(font);
		int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2; // Determine the X coordinate for the text
		int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent(); // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
		g.setColor(Color.BLACK);
		g.setFont(font);
		g.drawString(text, x, QR_CODE_HEIGHT-10);
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
		final boolean isClientSupported = userAgent.contains("msie") || userAgent.contains(" trident/") || userAgent.contains(" chrome/") || userAgent.contains(" firefox/");

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

	public void setPublicUserName(String publicUserName) {
		this.publicUserName = publicUserName;
	}
	
}
