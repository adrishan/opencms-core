/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/client/ui/input/form/Attic/CmsFormDialog.java,v $
 * Date   : $Date: 2010/06/15 12:34:30 $
 * Version: $Revision: 1.7 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.gwt.client.ui.input.form;

import org.opencms.gwt.client.Messages;
import org.opencms.gwt.client.ui.CmsPopupDialog;
import org.opencms.gwt.client.ui.CmsPushButton;
import org.opencms.gwt.client.ui.input.I_CmsFormField;
import org.opencms.xml.content.CmsXmlContentProperty;

import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

/**
 * A dialog containing a form.<p>
 * 
 * @author Georg Westenberger
 * 
 * @version $Revision: 1.7 $
 * 
 * @since 8.0.0
 */
public class CmsFormDialog extends CmsPopupDialog implements I_CmsFormDialog {

    /** The widget containing the form fields. */
    protected CmsForm m_form;

    /** The form handler for this dialog. */
    protected I_CmsFormHandler m_formHandler;

    /** The OK button of this dialog. */
    protected CmsPushButton m_okButton;

    /** 
     * Constructs a new form dialog with a given title.<p>
     * 
     * @param title the title of the form dialog 
     */
    public CmsFormDialog(String title) {

        super(title, new CmsForm());
        setGlassEnabled(true);
        setAutoHideEnabled(false);
        setModal(true);
        m_form = (CmsForm)getContent();
        addButton(createCancelButton());
        addButton(createResetButton());
        m_okButton = createOkButton();
        m_form.setFormDialog(this);
        addButton(m_okButton);

    }

    /**
     * Static utility method for opening a property form dialog.<p>
     * 
     * @param propertyConfig the configuration of the properties 
     * @param properties the current values of the properties 
     * @param title the title of the dialog 
     * @param formHandler the form handler which should be used 
     */
    public static void showPropertyDialog(
        Map<String, CmsXmlContentProperty> propertyConfig,
        Map<String, String> properties,
        String title,
        I_CmsFormHandler formHandler) {

        CmsFormDialog dialog = new CmsFormDialog(title);
        dialog.setFormHandler(formHandler);
        CmsForm form = dialog.getForm();
        Map<String, I_CmsFormField> formFields = CmsBasicFormField.createFields(propertyConfig.values());

        for (I_CmsFormField field : formFields.values()) {
            String currentValue = properties.get(field.getId());
            form.addField(field, currentValue);
        }
        dialog.center();
    }

    /**
     * @see org.opencms.gwt.client.ui.input.form.I_CmsFormDialog#closeDialog()
     */
    public void closeDialog() {

        hide();
    }

    /**
     * Gets the form of this dialog.<p>
     * 
     * @return the form of this dialog 
     */
    public CmsForm getForm() {

        return m_form;
    }

    /**
     * Sets the form handler for this form dialog.<p>
     * 
     * @param formHandler the new form handler 
     */
    public void setFormHandler(I_CmsFormHandler formHandler) {

        m_form.setFormHandler(formHandler);
    }

    /**
     * @see org.opencms.gwt.client.ui.input.form.I_CmsFormDialog#setOkButtonEnabled(boolean)
     */
    public void setOkButtonEnabled(final boolean enabled) {

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            /**
             * @see com.google.gwt.core.client.Scheduler.ScheduledCommand#execute()
             */
            public void execute() {

                // The event handling of GWT gets confused if we don't execute this as a scheduled command 
                m_okButton.setDown(false);
                m_okButton.setEnabled(enabled);
            }
        });

    }

    /**
     * The method which should be called when the user clicks on the cancel button of the dialog.<p>
     */
    protected void onClickCancel() {

        hide();
    }

    /**
     * The method which should be called when the user clicks on the OK button of the dialog.<p>
     */
    protected void onClickOk() {

        m_form.validateAndSubmit();
    }

    /**
     * Creates the cancel button.<p>
     * 
     * @return the cancel button
     */
    private CmsPushButton createCancelButton() {

        CmsPushButton button = new CmsPushButton();
        button.setText(Messages.get().key(Messages.GUI_CANCEL_0));
        button.setUseMinWidth(true);
        button.addClickHandler(new ClickHandler() {

            /**
             * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
             */
            public void onClick(ClickEvent event) {

                onClickCancel();
            }
        });
        return button;
    }

    /**
     * Creates the OK button.<p>
     * 
     * @return the OK button
     */
    private CmsPushButton createOkButton() {

        CmsPushButton button = new CmsPushButton();
        button.setText(Messages.get().key(Messages.GUI_OK_0));
        button.setUseMinWidth(true);
        button.addClickHandler(new ClickHandler() {

            /**
             * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
             */
            public void onClick(ClickEvent event) {

                m_form.validateAndSubmit();
            }
        });
        return button;
    }

    /** 
     * Creates the Reset button.<p>
     * 
     * @return the Reset button
     */
    private CmsPushButton createResetButton() {

        CmsPushButton button = new CmsPushButton();
        button.setText(Messages.get().key(Messages.GUI_RESET_0));
        button.setUseMinWidth(true);
        button.addClickHandler(new ClickHandler() {

            /**
             * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
             */
            public void onClick(ClickEvent event) {

                m_form.reset();
            }
        });
        return button;
    }

}