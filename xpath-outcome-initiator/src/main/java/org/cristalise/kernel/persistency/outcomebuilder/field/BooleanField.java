/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.persistency.outcomebuilder.field;

import org.cristalise.kernel.utils.Logger;

public class BooleanField extends StringField {

    Boolean checkbox;

    public BooleanField() {
        checkbox = false;
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "SWITCH";
    }

    @Override
    public String getText() {
        return checkbox.toString();
    }

    @Override
    public void setText(String text) {
        boolean newState = false;
        try {
            newState = Boolean.valueOf(text).booleanValue();
        } catch (Exception ex) {
            Logger.error("Invalid value for checkbox: "+text);
        }
        checkbox = newState;
    }

    @Override
    public String getDefaultValue() {
        return "false";
    }
}
