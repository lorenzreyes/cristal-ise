import static org.apache.commons.lang3.StringUtils.leftPad

import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.property.PropertyArrayList
import org.cristalise.kernel.utils.LocalObjectLoader
import org.cristalise.kernel.utils.Logger

//--------------------------------------------------
// item, agent and job are injected by the Script class
// automatically so these declaration are only needed
// to write the script with code completion.
// COMMENT OUT before you run the module generators
// otherwise the script will fail with NPE
//--------------------------------------------------
//ItemProxy item
//AgentProxy agent
//Job job
//--------------------------------------------------

Outcome outcome = job.getOutcome()
Integer lastCount = 0

if (item.checkViewpoint('Factory_NewInstanceDetails', 'last')) {
    def lastOutcome = item.getOutcome(item.getViewpoint('Factory_NewInstanceDetails', 'last'))
    lastCount = new Integer(lastOutcome.getField("LastCount"))
}

Logger.msg 5, "Script.Factory_InstantiateItem - Factory_NewInstanceDetails:%s", outcome.getData()

String  itemName     = outcome.getField('Name')
int     padSize      = new Integer(item.getProperty("LeftPadSize"))
String  root         = item.getProperty("Root")
String  prefix       = item.getProperty("IDPrefix")
String  version      = "last"
String  predefStep   = "CreateItemFromDescription"

String initaliseOutcomeXML = null

if (item.checkContent(ClusterType.COLLECTION.name, BuiltInCollections.SCHEMA_INITIALISE.name)) {
    def initSchemaCollection = item.getCollection(BuiltInCollections.SCHEMA_INITIALISE)
    DependencyMember member = initSchemaCollection.getMembers().list[0]

    def updateSchemaUUID = member.getChildUUID()
    def updateSchemaVersion = Integer.parseInt(member.getProperties().get("Version"))

    def updateSchema = LocalObjectLoader.getSchema(updateSchemaUUID, updateSchemaVersion).getName()

    def initialiseNode = outcome.getNodeByXPath("/NewInstanceDetails/SchemaInitialise/$updateSchema")

    if (initialiseNode) {
        initaliseOutcomeXML = Outcome.serialize(initialiseNode, true)

        Logger.msg 5, "Script.Factory_InstantiateItem - initaliseOutcomeXML:%s", initaliseOutcomeXML
    }
    else
        throw new InvalidDataException("Script.Factory_InstantiateItem - invalid path:/NewInstanceDetails/SchemaInitialise/$updateSchema")
}

boolean createAgent   = new Boolean(item.getProperty("CreateAgent",   'false'))
boolean generatedName = new Boolean(item.getProperty("GeneratedName", 'false'))

if (!prefix)  throw new InvalidDataException("Script.InstantiateItem - Activity property IDPrefix must contain value")
if (!padSize) throw new InvalidDataException("Script.InstantiateItem - Activity property LeftPadSize must contain value")

if (!generatedName && (!itemName || itemName == "string" || itemName == "null"))
    throw new InvalidDataException("Script.InstantiateItem - Name must be provided")

String itemID = prefix + leftPad((++lastCount).toString(), padSize, "0")

String[] params

if (createAgent) {
    predefStep = "CreateAgentFromDescription"

    if (generatedName) {
        //This kind of Agent does not have a user entered name, therefore ID is used for Name property
        params = new String[initaliseOutcomeXML ? 5 : 4];
        params[0] = itemID;
    }
    else {
        //This kind of Agent use user entered name, therefore ID is saved in ID property
        params = new String[initaliseOutcomeXML ? 7 : 6]
        params[0] = itemName
    }

    params[1] = root;
    params[2] = item.getProperty("DefaultRoles");
    params[3] = "password";

    if (params.length >= 6) {
        params[4] = version

        def initalProps = new PropertyArrayList();
        initalProps.put(new Property("ID", itemID, false));

        params[5] = agent.marshall(initalProps);
    }
}
else {
    if (generatedName) {
        //This kind of Item does not have a user entered name, therefore ID is used for Name property
        int length = initaliseOutcomeXML ? 3 : 2
        params = new String[length]
        params[0] = itemID
    }
    else {
        //This kind of Item use user entered name, therefore ID is saved in ID property
        int length = initaliseOutcomeXML ? 5 : 4
        params = new String[length]
        params[0] = itemName;
    }

    params[1] = root;

    if (params.length >= 4) {
        params[2] = version;

        def initalProps = new PropertyArrayList();
        initalProps.put(new Property("ID", itemID, false));

        params[3] = agent.marshall(initalProps);
    }
}

if (initaliseOutcomeXML) params[params.length-1] = initaliseOutcomeXML

outcome.setFieldByXPath("NewInstanceDetails/ID", itemID);
outcome.setFieldByXPath("NewInstanceDetails/LastCount", lastCount.toString());

try {
	agent.execute(item, predefStep, params);
} catch ( ObjectAlreadyExistsException ex ) {
    if (generatedName) {
        throw new ObjectAlreadyExistsException("[errorMessage]Error creating item with Generated ID: $itemID. Please contact support.[/errorMessage]")
    }
	throw new ObjectAlreadyExistsException("[errorMessage]Item already exists: $itemName[/errorMessage]")
}

if (createAgent) {
    params = new String[1]

    params[0] = 'password'

    ItemProxy employee = agent.getItem(root + '/' + itemName)
    agent.execute(employee, "SetAgentPassword", params)
}
