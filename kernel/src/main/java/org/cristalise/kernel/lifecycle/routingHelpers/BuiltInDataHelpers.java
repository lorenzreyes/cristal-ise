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
package org.cristalise.kernel.lifecycle.routingHelpers;

public enum BuiltInDataHelpers {
    PROPERTY_DH("property"),
    VIEWPOINT_DH("viewpoint"),
    ACTIVITY_DH("activity");

    private String dhName;

    private BuiltInDataHelpers(final String n) {
        dhName = n;
    }

    public String getName() {
        return dhName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInDataHelpers getValue(String name) {
        for (BuiltInDataHelpers prop : BuiltInDataHelpers.values()) {
            if(prop.getName().equals(name) || prop.name().equals(name)) return prop;
        }
        return null;
    }

}
