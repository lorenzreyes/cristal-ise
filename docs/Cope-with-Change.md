Designed to Cope with Change
----------------------------

Implement business functionality gradually without the need of reimplementing the full system

The idea of Description-Driven Framework was originated from [CRISTAL](http://proj-cristal.web.cern.ch/proj-cristal/), a system which was developed at CERN to manage the construction of the CMS/ECAL detector. Due to the scientific nature and the extended time-scale of the construction (5+ years), the users (physicists/engineers) wanted to gradually define and evolve detector elements, their data structures, construction steps and scientific calculations in order to produce the best possible device. In order to meet these requirements the team had to develop a system which could **cope with change** by design. Such a unique constraint has inspired the research and development teams at UWE/CERN to design and develop a so-called Description-Driven System in which all logic and data structures have “descriptions” which can be modified and versioned to reflect changes required by physicists.

The original CRISTAL software was a fully functional Description-Driven System, which had to be configured and installed so it could be used like a CRM or WFM system. The current version provided in the [CRISTAL-iSE](/cristal-ise) modules, offers building blocks to build fully functional description-driven system hence the name Description-Driven Framework (DDF).

### Technical Explanation

Description-Driven Approach (DDA) is an architecture style for Evolutionary Systems. From this point of view DDA is an alternative approach to [MicroServices](https://www.thoughtworks.com/insights/blog/microservices-evolutionary-architecture), the recently emerging architecture style, which promotes that independently deployable service is the key design principle to create evolutionary and scalable application. On the other hand DDA has evolved from the observation that hard-coded business model and process is one of the major barrier to write system which can cope with changes. DDA solve this by using a 2 dimensional abstraction of relationship, process and data. The two dimensions are the the definition-of and the instance-of relationship between objects within the system. In other words, Description-Driven approach means, that instead-of hard-coding business process and data into the system, the design of the system should be based on versionable description of data implementing the business model and process, hence the name DDA. The data and process aspect of DDA gives the advantage to provide guidelines and tools to support the entire Delivery Pipeline. For more information check the [Code and Data Evolves together](../Evolve-Code-with-Data) page.

### Scope of Change is Explicitly Defined

For evolution the most important aspect is to define the relevant **scope of change**. The software development community uses different approach to define such scope. In Domain Driven Design (DDD) the scope is the size of the [Aggregate](http://martinfowler.com/bliki/DDD_Aggregate.html), for Microservices the scope is the size of a Service, for Continuous Delivery the scope is the independently deployable unit of code. Even the different branching strategies define the scope of the branch (feature, task, release, integration, ...), so the SDLC can handle the change independently. 

CRISTAL-iSE is based on 3 major concept to define the possible scope of change: relationship, process, data ... (Needs to be finsihed)
