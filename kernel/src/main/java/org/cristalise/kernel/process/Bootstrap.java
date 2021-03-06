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
package org.cristalise.kernel.process;

import static org.cristalise.kernel.property.BuiltInItemProperties.KERNEL_VERSION;
import static org.cristalise.kernel.property.BuiltInItemProperties.MODULE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import lombok.extern.slf4j.Slf4j;


/**
 * Bootstrap loads all Items defined in the kernel resource XMLs and the module XML
 */
@Slf4j
public class Bootstrap
{
    static DomainPath thisServerPath;
    static HashMap<String, AgentProxy> systemAgents = new HashMap<String, AgentProxy>();
    public static boolean shutdown = false;
    static StateMachine predefSM;

    /**
     * Get the StateMachine of a Predefined Step
     * 
     * @return the fully initialised StateMachine DescriptionObject
     * @throws ObjectNotFoundException StateMachine called 'PredefinedStep' version '0' does not exists
     * @throws InvalidDataException the stored state machine data was invalid
     */
    public static StateMachine getPredefSM() throws ObjectNotFoundException, InvalidDataException {
        if (predefSM == null) predefSM = LocalObjectLoader.getStateMachine("PredefinedStep", 0);

        return predefSM;
    }
    
    /**
     * Initialise Bootstrap
     * 
     * @throws Exception in case of any error
     */
    static void init() throws Exception {
        getPredefSM();

        // check for system agents
        checkAdminAgents();

        // create the server's mother item
        ItemPath serverItem = createServerItem();

        // store system properties in server item
        storeSystemProperties(serverItem);
    }

