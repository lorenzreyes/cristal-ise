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
package org.cristalise.kernel.persistency.outcome;

import java.io.IOException;

import org.cristalise.kernel.common.InvalidDataException;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaValidator extends OutcomeValidator {

    @Override
    public synchronized String validate(String outcome) {
        Schema schema = new Schema(outcome);
        try {
            return schema.validate();
        }
        catch (IOException e) {
            return "Error reading schema";
        }
    }

    @Override
    public synchronized String validate(Document outcome) {
        try {
            return validate(Outcome.serialize(outcome, false));
        }
        catch (InvalidDataException e) {
            log.error("Could not validate Outcome", e);
            return null;
        }
    }
}
