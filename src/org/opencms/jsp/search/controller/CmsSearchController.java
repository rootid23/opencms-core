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

package org.opencms.jsp.search.controller;

import org.opencms.jsp.search.config.I_CmsSearchConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** The main controller that allows to access all single sub-controllers. */
public class CmsSearchController implements I_CmsSearchControllerMain {

    /** List of all controllers. */
    Collection<I_CmsSearchController> m_controllers;
    /** The controller for the common options. */
    I_CmsSearchControllerCommon m_common;
    /** The controller for the sort options. */
    I_CmsSearchControllerSorting m_sorting;
    /** The controller for the pagination options. */
    I_CmsSearchControllerPagination m_pagination;
    /** The controller for the field facets. */
    I_CmsSearchControllerFacetsField m_fieldFacets;
    /** The controller for the highlighting. */
    I_CmsSearchControllerHighlighting m_highlighting;
    /** The controller for the "Did you mean ...?" feature. */
    I_CmsSearchControllerDidYouMean m_didyoumean;

    /** Constructor that sets up the controller with a given configuration.
     * @param config The search configuration handled by the controller.
     */
    public CmsSearchController(final I_CmsSearchConfiguration config) {

        m_controllers = new ArrayList<I_CmsSearchController>();

        // create the separate controllers and add them to the list of
        // controllers
        m_common = new CmsSearchControllerCommon(config.getGeneralConfig());
        m_controllers.add(m_common);

        m_pagination = new CmsSearchControllerPagination(config.getPaginationConfig());
        m_controllers.add(m_pagination);

        m_sorting = new CmsSearchControllerSorting(config.getSortConfig());
        m_controllers.add(m_sorting);

        m_fieldFacets = new CmsSearchControllerFacetsField(config.getFieldFacetConfigs());
        m_controllers.add(m_fieldFacets);
        if (config.getHighlighterConfig() != null) {
            m_highlighting = new CmsSearchControllerHighlighting(config.getHighlighterConfig());
            m_controllers.add(m_highlighting);
        }
        m_didyoumean = new CmsSearchControllerDidYouMean(
            config.getDidYouMeanConfig(),
            m_common.getConfig().getQueryParam());
        m_controllers.add(m_didyoumean);
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchController#addParametersForCurrentState(java.util.Map)
     */
    @Override
    public void addParametersForCurrentState(final Map<String, String[]> parameters) {

        for (final I_CmsSearchController controller : m_controllers) {
            controller.addParametersForCurrentState(parameters);
        }

    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchController#generateQuery()
     */
    @Override
    public String generateQuery() {

        String query = "";
        final Iterator<I_CmsSearchController> it = m_controllers.iterator();
        query += it.next().generateQuery();
        while (it.hasNext()) {
            final String queryPart = it.next().generateQuery();
            if (!queryPart.isEmpty()) {
                if (!query.isEmpty()) {
                    query += "&";
                }
                query += queryPart;
            }
        }
        // fix for highlighting bug
        if ((getHighlighting() != null) && !(query.contains("df=") || query.contains("type="))) {
            String df = getHighlighting().getConfig().getHightlightField().trim();
            int index = df.indexOf(' ');
            query += "&df=" + (index > 0 ? df.substring(0, index) : df);
        }
        query = query.startsWith("&") ? query.substring(1, query.length()) : query;
        return query;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getCommon()
     */
    @Override
    public I_CmsSearchControllerCommon getCommon() {

        return m_common;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getDidYouMean()
     */
    public I_CmsSearchControllerDidYouMean getDidYouMean() {

        return m_didyoumean;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getFieldFacets()
     */
    @Override
    public I_CmsSearchControllerFacetsField getFieldFacets() {

        return m_fieldFacets;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getHighlighting()
     */
    @Override
    public I_CmsSearchControllerHighlighting getHighlighting() {

        return m_highlighting;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getPagination()
     */
    @Override
    public I_CmsSearchControllerPagination getPagination() {

        return m_pagination;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchControllerMain#getSorting()
     */
    @Override
    public I_CmsSearchControllerSorting getSorting() {

        return m_sorting;
    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchController#updateForQueryChange()
     */
    @Override
    public void updateForQueryChange() {

        for (final I_CmsSearchController controller : m_controllers) {
            controller.updateForQueryChange();
        }

    }

    /**
     * @see org.opencms.jsp.search.controller.I_CmsSearchController#updateFromRequestParameters(java.util.Map)
     */
    @Override
    public void updateFromRequestParameters(final Map<String, String[]> parameters) {

        // Check if query has changed
        final String lastQueryParam = m_common.getConfig().getLastQueryParam();
        final String queryParam = m_common.getConfig().getQueryParam();
        if (!m_common.getConfig().getIgnoreQueryParam()
            && parameters.containsKey(lastQueryParam)
            && parameters.containsKey(queryParam)
            && (parameters.get(lastQueryParam).length > 0)
            && (parameters.get(queryParam).length > 0)
            && !parameters.get(queryParam)[0].equals(parameters.get(lastQueryParam)[0])) {
            updateForQueryChange();
        }
        for (final I_CmsSearchController controller : m_controllers) {
            controller.updateFromRequestParameters(parameters);
        }

    }

}