    /**
     * Run everything without timing-out the service wrapper
     */
    static void run() throws Exception {
        init();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("Bootstrapper");

                    log.info("run() - Bootstrapper started");

                    ClassLoader wClassLoader = Bootstrap.class.getClassLoader();
                    log.info("run() setContextClassLoader=[{}]", wClassLoader);
                    Thread.currentThread().setContextClassLoader(wClassLoader);

                    // make sure all of the boot items are up-to-date
                    if (!shutdown) {
                        log.info("run() - Verifying kernel boot data items");
                        verifyBootDataItems();
                    }

                    // verify the server item's wf
                    if (!shutdown) {
                        log.info("run() - Initialising Server Item Workflow");
                        initServerItemWf();
                    }

                    if (!shutdown) {
                        Gateway.getModuleManager().setUser(systemAgents.get(SYSTEM_AGENT.getName()));
                        Gateway.getModuleManager().registerModules();
                    }

                    if (!shutdown) {
                        log.info("run() - Bootstrapper complete");
                        Gateway.getModuleManager().runScripts("initialized");

                        if (Gateway.getLookupManager() != null) Gateway.getLookupManager().postBoostrap();
                        Gateway.getStorage().postBoostrap();
                    }
                }
                catch (Throwable e) {
                    log.error("Exception performing bootstrap. Check that everything is OK.", e);
                    AbstractMain.shutdown(1);
                }
            }
        }).start();
    }

    /**
     * Set flag for the thread to abort gracefully
     */
    public static void abort() {
        shutdown = true;
    }

    /**
     * Checks all kernel descriptions stored in resources and create or update them if they were changed
     */
    public static void verifyBootDataItems() throws Exception {
        String bootItems;
        log.info("verifyBootDataItems() - Start checking kernel descriptions ...");

        bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));

        verifyBootDataItems(bootItems, null, true);

        log.info("verifyBootDataItems() - DONE.");
    }

    /**
     *
     * @param bootList
     * @param ns
     * @param reset
     * @throws InvalidItemPathException
     */
    private static void verifyBootDataItems(String bootList, String ns, boolean reset) throws InvalidItemPathException {
        StringTokenizer str = new StringTokenizer(bootList, "\n\r");

        while (str.hasMoreTokens() && !shutdown) {
            String thisItem = str.nextToken();
            String[] idFilename = thisItem.split(",");
            String id = idFilename[0], filename = idFilename[1];
            ItemPath itemPath = new ItemPath(id);
            String[] fileParts = filename.split("/");
            String itemType = fileParts[0], itemName = fileParts[1];

            try {
                String location = "boot/"+filename+(itemType.equals("OD")?".xsd":".xml");
                verifyResource(ns, itemName, 0, itemType, itemPath, location, reset);
            }
            catch (Exception e) {
                log.error("Error importing bootstrap items. Unsafe to continue.", e);
                AbstractMain.shutdown(1);
            }
        }
    }


    /**
     * Create a resource item from its module definition. The item should not exist.
     */
    public static DomainPath createResource(String ns, String itemName, int version, String itemType, Set<Outcome> outcomes, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemType, null, outcomes, null, reset);
    }

    /**
     * Verify a resource item against a module version, using a ResourceImportHandler configured
     * to find outcomes at the given dataLocation
     */
    public static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, String dataLocation, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemType, itemPath, null, dataLocation, reset);
    }

    /**
     * Verify a resource item against a module version, but supplies the resource outcomes directly
     * instead of through a location lookup
     */
    public static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, Set<Outcome> outcomes, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemType, itemPath, outcomes, null, reset);
    }

    /**
     *
     * @param ns
     * @param itemName
     * @param version
     * @param itemType
     * @param itemPath
     * @param outcomes
     * @param dataLocation
     * @param reset
     * @return the Path of the resource either created or initialised from existing data
     * @throws Exception
     */
    private static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, Set<Outcome> outcomes, String dataLocation, boolean reset)
            throws Exception
    {
        ResourceImportHandler typeImpHandler = Gateway.getResourceImportHandler(BuiltInResources.getValue(itemType));

        log.info("verifyResource() - Verifying "+typeImpHandler.getName()+" "+ itemName+" v"+version);

        // Find or create Item for Resource
        ItemProxy thisProxy;
        DomainPath modDomPath = typeImpHandler.getPath(itemName, ns);

        if (modDomPath.exists()) {
           log.info("verifyResource() - Found "+typeImpHandler.getName()+" "+itemName + ".");

            thisProxy = verifyPathAndModuleProperty(ns, itemType, itemName, itemPath, modDomPath, modDomPath);
        }
        else {
            if (itemPath == null) itemPath = new ItemPath();

            log.info("verifyResource() - "+typeImpHandler.getName()+" "+itemName+" not found. Creating new.");

            thisProxy = createResourceItem(typeImpHandler, itemName, ns, itemPath);
        }

        // Verify/Import Outcomes, creating events and views as necessary
        if (outcomes == null || outcomes.size() == 0) {
            outcomes = typeImpHandler.getResourceOutcomes(itemName, ns, dataLocation, version);
        }

        if (outcomes.size() == 0) log.warn("verifyResource() - no Outcome found therefore nothing stored!");

        for (Outcome newOutcome : outcomes) {
            if (checkToStoreOutcomeVersion(thisProxy, newOutcome, version, reset)) {
                // validate it, but not for kernel objects (ns == null) because those are to validate the rest
                if (ns != null) newOutcome.validateAndCheck();

                storeOutcomeEventAndViews(thisProxy.getPath(), newOutcome, version);

                CollectionArrayList cols = typeImpHandler.getCollections(itemName, version, newOutcome);

                for (Collection<?> col : cols.list) {
                    Gateway.getStorage().put(thisProxy.getPath(), col, null);
                    Gateway.getStorage().clearCache(thisProxy.getPath(), ClusterType.COLLECTION+"/"+col.getName());
                    col.setVersion(null);
                    Gateway.getStorage().put(thisProxy.getPath(), col, null);
                }
            }
        }
        Gateway.getStorage().commit(null);
        return modDomPath;
    }

    /**
     * Verify module property and location
     *
     * @param ns
     * @param itemType
     * @param itemName
     * @param itemPath
     * @param modDomPath
     * @param path
     * @return the ItemProxy either create or initialised for existing
     * @throws Exception
     */
    private static ItemProxy verifyPathAndModuleProperty(String ns, String itemType, String itemName, ItemPath itemPath, DomainPath modDomPath, DomainPath path)
            throws Exception
    {
        LookupManager lookupManager = Gateway.getLookupManager();
        ItemProxy thisProxy = Gateway.getProxyManager().getProxy(path);

        if (itemPath != null && !path.getItemPath().equals(itemPath)) {
            log.warn("Resource "+itemType+"/"+itemName+" should have path "+itemPath+" but was found with path "+path.getItemPath());
            itemPath = path.getItemPath();
        }

        if (itemPath == null) itemPath = path.getItemPath();

        String moduleName = (ns==null?"kernel":ns);
        String itemModule;
        try {
            itemModule = thisProxy.getProperty("Module");
            if (itemModule != null && !itemModule.equals("") && !itemModule.equals("null") && !moduleName.equals(itemModule)) {
                String error = "Module clash! Resource '"+itemName+"' included in module "+moduleName+" but is assigned to '"+itemModule + "'.";
                log.error(error);
                throw new InvalidDataException(error);
            }
        }
        catch (ObjectNotFoundException ex) {
            itemModule = "";
        }

        if (!modDomPath.equals(path)) {	 // move item to module subtree
            log.info("Module item "+itemName+" found with path "+path.toString()+". Moving to "+modDomPath.toString());
            modDomPath.setItemPath(itemPath);

            if (!modDomPath.exists()) lookupManager.add(modDomPath);
            lookupManager.delete(path);
        }
        return thisProxy;
    }

    /**
     *
     * @param item
     * @param newOutcome
     * @param version
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    private static void storeOutcomeEventAndViews(ItemPath item, Outcome newOutcome, Integer version)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        log.info("storeOutcomeEventAndViews() - Writing new " + newOutcome.getSchema().getName() + " v" + version + " to "+item.getName());

        History hist = new History(item, null);

        String viewName = "";
        if (version != null) viewName = String.valueOf(version);

        int eventID = hist.addEvent( systemAgents.get(SYSTEM_AGENT.getName()).getPath(), null,
                ADMIN_ROLE.getName(), "Bootstrap", "Bootstrap", "Bootstrap",
                newOutcome.getSchema(), getPredefSM(), PredefinedStep.DONE, version != null ? viewName : "last"
                ).getID();

        newOutcome.setID(eventID);

        Viewpoint newLastView = new Viewpoint(item, newOutcome.getSchema(), "last", eventID);

        Gateway.getStorage().put(item, newOutcome,  null);
        Gateway.getStorage().put(item, newLastView, null);

        if (version != null) {
            Viewpoint newNumberView = new Viewpoint(item, newOutcome.getSchema(), viewName, eventID);
            Gateway.getStorage().put(item, newNumberView, null);
        }
    }

    /**
     *
     * @param item
     * @param newOutcome
     * @param version
     * @param reset
     * @return true i the data was changed, since the last Bootstrap run
     * @throws PersistencyException
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private static boolean checkToStoreOutcomeVersion(ItemProxy item, Outcome newOutcome, int version, boolean reset)
            throws PersistencyException, InvalidDataException, ObjectNotFoundException
    {
        Schema schema = newOutcome.getSchema();
        try {
            Viewpoint currentData = (Viewpoint) item.getObject(ClusterType.VIEWPOINT+"/"+newOutcome.getSchema().getName()+"/"+version);

            if (newOutcome.isIdentical(currentData.getOutcome())) {
                log.debug("checkToStoreOutcomeVersion() - Data identical, no update required");
                return false;
            }
            else {
                if (!reset  && !currentData.getEvent().getStepPath().equals("Bootstrap")) {
                    log.info("checkToStoreOutcomeVersion() - Version " + version + " was not set by Bootstrap, and reset not requested. Not overwriting.");
                    return false;
                }
            }
        }
        catch (ObjectNotFoundException ex) {
            log.info("checkToStoreOutcomeVersion() - "+schema.getName()+" "+item.getName()+" v"+version+" not found! Attempting to insert new.");
        }
        return true;
    }

    /**
     *
     * @param impHandler
     * @param itemName
     * @param ns
     * @param itemPath
     * @return the ItemProxy representing the newly create Item
     * @throws Exception
     */
    private static ItemProxy createResourceItem(ResourceImportHandler impHandler, String itemName, String ns, ItemPath itemPath)
            throws Exception
    {
        // create props
        PropertyDescriptionList pdList = impHandler.getPropDesc();
        PropertyArrayList props = new PropertyArrayList();
        LookupManager lookupManager = Gateway.getLookupManager();

        for (int i = 0; i < pdList.list.size(); i++) {
            PropertyDescription pd = pdList.list.get(i);

            String propName = pd.getName();
            String propVal  = pd.getDefaultValue();

            if (propName.equals(NAME.toString()))        propVal = itemName;
            else if (propName.equals(MODULE.toString())) propVal = (ns == null) ? "kernel" : ns;

            props.list.add(new Property(propName, propVal, pd.getIsMutable()));
        }

        CompositeActivity ca = new CompositeActivity();
        try {
            ca = (CompositeActivity) ((CompositeActivityDef)LocalObjectLoader.getActDef(impHandler.getWorkflowName(), 0)).instantiate();
        }
        catch (ObjectNotFoundException ex) {
            log.error("Module resource workflow "+impHandler.getWorkflowName()+" not found. Using empty.", ex);
        }

        Gateway.getCorbaServer().createItem(itemPath);
        lookupManager.add(itemPath);
        DomainPath newDomPath = impHandler.getPath(itemName, ns);
        newDomPath.setItemPath(itemPath);
        lookupManager.add(newDomPath);
        ItemProxy newItemProxy = Gateway.getProxyManager().getProxy(itemPath);
        newItemProxy.initialise( systemAgents.get(SYSTEM_AGENT.getName()).getPath(), props, ca, null);
        return newItemProxy;
    }

    /**
     * Checks for the existence of a agents and creates it if needed so it can be used
     *
     * @param name the name of the agent
     * @param pass the password of the agent
     * @param rolePath the role of the agent
     * @param uuid the UUID os the agent
     * @return the Proxy representing the Agent
     * @throws Exception any exception found
     */
    private static AgentProxy checkAgent(String name, String pass, RolePath rolePath, String uuid) throws Exception {
        log.info("checkAgent() - Checking for existence of '"+name+"' agent.");
        LookupManager lookup = Gateway.getLookupManager();

        try {
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(lookup.getAgentPath(name));
            systemAgents.put(name, agentProxy);
           log.info("checkAgent() - Agent '"+name+"' found.");
            return agentProxy;
        }
        catch (ObjectNotFoundException ex) { }

        log.info("checkAgent() - Agent '"+name+"' not found. Creating.");

        try {
            AgentPath agentPath = new AgentPath(new ItemPath(uuid), name);

            Gateway.getCorbaServer().createAgent(agentPath);
            lookup.add(agentPath);

            if (StringUtils.isNotBlank(pass)) lookup.setAgentPassword(agentPath, pass);

            // assign role
            log.info("checkAgent() - Assigning role '"+rolePath.getName()+"'");
            Gateway.getLookupManager().addRole(agentPath, rolePath);
            Gateway.getStorage().put(agentPath, new Property(NAME, name, true), null);
            Gateway.getStorage().put(agentPath, new Property(TYPE, "Agent", false), null);
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(agentPath);
            //TODO: properly init agent here with wf, props and colls -> use CreatItemFromDescription
            systemAgents.put(name, agentProxy);
            return agentProxy;
        }
        catch (Exception ex) {
            log.error("Unable to create '"+name+"' Agent.", ex);
            throw ex;
        }
    }

    /**
     * 
     * @throws Exception
     */
    public static void checkAdminAgents() throws Exception {
        RolePath rootRole = new RolePath();
        if (!rootRole.exists()) Gateway.getLookupManager().createRole(rootRole);

        // check for admin role
        RolePath adminRole = new RolePath(rootRole, ADMIN_ROLE.getName(), false);
        if (!adminRole.exists()) Gateway.getLookupManager().createRole(adminRole);
        Gateway.getLookupManager().setPermission(adminRole, "*");

        // check for import Agent
        AgentProxy system = checkAgent(SYSTEM_AGENT.getName(), null, adminRole, new UUID(0, 1).toString());
        ScriptConsole.setUser(system);

        String ucRole = Gateway.getProperties().getString("UserCode.roleOverride", UserCodeProcess.DEFAULT_ROLE);

        // check for local usercode user & role
        RolePath usercodeRole = new RolePath(rootRole, ucRole, true);
        if (!usercodeRole.exists()) Gateway.getLookupManager().createRole(usercodeRole);
        checkAgent(
                Gateway.getProperties().getString(ucRole + ".agent",     InetAddress.getLocalHost().getHostName()),
                Gateway.getProperties().getString(ucRole + ".password", "uc"),
                usercodeRole,
                UUID.randomUUID().toString());
    }

    private static ItemPath createServerItem() throws Exception {
        LookupManager lookupManager = Gateway.getLookupManager();
        String serverName = Gateway.getProperties().getString("ItemServer.name", InetAddress.getLocalHost().getHostName());
        thisServerPath = new DomainPath("/servers/"+serverName);
        ItemPath serverItem;
        try {
            serverItem = thisServerPath.getItemPath();
        }
        catch (ObjectNotFoundException ex) {
            log.info("Creating server item "+thisServerPath);
            serverItem = new ItemPath();
            Gateway.getCorbaServer().createItem(serverItem);
            lookupManager.add(serverItem);
            thisServerPath.setItemPath(serverItem);
            lookupManager.add(thisServerPath);
        }

        int proxyPort = Gateway.getProperties().getInt("ItemServer.Proxy.port", 1553);

        Gateway.getStorage().put(serverItem, new Property(NAME,            serverName,                              false), null);
        Gateway.getStorage().put(serverItem, new Property(TYPE,            "Server",                                false), null);
        Gateway.getStorage().put(serverItem, new Property(KERNEL_VERSION,  Gateway.getKernelVersion(),              true),  null);
        Gateway.getStorage().put(serverItem, new Property("ProxyPort",     String.valueOf(proxyPort),               false), null);
        Gateway.getStorage().put(serverItem, new Property("ConsolePort",   String.valueOf(Logger.getConsolePort()), true),  null);

        Gateway.getProxyManager().connectToProxyServer(serverName, proxyPort);

        return serverItem;
    }

    private static void storeSystemProperties(ItemPath serverItem) throws Exception {
        Outcome newOutcome = Gateway.getProperties().convertToOutcome("ItemServer");
        storeOutcomeEventAndViews(serverItem, newOutcome, null);
    }

    public static void initServerItemWf() throws Exception {
        CompositeActivityDef serverWfCa = (CompositeActivityDef)LocalObjectLoader.getActDef("ServerItemWorkflow", 0);
        Workflow wf = new Workflow((CompositeActivity)serverWfCa.instantiate(), new ServerPredefinedStepContainer());
        wf.initialise(thisServerPath.getItemPath(), systemAgents.get(SYSTEM_AGENT.getName()).getPath(), null);
        Gateway.getStorage().put(thisServerPath.getItemPath(), wf, null);
    }
}
