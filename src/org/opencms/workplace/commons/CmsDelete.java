/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/CmsDelete.java,v $
 * Date   : $Date: 2006/10/04 09:41:00 $
 * Version: $Revision: 1.17.4.10 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.commons;

import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsDeleteResourcesHelper;
import org.opencms.relations.CmsRelation;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsRole;
import org.opencms.site.CmsSiteManager;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsDialogSelector;
import org.opencms.workplace.CmsMultiDialog;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.I_CmsDialogHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Provides methods for the delete resources dialog.<p> 
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/delete_standard.jsp
 * </ul>
 * <p>
 *
 * @author Andreas Zahner 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.17.4.10 $ 
 * 
 * @since 6.0.0 
 */
public class CmsDelete extends CmsMultiDialog implements I_CmsDialogHandler {

    /** Value for the action: delete the resource. */
    public static final int ACTION_DELETE = 100;

    /** The dialog type. */
    public static final String DIALOG_TYPE = "delete";

    /** Request parameter name for the deletesiblings parameter. */
    public static final String PARAM_DELETE_SIBLINGS = "deletesiblings";

    /** The delete dialog URI. */
    public static final String URI_DELETE_DIALOG = PATH_DIALOGS + "delete_standard.jsp";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsDelete.class);

    /** The broken relations helper object. */
    private CmsDeleteResourcesHelper m_helper;

    /** The delete siblings parameter value. */
    private String m_deleteSiblings;

    /**
     * Default constructor needed for dialog handler implementation.<p>
     */
    public CmsDelete() {

        super(null);
    }

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsDelete(CmsJspActionElement jsp) {

        super(jsp);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsDelete(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Performs the delete action, will be called by the JSP page.<p>
     * 
     * @throws JspException if problems including sub-elements occur
     */
    public void actionDelete() throws JspException {

        // save initialized instance of this class in request attribute for included sub-elements
        getJsp().getRequest().setAttribute(SESSION_WORKPLACE_CLASS, this);
        try {
            boolean isFolder = false;
            if (!isMultiOperation()) {
                isFolder = getCms().readResource(getParamResource(), CmsResourceFilter.ALL).isFolder();
            }
            if (performDialogOperation()) {
                // if no exception is caused and "true" is returned delete operation was successful
                if (isMultiOperation() || isFolder) {
                    // set request attribute to reload the explorer tree view
                    List folderList = new ArrayList(1);
                    folderList.add(CmsResource.getParentFolder((String)getResourceList().get(0)));
                    getJsp().getRequest().setAttribute(REQUEST_ATTRIBUTE_RELOADTREE, folderList);
                }
                actionCloseDialog();
            } else {
                // "false" returned, display "please wait" screen
                getJsp().include(FILE_DIALOG_SCREEN_WAIT);
            }
        } catch (Throwable e) {
            // prepare common message part
            includeErrorpage(this, e);
        }
    }

    /**
     * Returns the html for the "delete siblings" options when deleting a a resource with siblings.<p>
     * 
     * @return the html for the "delete siblings" options
     */
    public String buildDeleteSiblings() {

        StringBuffer result = new StringBuffer(512);
        boolean isFolder = false;
        if (!isMultiOperation()) {
            try {
                isFolder = getCms().readResource(getParamResource(), CmsResourceFilter.ALL).isFolder();
            } catch (CmsException e) {
                // ignore
            }
        }
        if (isMultiOperation() || isFolder || (hasSiblings() && hasCorrectLockstate())) {
            // show only for multi resource operation or if resource has siblings and correct lock state
            int defaultMode = Boolean.valueOf(getParamDeleteSiblings()).booleanValue() ? CmsResource.DELETE_REMOVE_SIBLINGS
            : CmsResource.DELETE_PRESERVE_SIBLINGS;
            if (!isMultiOperation() && !isFolder) {
                result.append(key(Messages.GUI_DELETE_WARNING_SIBLINGS_0));
                result.append("<p>");
            }
            result.append("<input type=\"radio\" name=\"");
            result.append(PARAM_DELETE_SIBLINGS);
            result.append("\" value=\"false\"");
            if (defaultMode == CmsResource.DELETE_PRESERVE_SIBLINGS) {
                result.append(" checked=\"checked\"");
            }
            result.append(" onclick='reloadDialog(false);'>&nbsp;");
            result.append(key(Messages.GUI_DELETE_PRESERVE_SIBLINGS_0));
            result.append("<br>");
            result.append("<input type=\"radio\" name=\"");
            result.append(PARAM_DELETE_SIBLINGS);
            result.append("\" value=\"true\"");
            if (defaultMode == CmsResource.DELETE_REMOVE_SIBLINGS) {
                result.append(" checked=\"checked\"");
            }
            result.append(" onclick='reloadDialog(true);'>&nbsp;");
            result.append(key(Messages.GUI_DELETE_ALL_SIBLINGS_0));
            result.append("<p>");
        }
        return result.toString();
    }

    /**
     * Returns the html for the confirmation message.<p>
     * 
     * @return the html for the confirmation message
     */
    public String buildConfirmation() {

        StringBuffer result = new StringBuffer(512);
        boolean isFolder = false;
        if (!isMultiOperation()) {
            try {
                isFolder = getCms().readResource(getParamResource(), CmsResourceFilter.ALL).isFolder();
            } catch (CmsException e) {
                // ignore
            }
        }
        result.append("<div id='conf-msg'>\n");
        if (isMultiOperation() || isFolder || (hasSiblings() && hasCorrectLockstate())) {
            result.append(key(Messages.GUI_DELETE_MULTI_CONFIRMATION_0));
        } else {
            result.append(key(Messages.GUI_DELETE_CONFIRMATION_0));
        }
        result.append("\n</div>\n");
        return result.toString();
    }

    /**
     * Returns html code for the 'delete despite relations' option.<p>
     * 
     * @return html code for the 'delete despite relations' option
     */
    public String buildRelations() {

        if (getHelper().isEmpty()) {
            return "";
        }

        // check how many row we will need to display to decide using a div or not
        int rows = 0;
        Iterator itBrokenRelations = getHelper().values().iterator();
        while (itBrokenRelations.hasNext()) {
            List relations = (List)itBrokenRelations.next();
            rows++; // resName
            rows += relations.size();
            if (rows > 6) {
                break;
            }
        }

        StringBuffer result = new StringBuffer(512);
        // TODO: display the 'Print' text at the right of the icon  
        result.append("<div style='z-index: 100; position:absolute; padding-right: ");
        if (getJsp().getRequest().getHeader("user-agent").indexOf("MSIE") < 0) {
            result.append(rows > 6 ? 35 : 15);
        } else {
            result.append(rows > 6 ? 20 : 0);
        }
        result.append("px;' >\n");
        result.append("<a href='#' onclick=\"javascript:document.forms['printform'].submit();\" style='color: ButtonText; text-decoration: none; cursor: pointer; '>\n");
        result.append("<img src='");
        result.append(getResourceUri("list/print.png"));
        result.append("' border='0' align='right'/>\n");
        result.append("</a>");
        result.append("</div>\n");

        result.append("<div style='width: 100%; height:100px; overflow: auto;'>\n");

        // sort the resulting hash map
        List resourceList = new ArrayList(getHelper().keySet());
        Collections.sort(resourceList);

        // for every resource that will break a relation 
        itBrokenRelations = resourceList.iterator();
        int id = 0;
        while (itBrokenRelations.hasNext()) {
            String resName = (String)itBrokenRelations.next();
            result.append(buildFoldableEntry(resName, id));
            id++;
        }

        result.append("</div>\n");

        return result.toString();
    }

    /**
     * Returns the broken relations helper object.<p>
     * 
     * @return a helper object
     */
    public CmsDeleteResourcesHelper getHelper() {

        if (m_helper == null) {
            m_helper = new CmsDeleteResourcesHelper(getCms(), getResourceList(), Boolean.valueOf(
                getParamDeleteSiblings()).booleanValue());
        }
        return m_helper;
    }

    /**
     * @see org.opencms.workplace.I_CmsDialogHandler#getDialogHandler()
     */
    public String getDialogHandler() {

        return CmsDialogSelector.DIALOG_DELETE;
    }

    /**
     * @see org.opencms.workplace.I_CmsDialogHandler#getDialogUri(java.lang.String, CmsJspActionElement)
     */
    public String getDialogUri(String resource, CmsJspActionElement jsp) {

        return URI_DELETE_DIALOG;
    }

    /**
     * Returns the value of the boolean option to delete siblings.<p>
     * 
     * @return the value of the boolean option to delete siblings as a lower case string
     */
    public String getParamDeleteSiblings() {

        return m_deleteSiblings;
    }

    /**
     * Checks if the current resource has lock state exclusive or inherited.<p>
     * 
     * This is used to determine whether the dialog shows the option to delete all
     * siblings of the resource or not.
     * 
     * @return true if lock state is exclusive or inherited, otherwise false
     */
    public boolean hasCorrectLockstate() {

        CmsLock lock = null;
        try {
            // get the lock state for the current resource
            lock = getCms().getLock(getParamResource());
        } catch (CmsException e) {
            // error getting lock state, log the error and return false
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        }
        // check if autolock feature is enabled
        boolean autoLockFeature = lock.isNullLock() && OpenCms.getWorkplaceManager().autoLockResources();
        return autoLockFeature || lock.isExclusive() || lock.isInherited();
    }

    /**
     * Checks if this resource has siblings.<p>
     * 
     * @return true if this resource has siblings
     */
    public boolean hasSiblings() {

        try {
            return getCms().readResource(getParamResource(), CmsResourceFilter.ALL).getSiblingCount() > 1;
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        }

    }

    /**
     * Sets the value of the boolean option to delete siblings.<p>
     * 
     * @param value the value of the boolean option to delete siblings
     */
    public void setParamDeleteSiblings(String value) {

        m_deleteSiblings = value;
        m_helper = null;
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {

        // fill the parameter values in the get/set methods
        fillParamValues(request);

        if (getParamDeleteSiblings() == null) {
            setParamDeleteSiblings(Boolean.toString(getSettings().getUserSettings().getDialogDeleteFileMode() == CmsResource.DELETE_REMOVE_SIBLINGS));
        }
        // check the required permissions to delete the resource       
        if (!checkResourcePermissions(CmsPermissionSet.ACCESS_WRITE, false)) {
            // no write permissions for the resource, set cancel action to close dialog
            setParamAction(DIALOG_CANCEL);
        }

        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        // set the action for the JSP switch 
        if (DIALOG_TYPE.equals(getParamAction())) {
            setAction(ACTION_DELETE);
        } else if (DIALOG_WAIT.equals(getParamAction())) {
            setAction(ACTION_WAIT);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            setAction(ACTION_CANCEL);
        } else {
            setAction(ACTION_DEFAULT);
            // build title for delete dialog     
            setDialogTitle(Messages.GUI_DELETE_RESOURCE_1, Messages.GUI_DELETE_MULTI_2);
        }
    }

    /**
     * Performs the resource deletion.<p>
     * 
     * @return true, if the resource(s) was/were deleted, otherwise false
     * @throws CmsException if deletion is not successful
     */
    protected boolean performDialogOperation() throws CmsException {

        int deleteOption = -1;

        // check if the current resource is a folder for single operation
        boolean isFolder = isOperationOnFolder();
        // on folder deletion or multi operation display "please wait" screen, not for simple file deletion
        if ((isMultiOperation() || isFolder) && !DIALOG_WAIT.equals(getParamAction())) {
            // return false, this will trigger the "please wait" screen
            return false;
        }

        // determine the correct delete option
        deleteOption = Boolean.valueOf(getParamDeleteSiblings()).booleanValue() ? CmsResource.DELETE_REMOVE_SIBLINGS
        : CmsResource.DELETE_PRESERVE_SIBLINGS;

        Iterator i = getResourceList().iterator();
        // iterate the resources to delete
        while (i.hasNext()) {
            String resName = (String)i.next();
            try {
                performSingleDeleteOperation(resName, deleteOption);
            } catch (CmsException e) {
                if (isMultiOperation()) {
                    // collect exceptions to create a detailed output
                    addMultiOperationException(e);
                } else {
                    // for single operation, throw the exception immediately
                    throw e;
                }
            }
        }
        // check if exceptions occured
        checkMultiOperationException(Messages.get(), Messages.ERR_DELETE_MULTI_0);

        return true;
    }

    /**
     * Performs the delete operation for a single VFS resource.<p>
     * 
     * @param resource the resource VFS path
     * @param deleteOption the delete option for sibling deletion
     * @throws CmsException if deleting the resource fails
     */
    protected void performSingleDeleteOperation(String resource, int deleteOption) throws CmsException {

        // lock resource if autolock is enabled
        checkLock(resource);
        // delete the resource
        getCms().deleteResource(resource, deleteOption);
    }

    /**
     * Creates a foldable entry for the given resource and broken links.<p>
     * 
     * @param resourceName the current resource to delete that will break links
     * @param id the html id attribute for the folding element
     * 
     * @return StringBuffer with HTML code of the form
     */
    private StringBuffer buildFoldableEntry(String resourceName, int id) {

        StringBuffer result = new StringBuffer(256);

        String idValue = "res" + id;

        // add toggle symbol and link to details
        result.append("<a href=\"javascript:toggleDetail('").append(idValue).append("');\">");
        result.append("<img src=\"").append(getSkinUri()).append("commons/minus.png\" class=\"noborder\" id=\"ic-").append(
            idValue).append("\"></a>");

        result.append("<span class=\"textbold\" style='vertical-align:top;'>");

        String resName = null;
        CmsDeleteResourcesHelper.InfoEntry infoEntry = getHelper().getInfoEntry(resourceName);
        if (infoEntry.isSibling()) {
            if (!infoEntry.isInOtherSite()) {
                resName = key(Messages.GUI_DELETE_SIBLING_RELATION_1, new Object[] {infoEntry.getResourceName()});
            } else {
                String siblingName = key(
                    Messages.GUI_DELETE_SIBLING_RELATION_1,
                    new Object[] {infoEntry.getResourceName()});
                resName = key(Messages.GUI_DELETE_SITE_RELATION_2, new Object[] {infoEntry.getSiteName(), siblingName});
            }
        } else {
            resName = CmsResource.getName(resourceName);
        }
        result.append(resName);
        result.append("</span><br>\n");

        result.append("<div id =\"").append(idValue).append("\" class=\"show\">");
        result.append("<p style='padding-left: 25px; margin-top: -3px; margin-bottom: 3px;' >\n");

        Iterator itRelations = infoEntry.getRelations().iterator();

        // show all links that will get broken
        while (itRelations.hasNext()) {
            CmsRelation relation = (CmsRelation)itRelations.next();
            String relationName = relation.getSourcePath();
            if (relationName.startsWith(infoEntry.getSiteRoot())) {
                // same site
                relationName = relationName.substring(infoEntry.getSiteRoot().length());
                relationName = CmsStringUtil.formatResourceName(relationName, 50);
            } else {
                // other site
                String site = CmsSiteManager.getSiteRoot(relationName);
                String siteName = site;
                if (site != null) {
                    relationName = relationName.substring(site.length());
                    siteName = CmsSiteManager.getSite(site).getTitle();
                } else {
                    siteName = "/";
                }
                relationName = CmsStringUtil.formatResourceName(relationName, 50);
                relationName = key(Messages.GUI_DELETE_SITE_RELATION_2, new Object[] {siteName, relationName});
            }
            result.append(relationName);
            result.append("&nbsp;<span style='color: #666666;'>(");
            result.append(relation.getType().getLocalizedName(getLocale()));
            result.append(")</span>");
            if (itRelations.hasNext()) {
                result.append("<br>");
            }
            result.append("\n");
        }

        result.append("</p>");
        result.append("</div>\n");

        return result;
    }

    /**
     * Returns <code>true</code> if the current user is allowed 
     * to delete the selected resources.<p>
     * 
     * @return <code>true</code> if the current user is allowed 
     *          to delete the selected resources
     */
    public boolean isCanDelete() {

        return OpenCms.getWorkplaceManager().getDefaultUserSettings().isAllowBrokenRelations()
            || getCms().hasRole(CmsRole.VFS_MANAGER);
    }

}