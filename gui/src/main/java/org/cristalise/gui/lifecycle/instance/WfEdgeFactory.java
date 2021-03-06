/**
 * This file is part of the CRISTAL-iSE default user interface.
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
package org.cristalise.gui.lifecycle.instance;

import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.EdgeFactory;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.instance.WfVertex;

public class WfEdgeFactory implements EdgeFactory
{
    @Override
	public void create
    (
        GraphModelManager           graphModelManager,
        Vertex                      origin,
        Vertex                      terminus,
        TypeNameAndConstructionInfo typeNameAndConstructionInfo
    )
    {
		if ( validCreation( graphModelManager, origin, terminus ) )
			((WfVertex)origin).addNext((WfVertex)terminus);

    }

    private static boolean validCreation( GraphModelManager graphModelManager, Vertex origin, Vertex terminus )
    {
        DirectedEdge[] connectingEdgesAToB = graphModelManager.getModel().getConnectingEdges( origin.getID()  , terminus.getID() );
        DirectedEdge[] connectingEdgesBToA = graphModelManager.getModel().getConnectingEdges( terminus.getID(), origin.getID() );


        return ( origin != terminus ) && ( connectingEdgesAToB.length == 0 ) && ( connectingEdgesBToA.length == 0 );
    }
}

