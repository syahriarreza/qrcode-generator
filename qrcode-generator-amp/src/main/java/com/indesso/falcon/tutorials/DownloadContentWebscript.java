package com.indesso.falcon.tutorials;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
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
public class DownloadContentWebscript extends StreamContent {

    /**
     * The Constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadContentWebscript.class);

    /**
     * The content service.
     */
    private ContentService contentService;

    /**
     * The authentication service.
     */
    private AuthenticationService authenticationService;

    /**
     * The site service.
     */
    private SiteService siteService;

    /**
     * The authority service.
     */
    private AuthorityService authorityService;

    /* (non-Javadoc)
  * @see org.springframework.extensions.webscripts.WebScript#execute(org.springframework.extensions.webscripts.WebScriptRequest, org.springframework.extensions.webscripts.WebScriptResponse)
     */
    @Override
    public void execute(final WebScriptRequest request, final WebScriptResponse response) throws IOException {
        LOGGER.info("Started executing DownloadContentWebscript...");
        try {
            final NodeRef nodeRef = getParameterAsNodeRef(request, "nodeRef");
            final String userName = authenticationService.getCurrentUserName();
            if (isNotAuthorised(nodeRef, userName, siteService, permissionService, authorityService)) {
                response.setStatus(401);
                response.getWriter().write("User is unauthorised to download the requested content!");
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Processing the download requested by: {}", userName);
                }
                final boolean attach = Boolean.valueOf(request.getParameter("attach"));
                processDownload(request, response, nodeRef, attach, ContentModel.PROP_CONTENT);
            }
        } catch (AccessDeniedException accessDenied) {
            LOGGER.error("Access denied while downloading content", accessDenied);
            throw new WebScriptException(Status.STATUS_UNAUTHORIZED,
                    accessDenied.getMessage(), accessDenied);
        } catch (IOException | AlfrescoRuntimeException
                | InvalidNodeRefException excp) {
            LOGGER.error("Exception occurred while downloading content", excp);
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
                    excp.getMessage(), excp);
        }
        LOGGER.info("Existing from DownloadContentWebscript...");
    }

    /**
     * Process download.
     *
     * @param request the request
     * @param response the response
     * @param nodeRef the node ref
     * @param attach the attach
     * @param propertyQName the property q name
     * @throws IOException the IO exception
     */
    private void processDownload(final WebScriptRequest request, final WebScriptResponse response, final NodeRef nodeRef, final boolean attach, final QName propertyQName) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        userAgent = StringUtils.isNotBlank(userAgent) ? userAgent.toLowerCase(Locale.ENGLISH) : StringUtils.EMPTY;
        final boolean isClientSupported = userAgent.contains("msie")
                || userAgent.contains(" trident/")
                || userAgent.contains(" chrome/")
                || userAgent.contains(" firefox/");

        if (attach && isClientSupported) {
            String fileName = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            if (userAgent.contains("msie") || userAgent.contains(" trident/")) {
                final String mimeType = contentService.getReader(nodeRef, propertyQName).getMimetype();
                if (!(this.mimetypeService.getMimetypes(FilenameUtils.getExtension(fileName)).contains(mimeType))) {
                    fileName = FilenameUtils.removeExtension(fileName) + FilenameUtils.EXTENSION_SEPARATOR_STR + this.mimetypeService.getExtension(mimeType);
                }
            }
            streamContent(request, response, nodeRef, propertyQName, attach, fileName, null);
        } else {
            streamContent(request, response, nodeRef, propertyQName, attach, null, null);
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
     * Checks if is not authorised.
     *
     * @param nodeRef the node ref
     * @param userName the user name
     * @param siteService the site service
     * @param permissionService the permission service
     * @param authorityService the authority service
     * @return true, if checks if is not authorised
     */
    private boolean isNotAuthorised(final NodeRef nodeRef,
            final String userName, final SiteService siteService,
            final PermissionService permissionService,
            final AuthorityService authorityService) {
        boolean isNotAuthorised = false;
        final SiteInfo siteInfo = siteService.getSite(nodeRef);
        // Checking siteInfo, If it is null that means user is not a member of site and 
        // hence isNotAuthorised is default to false.
        if (null != siteInfo) {
            if (siteService.isMember(siteInfo.getShortName(), userName)) {
                final Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nodeRef);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking isNotAuthorised, Available access permissions are: {}", permissions);
                }
                for (final AccessPermission permission : permissions) {
                    if (permission.getPermission().equals("SiteConsumer")
                            || permission.getPermission().equals("Consumer")) {
                        if (permission.getAuthorityType().equals("USER")
                                && permission.getAuthority().equals(userName)) {
                            isNotAuthorised = true;
                            break;
                        } else if (permission.getAuthorityType().toString().equals("GROUP")) {
                            //Set run as system user since other users including consumers can not fetch authorities
                            AuthenticationUtil.setRunAsUserSystem();
                            final Set<String> authorities = authorityService.getAuthoritiesForUser(userName);
                            //Clear system user context and set original user context
                            AuthenticationUtil.clearCurrentSecurityContext();
                            AuthenticationUtil.setFullyAuthenticatedUser(userName);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Checking permissions at GROUP level, user has following authorities: {}", authorities);
                            }
                            for (final String authority : authorities) {
                                if (authority.equals(permission.getAuthority())) {
                                    isNotAuthorised = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                isNotAuthorised = true;//Not a member in the site.
            }
        }
        return isNotAuthorised;
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
     * Sets the authentication service.
     *
     * @param authenticationService the authentication service
     */
    public void setAuthenticationService(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Sets the site service.
     *
     * @param siteService the site service
     */
    public void setSiteService(final SiteService siteService) {
        this.siteService = siteService;
    }

    /**
     * Sets the authority service.
     *
     * @param authorityService the authority service
     */
    public void setAuthorityService(final AuthorityService authorityService) {
        this.authorityService = authorityService;
    }
}
