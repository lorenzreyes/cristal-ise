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
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.LAST_NUM;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION;

import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.routingHelpers.DataHelperUtility;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptingEngineException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Split extends WfVertex {
    public Vector<String> mErrors;

    public Split() {
        mErrors = new Vector<String>(0, 1);
        setBuiltInProperty(ROUTING_SCRIPT_NAME, "");
        setBuiltInProperty(ROUTING_SCRIPT_VERSION, "");
        setBuiltInProperty(ROUTING_EXPR, "");
    }

    private boolean loopTested;

    /**
     * 
     */
    @Override
    public abstract void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

    /**
     * Method addNext.
     *
     * @param idNext
     */
    void addNext(String idNext) {
        new Next(this, (WfVertex) getParent().search(idNext));
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#addNext(org.cristalise.kernel.lifecycle.instance.WfVertex)
     */
    @Override
    public Next addNext(WfVertex vertex) {
        Next nxt = new Next(this, vertex);
        int num = getOutGraphables().length;

        try {
            num = Integer.parseInt((String) getBuiltInProperty(LAST_NUM));
        }
        catch (Exception e) {
            log.debug("addNext() - Exception:" + e.getMessage());
        }

        nxt.setBuiltInProperty(ALIAS, String.valueOf(num));
        setBuiltInProperty(LAST_NUM, String.valueOf(num + 1));
        return nxt;
    }

    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        log.debug("reinit(id:{}, idLoop:{}) - parent:{}", getID(), idLoop, getParent().getName());

        Vertex[] outVertices = getOutGraphables();
        for (Vertex outVertice : outVertices)
            ((WfVertex) outVertice).reinit(idLoop);
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
     */
    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        int nbInEdgres = getParent().getChildrenGraphModel().getInEdges(this).length;
        if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId()) {
            mErrors.add("not enough previous");
            return false;
        }
        if (nbInEdgres > 1) {
            mErrors.add("Bad nb of previous");
            return false;
        }
        if (getOutEdges().length <= 1 && !(this instanceof Loop)) {
            mErrors.add("not enough next");
            return false;
        }
        if (!(this instanceof Loop)) {
            Vertex[] outV = getOutGraphables();
            Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
            boolean loop = false;
            boolean errInLoop = true;
            for (int i = 0; i < outV.length; i++) {
                for (int j = 0; j < anteVertices.length; j++)
                    if (!loop && outV[i].getID() == anteVertices[j].getID()) {
                        if (outV[i] instanceof Loop) {
                            loop = true;
                            j = anteVertices.length;
                            i = outV.length;
                        }
                        else {
                            errInLoop = false;
                        }
                    }
            }
            if (errInLoop && loop) {
                mErrors.add("Problem in Loop");
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#getErrors()
     */
    @Override
    public String getErrors() {
        if (mErrors.size() == 0)
            return "No error";
        else
            return mErrors.elementAt(0);
    }

    /**
     * 
     */
    @Override
    public void run(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        runNext(agent, itemPath, locker);
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#loop()
     */
    @Override
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0) {
                Vertex[] outVertices = getOutGraphables();
                for (int i = 0; i < outVertices.length; i++) {
                    WfVertex tmp = (WfVertex) getOutGraphables()[i];
                    loop2 = loop2 || tmp.loop();
                }
            }
        }
        loopTested = false;
        return loop2;
    }

    private String[] getRoutingReturnValue(Object value) throws InvalidDataException {
        String stringValue;

        if (value == null) {
            return new String[0];
        }
        else if (value instanceof String) {
            stringValue = (String) value;
        }
        else {
            if (Gateway.getProperties().getBoolean("RoutingScript.enforceStringReturnValue", false))
                throw new InvalidDataException("Routing script or expression must return String");
            else
                stringValue = value.toString();
        }

        return stringValue.split(",");
    }

    protected String[] calculateNexts(ItemPath itemPath, Object locker) throws InvalidDataException {
        String expr = (String) getBuiltInProperty(ROUTING_EXPR);
        String scriptName = (String) getBuiltInProperty(ROUTING_SCRIPT_NAME);
        Integer scriptVersion = deriveVersionNumber(getBuiltInProperty(ROUTING_SCRIPT_VERSION));

        if (StringUtils.isNotBlank(expr)) {
            try {
                Object returnValue = DataHelperUtility.evaluateValue(itemPath, expr, getActContext(), locker);
                return getRoutingReturnValue(returnValue);
            }
            catch (PersistencyException | ObjectNotFoundException e) {
                log.error("", e);
                throw new InvalidDataException("Routing expression evaulation failed: " + expr + " with " + e.getMessage());
            }
        }
        else if (StringUtils.isNotBlank(scriptName)) {
            try {
                Object returnValue = evaluateScript(scriptName, scriptVersion, itemPath, locker);
                return getRoutingReturnValue(returnValue);
            }
            catch (ScriptingEngineException e) {
                log.error("", e);
                throw new InvalidDataException("Error running Routing script " + scriptName + " v" + scriptVersion);
            }
        }
        else
            throw new InvalidDataException("Split is invalid without valid Routing script or expression");
    }

    public String[] nextNames() {
        Vertex[] vs = getOutGraphables();
        String[] result = new String[vs.length];

        for (int i = 0; i < vs.length; i++) result[i] = vs[i].getName();

        return result;
    }

    protected boolean isInTable(String test, String[] list) {
        if (test == null) return false;

        for (String element : list) {
            for (String value: test.split("\\|")) {
                if (value.equals(element)) return true;
            }
        }

        return false;
    }

    @Override
    public void runFirst(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        runNext(agent, itemPath, locker);
    }
}