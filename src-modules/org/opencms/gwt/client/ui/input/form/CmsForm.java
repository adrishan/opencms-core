/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/client/ui/input/form/Attic/CmsForm.java,v $
 * Date   : $Date: 2010/06/15 12:34:30 $
 * Version: $Revision: 1.12 $
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

import org.opencms.gwt.client.ui.css.I_CmsInputCss;
import org.opencms.gwt.client.ui.css.I_CmsInputLayoutBundle;
import org.opencms.gwt.client.ui.input.I_CmsFormField;
import org.opencms.gwt.client.ui.input.I_CmsFormWidget;
import org.opencms.gwt.client.ui.input.I_CmsHasBlur;
import org.opencms.gwt.client.validation.CmsValidationController;
import org.opencms.gwt.client.validation.I_CmsValidationHandler;
import org.opencms.gwt.shared.CmsValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * This class acts as a container for form fields.<p>
 * 
 * It is also responsible for collecting and validating the values of the form fields.
 * 
 * @author Georg Westenberger
 * 
 * @version $Revision: 1.12 $
 * 
 * @since 8.0.0
 * 
 */
public class CmsForm extends Composite {

    /** The CSS bundle used for this form. **/
    private static final I_CmsInputCss CSS = I_CmsInputLayoutBundle.INSTANCE.inputCss();

    /** A map from field ids to the corresponding widgets. */
    protected Map<String, I_CmsFormField> m_fields = new LinkedHashMap<String, I_CmsFormField>();

    /** A reference to the dialog this form is contained in. */
    protected I_CmsFormDialog m_formDialog;

    /** The form handler. */
    protected I_CmsFormHandler m_formHandler;

    /** A flag which indicates whether the user has pressed enter in a widget. */
    protected boolean m_pressedEnter;

    /** The initial values of the form fields. */
    private Map<String, String> m_initialValues = new HashMap<String, String>();

    /** The main panel for this widget. */
    private FlowPanel m_panel = new FlowPanel();

    /** 
    private boolean m_isSubmittable;

    /** The list of form reset handlers. */
    private List<I_CmsFormResetHandler> m_resetHandlers = new ArrayList<I_CmsFormResetHandler>();

    /**
     * The default constructor.<p>
     * 
     */
    public CmsForm() {

        initWidget(m_panel);
        m_panel.addStyleName(CSS.form());

    }

