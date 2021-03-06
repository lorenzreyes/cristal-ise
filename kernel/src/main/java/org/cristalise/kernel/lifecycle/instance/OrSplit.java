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
package org.cristalise.kernel.lifecycle.instance;

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrSplit extends Split {

    public OrSplit() {
        super();
    }

    @Override
    public void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        int id = getID();
        String[] nextsTab = calculateNexts(itemPath, locker);

        int active = 0;
        DirectedEdge[] outEdges = getOutEdges();
        for (String thisNext : nextsTab) {
            log.debug("runNext(id:{}) - Finding edge with {} '{}'", id, ALIAS, thisNext);

            if (thisNext != null) {
                for (DirectedEdge outEdge : outEdges) {
                    Next nextEdge = (Next) outEdge;
                    if (thisNext.equals(nextEdge.getBuiltInProperty(ALIAS))) {
                        WfVertex term = nextEdge.getTerminusVertex();
                        try {
                            term.run(agent, itemPath, locker);
                        }
                        catch (InvalidDataException e) {
                            log.error("", e);
                            throw new InvalidDataException("Error enabling next " + thisNext);
                        }
                        log.debug("runNext(id:{}) - Running {}", id, nextEdge.getBuiltInProperty(ALIAS));
                        active++;
                    }
                }
            }
        }

        // if no active nexts throw exception
        if (active == 0)
            throw new InvalidDataException("No edges found, no next vertex activated! (id: " + id + ")");
    }

}
