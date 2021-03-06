def newInstanceDetails = Schema('Factory_NewInstanceDetails', 0) {
    struct(name: 'Factory_NewInstanceDetails', useSequence: true) {
        field(name: 'Name',      type: 'string',  documentation: 'The Name of the new instance, it can be generated')
        field(name: 'LastCount', type: 'integer', documentation: 'The last number used to generate the ID', multiplicity: '0..1') {
            dynamicForms (hidden: true)
        }
        struct(name: 'SchemaInitialise', useSequence: true, multiplicity: '0..1') {
            anyField()
        }
    }
}

def instantiateItem = Script("Factory_InstantiateItem", 0) {
    script('groovy', moduleDir+'/script/Factory_InstantiateItem.groovy')
}

changeName = Script("Entity_ChangeName", 0) {
    script('groovy', moduleDir+'/script/Entity_ChangeName.groovy')
}

def createItem = Activity('Factory_CreateItem', 0) {
    Property('OutcomeInit': 'Empty')

    Schema(newInstanceDetails)
    Script(instantiateItem)
}

Workflow('Factory_Workflow', 0) {
    ElemActDef(createItem)
}
