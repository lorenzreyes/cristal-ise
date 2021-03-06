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

import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic;


/**
 * Provides mechanism to specify validation Warning. It behaves like any validation rule 
 * (evaluated by the form and when fails it shows a message), except it does NOT invalidate 
 * the form therefore it can be submitted.
 */
@CompileStatic
class Warning {
    /**
     * Javascript regex pattern to validate the value of the field in the from
     */
    String pattern = null
    /**
     * Javascript expression (code fragment) to validate the value of the field in the form
     */
    String expression = null
    /**
     * Message to show to the user for the validation warning 
     */
    String message = null

    public void setPattern(String p) {
        if (pattern && expression) throw new InvalidDataException("Do not use pattern and expression at the same time")

        pattern = p
    }

    public void setExpression(String e) {
        if (pattern && expression) throw new InvalidDataException("Do not use pattern and expression at the same time")

        expression = e
    }
}
