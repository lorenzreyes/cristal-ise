The Workflow object is the container for all [Activities](../Activity) in an [Item](../Item). It resolves all activity paths from execution requests to individual Activities, and passes requests to them. Its contents are fixed as two [CompositeActivities](../CompositeActivity): one PredefinedStepContainer, which is a non-persistent container for all implemented [PredefinedStep](../PredefinedStep)s, and a persistent CompositeActivity that contains the domain workflow, instantiated from a Workflow description. Workflow descriptions can be any self-contained CompositeActivity description Item.

During Item initialization, the Workflow object activates the first Activity of the domain workflow, enabling execution. 