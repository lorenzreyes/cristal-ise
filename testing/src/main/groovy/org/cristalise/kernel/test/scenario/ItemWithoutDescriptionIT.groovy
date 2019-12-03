package org.cristalise.kernel.test.scenario;

import static org.junit.Assert.*

import org.apache.commons.collections4.CollectionUtils
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic


/**
 * 
 */
@CompileStatic
class ItemWithoutDescriptionIT extends KernelScenarioTestBase {

    ItemProxy serverItem

    @Before
    public void before() {
        super.before();

        serverItem = agent.getItem("/domain/servers/localhost")
        assert serverItem && serverItem.getName() == "localhost"
    }

    private RolePath createRole(String role) {
        executeDoneJob(serverItem, "CreateNewRole", KernelXMLUtility.getRoleXML(name: role))
        return Gateway.getLookup().getRolePath(role);
    }

    private void removeRole(String role) {
        String[] params = [ role ];
        agent.execute(serverItem, "RemoveRole", params);
        assert ! Gateway.getLookup().exists(new RolePath(role, false));
    }

    private AgentProxy createAgent(String name, String role) {
        Job j = executeDoneJob(serverItem, "CreateNewAgent", KernelXMLUtility.getAgentXML(name: name, password: "test", Role: role))
        return Gateway.getProxyManager().getAgentProxy( Gateway.getLookup().getAgentPath(name) )
    }

    private ItemProxy createItem(String name) {
        Job j = executeDoneJob(serverItem, "CreateNewItem", KernelXMLUtility.getItemXML(name: name, workflow: 'NoWorkflow', initialPath: '/domain/itemTest'))
        return Gateway.getProxyManager().getProxy( Gateway.getLookup().getItemPath(j.itemUUID) )
    }

    @Test
    public void 'CreateNewRole and RemoveRole predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        createRole(role)
        removeRole(role)
    }

    @Test
    public void 'CreateNewAgent with initialPath using predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        ImportRole newRole = new ImportRole()
        newRole.setName(role)
        newRole.jobList = false
        newRole.permissions.add('dom1:Func1,Func2:')
        newRole.permissions.add('dom2:Func1:toto')

        agent.execute(serverItem, CreateNewRole.class, agent.marshall(newRole))
        
        def rp = Gateway.getLookup().getRolePath(role)

        ImportAgent newAgent = new ImportAgent('/itemTest/agents', name, 'pwd');
        newAgent.addRoles([rp]);
    
        agent.execute(serverItem, CreateNewAgent.class, agent.marshall(newAgent));
    }

    @Test
    public void 'CreateNewAgent predefined step of ServerItem'() {
        String role = "TestRole-$timeStamp"
        String name = "TestAgent-$timeStamp"

        createRole(role)
        createAgent(name, role)
    }

    @Test
    public void 'CreateNewItem predefined step of ServerItem'() {
        createItem("TestItem-$timeStamp")
    }
}
