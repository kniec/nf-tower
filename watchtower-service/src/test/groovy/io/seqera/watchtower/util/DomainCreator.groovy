package io.seqera.watchtower.util

import io.seqera.watchtower.domain.MagnitudeSummary
import io.seqera.watchtower.domain.Task
import io.seqera.watchtower.domain.Workflow
import io.seqera.watchtower.pogo.enums.TaskStatus
import io.seqera.watchtower.pogo.enums.WorkflowStatus

import java.time.Instant

class DomainCreator {

    Boolean save = true
    Boolean validate = true
    Boolean failOnError = true
    Boolean withNewTransaction = true

    static void cleanupDatabase() {
        MagnitudeSummary.deleteAll(MagnitudeSummary.list())
        Task.deleteAll(Task.list())
        Workflow.deleteAll(Workflow.list())
    }

    Workflow createWorkflow(Map fields = [:]) {
        Workflow workflow = new Workflow()

        fields.sessionId = fields.containsKey('sessionId') ? fields.sessionId : "35cce421-4712-4da5-856b-6557635e54${generateUniqueNamePart()}d".toString()
        fields.runName = fields.containsKey('runName') ? fields.runName : "astonishing_majorana${generateUniqueNamePart()}".toString()
        fields.currentStatus = fields.containsKey('currentStatus') ? fields.currentStatus : WorkflowStatus.STARTED
        fields.submitTime = fields.containsKey('submitTime') ? fields.submitTime : Instant.now()
        fields.startTime = fields.containsKey('startTime') ? fields.startTime : fields.submitTime
        fields.running = fields.containsKey('running') ? fields.running : 0
        fields.submitted = fields.containsKey('submitted') ? fields.submitted : 0
        fields.failed = fields.containsKey('failed') ? fields.failed : 0
        fields.pending = fields.containsKey('pending') ? fields.pending : 0
        fields.succeeded = fields.containsKey('succeeded') ? fields.succeeded : 0
        fields.cached = fields.containsKey('cached') ? fields.cached : 0

        populateInstance(workflow, fields)
    }

    Task createTask(Map fields = [:]) {
        Task task = new Task()

        fields.workflow = fields.containsKey('workflow') ? fields.workflow : createWorkflow()
        fields.taskId = fields.containsKey('taskId') ? fields.taskId : 1
        fields.name = fields.containsKey('name') ? fields.name : "taskName_${generateUniqueNamePart()}"
        fields.hash = fields.containsKey('hash') ? fields.hash : "taskHash_${generateUniqueNamePart()}"
        fields.currentStatus = fields.containsKey('currentStatus') ? fields.currentStatus : TaskStatus.SUBMITTED
        fields.submitTime = fields.containsKey('submitTime') ? fields.submitTime : Instant.now()

        populateInstance(task, fields)
    }

    MagnitudeSummary createMagnitudeSummary(Map fields = [:]) {
        MagnitudeSummary magnitudeSummary = new MagnitudeSummary()

        fields.name = fields.containsKey('name') ? fields.name : "magnitude_${generateUniqueNamePart()}"
        fields.taskLabel = fields.containsKey('taskLabel') ? fields.taskLabel : "task_${generateUniqueNamePart()}"
        fields.mean = fields.containsKey('mean') ? fields.mean : 0.0
        fields.min = fields.containsKey('min') ? fields.min : 0.0
        fields.q1 = fields.containsKey('q1') ? fields.q1 : 0.0
        fields.q2 = fields.containsKey('q2') ? fields.q2 : 0.0
        fields.q2 = fields.containsKey('q2') ? fields.q2 : 0.0
        fields.q3 = fields.containsKey('q3') ? fields.q3 : 0.0
        fields.max = fields.containsKey('max') ? fields.max : 0.0
        fields.minLabel = fields.containsKey('minLabel') ? fields.minLabel : 'minLabel'
        fields.maxLabel = fields.containsKey('maxLabel') ? fields.maxLabel : 'maxLabel'
        fields.q1Label = fields.containsKey('q1Label') ? fields.q1Label : 'q1Label'
        fields.q2Label = fields.containsKey('q2Label') ? fields.q2Label : 'q2Label'
        fields.q3Label = fields.containsKey('q3Label') ? fields.q3Label : 'q3Label'

        populateInstance(magnitudeSummary, fields)
    }


    /**
     * Populates and persists (if the meta params say so) an instance of a class in the database given their params
     * @param clazz the class to create the instance of
     * @param params the params to create the instance, it can contain lists too
     * @param persist if the instance to save is persisted in the database (true by default)
     * @param validate if the instance to save needs to be validated (true by default)
     * @return the persisted instance
     */
    private def populateInstance(def instance, Map params) {
        Map regularParams = [:]
        Map listParams = [:]
        extractListsFromMap(params, regularParams, listParams)

        regularParams.each { String k, def v ->
            if (instance.hasProperty(k)) {
                instance[k] = v
            }
        }
        listParams.each { String k, List v ->
            addAllInstances(instance, k, v)
        }

        if (!save) {
            return instance
        }
        if (withNewTransaction) {
            instance.withNewTransaction { instance.save(validate: validate, failOnError: failOnError) }
            return instance
        }

        instance.save(validate: validate, failOnError: failOnError)
        instance
    }

    /**
     * Separates the entries whose value is a regular instance from the entries whose value is a list instance of a map
     * @param params the map with all the params for an instance
     * @param regularParams the map to populate with entries whose value is a regular instance
     * @param listsParams noLists the map to populate with entries whose value is a list instance
     */
    private void extractListsFromMap(Map params, Map regularParams, Map listsParams) {
        params?.each { k, v ->
            if (v instanceof List) {
                listsParams[k] = v
            } else {
                regularParams[k] = v
            }
        }
    }

    /**
     * Associates the objects contained in the collection to the corresponding property of the given instance
     * @param instance the instance to populate its collection
     * @param collectionName the name of the collection property
     * @param collection the collection which contains the instances to add
     */
    private void addAllInstances(def instance, String collectionName, List collection) {
        collection?.each {
            instance."addTo${collectionName.capitalize()}"(it)
        }
    }

    /**
     * Generate a unique string in order to make a name distinguishable
     * @return a random string with a low probability of collision with other generated strings
     */
    private String generateUniqueNamePart() {
        "${UniqueIdentifierGenerator.generateUniqueId()}"
    }
}

class UniqueIdentifierGenerator {

    private static long uniqueIdentifier = 0

    /**
     * Generates an unique numeric id
     * @return an unique numeric id
     */
     static long generateUniqueId() {
        uniqueIdentifier++
    }
}