    /**
     * Adds a form field to the form.<p>
     * 
     * @param formField the form field which should be added
     */
    @SuppressWarnings("unchecked")
    public void addField(final I_CmsFormField formField) {

        String initialValue = formField.getWidget().getFormValueAsString();
        m_initialValues.put(formField.getId(), initialValue);
        String description = formField.getDescription();
        String labelText = formField.getLabel();
        final I_CmsFormWidget widget = formField.getWidget();
        if (widget instanceof HasValueChangeHandlers) {
            ((HasValueChangeHandlers<String>)widget).addValueChangeHandler(new ValueChangeHandler<String>() {

                /**
                 * @see com.google.gwt.event.logical.shared.ValueChangeHandler#onValueChange(ValueChangeEvent event) 
                 */
                public void onValueChange(ValueChangeEvent<String> event) {

                    formField.setValidationStatus(I_CmsFormField.ValidationStatus.unknown);

                    // if the user presses enter, the keypressed event is fired before the change event,
                    // so we use a flag to keep track of whether enter was pressed.
                    if (!m_pressedEnter) {
                        validateField(formField);
                    } else {
                        validateAndSubmit();
                    }
                }
            });
        }

        if (widget instanceof HasKeyPressHandlers) {
            ((HasKeyPressHandlers)widget).addKeyPressHandler(new KeyPressHandler() {

                /**
                 * @see com.google.gwt.event.dom.client.KeyPressHandler#onKeyPress(com.google.gwt.event.dom.client.KeyPressEvent)
                 */
                public void onKeyPress(KeyPressEvent event) {

                    int keyCode = event.getNativeEvent().getKeyCode();
                    if (keyCode == KeyCodes.KEY_ENTER) {
                        m_pressedEnter = true;
                        if (widget instanceof I_CmsHasBlur) {
                            // force a blur because not all browsers send a change event if the user just presses enter in a field
                            ((I_CmsHasBlur)widget).blur();

                        }
                        // make sure that the flag is set to false again after the other events have been processed 
                        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                            /**
                             * @see com.google.gwt.core.client.Scheduler.ScheduledCommand#execute()
                             */
                            public void execute() {

                                m_pressedEnter = false;
                            }
                        });
                    }
                }

            });
        }

        m_fields.put(formField.getId(), formField);
        addRow(labelText, description, (Widget)widget);
    }

    /**
     * Adds a form field to the form and sets its initial value.<p>
     * 
     * @param formField the form field which should be added 
     * @param initialValue the initial value of the form field, or null if the field shouldn't have an initial value 
     */
    public void addField(I_CmsFormField formField, String initialValue) {

        if (initialValue != null) {
            formField.getWidget().setFormValueAsString(initialValue);
        }
        addField(formField);
    }

    /**
     * Adds a text label.<p>
     * 
     * @param labelText the text for the label
     * @return a label with the given text
     */
    public Label addLabel(String labelText) {

        Label label = new Label(labelText);
        label.setStyleName(CSS.formDescriptionLabel());
        m_panel.add(label);
        return label;
    }

    /** 
     * Adds a new form reset handler to the form.<p>
     * 
     * @param handler the new form reset handler 
     */
    public void addResetHandler(I_CmsFormResetHandler handler) {

        m_resetHandlers.add(handler);
    }

    /**
     * Adds a new row with a given label and input widget to the form.<p>
     * 
     * @param labelText the label text for the form field
     * @param description the description of the form field 
     * @param widget the widget for the form field 
     *  
     * @return the newly added form row 
     */
    public CmsFormRow addRow(String labelText, String description, Widget widget) {

        CmsFormRow row = new CmsFormRow();
        Label label = row.getLabel();
        label.setText(labelText);
        label.setTitle(description);
        row.getWidgetContainer().add(widget);
        m_panel.add(row);
        return row;
    }

    /**
     * Adds a separator below the last added form field.<p>
     * 
     */
    public void addSeparator() {

        m_panel.add(new CmsSeparator());
    }

    /**
     * Collects all values from the form fields.<p>
     * 
     * This method omits form fields whose values are null.
     * 
     * @return a map of the form field values 
     */
    public Map<String, String> collectValues() {

        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, I_CmsFormField> entry : m_fields.entrySet()) {
            String key = entry.getKey();
            String value = null;
            I_CmsFormField field = entry.getValue();
            I_CmsFormWidget widget = field.getWidget();
            value = widget.getFormValueAsString();
            result.put(key, value);
        }
        return result;
    }

    /**
     * Performs an initial validation of all form fields.<p>
     */
    public void doInitialValidation() {

        CmsValidationController validationController = new CmsValidationController(
            m_fields.values(),
            createValidationHandler());
        validationController.startValidation();
    }

    /**
     * Returns the form field with a given id.<p>
     * 
     * @param id the id of the form field 
     * 
     * @return the form field with the given id, or null if no field was found 
     */
    public I_CmsFormField getField(String id) {

        return m_fields.get(id);
    }

    /**
     * Returns true if none of the fields in a collection are marked as invalid.<p>
     *
     * @param fields the form fields
     * 
     * @return true if none of the fields are invalid 
     */
    public boolean noFieldsInvalid(Collection<I_CmsFormField> fields) {

        for (I_CmsFormField field : fields) {
            if (field.getValidationStatus().equals(I_CmsFormField.ValidationStatus.invalid)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets all form fields to their initial values.<p>
     */
    public void reset() {

        for (Map.Entry<String, I_CmsFormField> entry : m_fields.entrySet()) {
            String id = entry.getKey();
            I_CmsFormField field = entry.getValue();
            field.setValidationStatus(I_CmsFormField.ValidationStatus.unknown);
            field.getWidget().setFormValueAsString(m_initialValues.get(id));
            field.getWidget().setErrorMessage(null);
        }
        m_formDialog.setOkButtonEnabled(noFieldsInvalid(m_fields.values()));
        for (I_CmsFormResetHandler resetHandler : m_resetHandlers) {
            resetHandler.onResetForm();
        }
    }

    /**
     * Sets the form dialog in which this form is being used.<p>
     * 
     * @param dialog the form dialog 
     */
    public void setFormDialog(I_CmsFormDialog dialog) {

        m_formDialog = dialog;
    }

    /**
     * Sets the form handler for this form.<p>
     * 
     * @param handler the form handler 
     */
    public void setFormHandler(I_CmsFormHandler handler) {

        m_formHandler = handler;
    }

    /**
     * Validates the form fields and submits their values if the validation was successful.<p>
     */
    public void validateAndSubmit() {

        CmsValidationController validationController = new CmsValidationController(
            m_fields.values(),
            new I_CmsValidationHandler() {

                /**
                 * @see org.opencms.gwt.client.validation.I_CmsValidationHandler#onValidationFinished(boolean)
                 */
                public void onValidationFinished(boolean ok) {

                    if (ok) {
                        m_formDialog.closeDialog();
                        m_formHandler.onSubmitForm(collectValues());

                    } else {
                        m_formDialog.setOkButtonEnabled(noFieldsInvalid(m_fields.values()));
                    }
                }

                /**
                 * @see org.opencms.gwt.client.validation.I_CmsValidationHandler#onValidationResult(java.lang.String, org.opencms.gwt.shared.CmsValidationResult)
                 */
                public void onValidationResult(String field, CmsValidationResult result) {

                    updateFieldValidationStatus(field, result);

                }

            });
        validationController.startValidation();

    }

    /**
     * Validates a single field.<p>
     * 
     * @param field the field to validate 
     */
    public void validateField(final I_CmsFormField field) {

        CmsValidationController validationController = new CmsValidationController(field, createValidationHandler());
        validationController.startValidation();
    }

    /**
     * Applies a validation result to a form field.<p>
     * 
     * @param fieldId the field id to which the validation result should be applied 
     * @param result the result of the validation operation 
     */
    protected void updateFieldValidationStatus(String fieldId, CmsValidationResult result) {

        I_CmsFormField field = m_fields.get(fieldId);
        if (result.hasNewValue()) {
            field.getWidget().setFormValueAsString(result.getNewValue());
        }
        String errorMessage = result.getErrorMessage();
        field.getWidget().setErrorMessage(result.getErrorMessage());
        field.setValidationStatus(errorMessage == null
        ? I_CmsFormField.ValidationStatus.valid
        : I_CmsFormField.ValidationStatus.invalid);
    }

    /**
     * Creates a validation handler which updates the OK button state when validation results come in.<p>
     * 
     * @return a validation handler 
     */
    private I_CmsValidationHandler createValidationHandler() {

        return new I_CmsValidationHandler() {

            /**
             * @see org.opencms.gwt.client.validation.I_CmsValidationHandler#onValidationFinished(boolean)
             */
            public void onValidationFinished(boolean ok) {

                m_formDialog.setOkButtonEnabled(noFieldsInvalid(m_fields.values()));
            }

            /**
             * @see org.opencms.gwt.client.validation.I_CmsValidationHandler#onValidationResult(java.lang.String, org.opencms.gwt.shared.CmsValidationResult)
             */
            public void onValidationResult(String fieldId, CmsValidationResult result) {

                updateFieldValidationStatus(fieldId, result);
            }
        };
    }
}