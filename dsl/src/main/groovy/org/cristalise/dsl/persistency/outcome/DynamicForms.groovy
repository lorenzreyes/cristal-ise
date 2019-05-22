/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.persistency.outcome

import java.util.regex.Pattern

import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic;


@CompileStatic
class DynamicForms {
    Boolean disabled = null
    String errmsg = null
    Boolean hidden = null
    String inputType = null
    String label = null
    String mask = null
    Integer max = null
    Integer min = null
    Boolean multiple = null
    String pattern = null
    String placeholder = null
    Boolean required = null
    Boolean showSeconds = null
    String type = null
    String value = null
    
    /**
     * Defines a pattern and message that is used by the UI to show a warning message
     */
    OutOfSpecs outOfSpecs = null

    /**
     * Sets the width of the form
     */
    String width = null

    /**
     * Defines the Script name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated
     */
    String updateScriptRef = null
    /**
     * Defines the Query name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated
     */
    String updateQuerytRef = null

    /**
     * Number of digits that are present in the number. Possible value are: P, P-
     */
    String precision = null
    /**
     * Number of decimal places that are present in the number.Possible value are: S, S-
     */
    String scale = null

    /**
     * 
     * @param p
     */
    public void setPrecision(String p) {
        if (!(p ==~ /^\d+[-]?$/)) throw new InvalidDataException("Invalid precision value ("+p+"). Value should be '5' or '5-'")
        precision = p
    }

    /**
     * 
     * @param s
     */
    public void setScale(String s) {
        if (!(s ==~ /^\d+[-]?$/)) throw new InvalidDataException("Invalid scale value ("+s+"). Value should be '5' or '5-'")
        scale = s
    }
}
