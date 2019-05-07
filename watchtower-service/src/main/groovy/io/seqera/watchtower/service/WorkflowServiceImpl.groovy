package io.seqera.watchtower.service

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import io.seqera.watchtower.domain.Workflow
import io.seqera.watchtower.pogo.WorkflowTraceJsonUnmarshaller
import io.seqera.watchtower.pogo.enums.WorkflowStatus
import io.seqera.watchtower.pogo.exceptions.NonExistingWorkflowException

import javax.inject.Singleton

@Transactional
@Singleton
class WorkflowServiceImpl implements WorkflowService {

    Workflow processWorkflowJsonTrace(Map workflowJson) {
        WorkflowStatus workflowStatus = WorkflowTraceJsonUnmarshaller.identifyWorflowStatus(workflowJson)

        workflowStatus == WorkflowStatus.STARTED ? createFromJson(workflowJson) : updateFromJson(workflowJson, workflowStatus)
    }

    private Workflow createFromJson(Map workflowJson) {
        Workflow newWorkflow = new Workflow()
        WorkflowTraceJsonUnmarshaller.populateWorkflowFields(workflowJson, WorkflowStatus.STARTED, newWorkflow)

        newWorkflow.save()
        newWorkflow
    }

    private Workflow updateFromJson(Map workflowJson, WorkflowStatus workflowStatus) {
        Workflow existingWorkflow = Workflow.get((Long) workflowJson.workflow['workflowId'])
        if (!existingWorkflow) {
            throw new NonExistingWorkflowException("Can't update a non-existing workflow")
        }

        WorkflowTraceJsonUnmarshaller.populateWorkflowFields(workflowJson, workflowStatus, existingWorkflow)

        existingWorkflow.save()
        existingWorkflow
    }

}