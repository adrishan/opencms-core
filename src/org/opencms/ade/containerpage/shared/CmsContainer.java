/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.ade.containerpage.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Container bean.<p>
 * 
 * @since 8.0.0
 */
public class CmsContainer implements IsSerializable {

    /** Key for container data. This has to be identical with {@link org.opencms.jsp.CmsJspTagContainer#KEY_CONTAINER_DATA}. */
    public static final String KEY_CONTAINER_DATA = "org_opencms_ade_containerpage_containers";

    /** List of the contained elements id's. */
    private List<CmsContainerElement> m_elements;

    /** The maximum number of elements. */
    private int m_maxElements;

    /** The container name. */
    private String m_name;

    /** The container type. */
    private String m_type;

    /** The width of the container. */
    private int m_width;

    /**
     * Constructor.<p>
     * 
     * @param name the container name, also used as id within a container-page
     * @param type the container type
     * @param width the width of the container 
     * @param maxElements the maximum number of elements displayed by this container
     * @param elements the container elements id's
     */
    public CmsContainer(String name, String type, int width, int maxElements, List<CmsContainerElement> elements) {

        m_elements = elements;
        m_name = name;
        m_type = type;
        m_maxElements = maxElements;
        m_width = width;
    }

    /**
     * Hidden default constructor (for GWT serialization).<p>
     */
    protected CmsContainer() {

        // do nothing 
    }

    /**
     * Returns the list of the contained elements id's.<p>
     * 
     * @return the list of the contained elements id's
     */
    public List<CmsContainerElement> getElements() {

        return m_elements;
    }

    /**
     * Returns the maximum number of elements allowed in this container.<p>
     * 
     * @return the maximum number of elements allowed in this container
     */
    public int getMaxElements() {

        return m_maxElements;
    }

    /**
     * Returns the container name, also used as HTML-id for the container DOM-element. Has to be unique within the template.<p>
     * 
     * @return the container name
     */
    public String getName() {

        return m_name;
    }

    /**
     * Returns the container type. Used to determine the formatter used to render the contained elements.<p>
     * 
     * @return the container type
     */
    public String getType() {

        return m_type;
    }

    /**
     * Returns the container width.<p>
     * 
     * @return the container width 
     */
    public int getWidth() {

        return m_width;
    }

    /**
     * Sets the elements contained in this container.<p>
     * 
     * @param elements the elements
     */
    public void setElements(List<CmsContainerElement> elements) {

        m_elements = elements;

    }

    /**
     * Sets the maxElements.<p>
     *
     * @param maxElements the maxElements to set
     */
    public void setMaxElements(int maxElements) {

        m_maxElements = maxElements;
    }

    /**
     * Sets the name.<p>
     *
     * @param name the name to set
     */
    public void setName(String name) {

        m_name = name;
    }

    /**
     * Sets the type.<p>
     *
     * @param type the type to set
     */
    public void setType(String type) {

        m_type = type;
    }

}
