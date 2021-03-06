package org.cristalise.kernel.test;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION

import java.time.LocalDateTime

import org.cristalise.dev.dsl.DevItemDSL
import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.LocalObjectLoader
import org.junit.After
import org.junit.Before
import org.mvel2.templates.CompiledTemplate
import org.mvel2.templates.TemplateCompiler
import org.mvel2.templates.TemplateRuntime

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 * 
 */
@CompileStatic @Slf4j
class KernelScenarioTestBase extends DevItemDSL {

    String timeStamp = null
    String folder = "integTest"

    private static Map<String, CompiledTemplate> mvelExpressions = new HashMap<String, CompiledTemplate>();

    /**
     * Utility method to evaluate MVEL templates which are based on simple maps
     *
     * @param vars input parameters
     * @return the XML string
     * @throws IOException
     */
    public static String evalMVELTemplate(String templ, Map<String, Object> vars) {
        CompiledTemplate expr = mvelExpressions.get(templ);

        if(expr == null) {
            log.debug("evalMVELTemplate() - Compiling template for "+templ);
            expr = TemplateCompiler.compileTemplate( new File(templ) );
            mvelExpressions.put(templ, expr);
        }
        else {
            log.debug("evalMVELTemplate() - CompiledTemplate was found for "+templ);
        }

        return (String) TemplateRuntime.execute(expr, vars);
    }

    public static String getNowString() {
        return LocalDateTime.now().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    /**
     * 
     * @param config
     * @param connect
     */
    public void init(String config, String connect) {
        String[] args = ['-logLevel', '8', '-config', config, '-connect', connect]

        Properties props = AbstractMain.readC2KArgs(args)
        Gateway.init(props)

        timeStamp = getNowString()
        agent = Gateway.connect("user", "test")
    }

    @Before
    public void before() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
    }

    @After
    public void after() {
        Gateway.close()
    }

    public ItemProxy createItemWithUpdateAndCheck(Map record, String factoryPath) {
        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = factory.getProperty('Root')
        String itemName = record.Name ?: ''
        Schema updateSchema = getUpdateSchema(factory)

        if (itemName) eraseItemIfExists(itemRoot, itemName)
        ItemProxy item = createItemAndCheck(factory, itemRoot, itemName)
        itemName = item.getName()

        //Name could be the generated by the Factory
        record.Name = itemName

        updateItemAndCheck(item, itemRoot, itemName, updateSchema, record)

        return item
    }

    public ItemProxy createItemAndCheck(ItemProxy factory, String itemRoot, String itemName) {
        def createJob  = factory.getJobByTransitionName('CreateItem', 'Done', agent)
        assert createJob, "Cannot get Job for Activity 'CreateItem' of Factory '$factory.path'"

        def outcome = createJob.getOutcome()

        //Name could be the generated by the Factory
        if (itemName) outcome.setField('Name', itemName)

        def result = agent.execute(createJob)

        //Name could be the generated by the Factory
        if (!itemName) {
            def o = new Outcome(result)
            itemName = o.getField('Name')
        }

        return agent.getItem("$itemRoot/$itemName")
    }

    public void updateItemAndCheck(ItemProxy newItem, String itemRoot, String itemName, Schema updateSchema, Map record) {
        def itemUpdateJob
        try {
            def woType = newItem.getProperty('WorkOrderType')
            if (woType.equals('Manufacturing')) {
                itemUpdateJob = newItem.getJobByName('Create', agent)
            } else if (woType.equals('Parts')) {
                itemUpdateJob = newItem.getJobByName('CreateParts', agent)
            }
            else if (woType.equals('Non-Productive')) {
                itemUpdateJob = newItem.getJobByName('CreateNonProductiveWorkOrder', agent)
            }
        }
        catch (ObjectNotFoundException e) {
            itemUpdateJob = newItem.getJobByName('Update', agent)
        }

        assert itemUpdateJob, "Cannot get Job for Activity 'Update' of Item '$itemRoot/$itemName'"

        def updateOutcome = itemUpdateJob.getOutcome()

        itemUpdateJob.setOutcome(new Outcome(DevXMLUtility.recordToXML(updateSchema.getName(), record), updateSchema))

        agent.execute(itemUpdateJob)

        //Checks viewpoint of Update outcome
        newItem.getViewpoint(updateSchema.getName(), 'last')
    }

    /**
     * 
     * @param record
     * @param factoryPath
     * @return
     */
    public ItemProxy createItemWithConstructorAndCheck(Map record, String factoryPath) {
        ItemProxy factory = agent.getItem(factoryPath)
        String itemRoot = factory.getProperty('Root')
        String itemName = record.Name ?: ''
        Schema updateSchema = getUpdateSchema(factory)

        if (itemName) eraseItemIfExists(itemRoot, itemName)

        def createJob  = factory.getJobByTransitionName('CreateItem', 'Done', agent)
        assert createJob, "Cannot get Job for Activity 'CreateItem' of Factory '$factory.path'"

        def outcome = createJob.getOutcome()
        //Name could be the generated by the Factory
        if (itemName) outcome.setField('Name', itemName)
        outcome.appendXmlFragment "/Factory_NewInstanceDetails/SchemaInitialise", DevXMLUtility.recordToXML(updateSchema.getName(), record)

        def result = agent.execute(createJob)

        //Name could be the generated by the Factory
        if (!itemName) {
            def o = new Outcome(result)
            itemName = o.getField('Name')
        }

        return agent.getItem("$itemRoot/$itemName")
    }

    /**
     * 
     * @param factory
     * @return
     */
    public Schema getUpdateSchema(ItemProxy factory) {
        if (factory.checkContent(ClusterType.COLLECTION, BuiltInCollections.SCHEMA_INITIALISE.name)) {
            def initSchemaCollection = factory.getCollection(BuiltInCollections.SCHEMA_INITIALISE)
            DependencyMember member = initSchemaCollection.getMembers().list[0]

            def updateSchemaUUID = member.getChildUUID()
            def updateSchemaVersion = member.getProperties().getBuiltInProperty(VERSION)
            if (updateSchemaVersion instanceof String) updateSchemaVersion = Integer.parseInt(updateSchemaVersion)

            return LocalObjectLoader.getSchema(updateSchemaUUID, (Integer)updateSchemaVersion)
        }
        else {
            def nameAndVersion = factory.getProperty('UpdateSchema').split(':')
            return LocalObjectLoader.getSchema(nameAndVersion[0], Integer.parseInt(nameAndVersion[1]))
        }
    }

    /**
     * 
     * @param itemRoot
     * @param itemName
     */
    public void eraseItemIfExists(String itemRoot, String itemName) {
        if (itemName) {
            try {
                agent.execute(agent.getItem("$itemRoot/$itemName"), Erase.class);
            }
            catch (ObjectNotFoundException e) {}
        }
    }

}
