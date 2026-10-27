package com.example.theekhai.data.workflow

import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.domain.workflow.*

class SqlWorkflowStore(private val db: AppDatabase) : WorkflowStore {
    private val q get() = db.pilotQueries
    override fun <T> transaction(block: () -> T): T = db.transactionWithResult { block() }
    override fun list() = q.allCases().executeAsList().map { it.domain() }
    override fun get(id: String) = q.caseById(id).executeAsOneOrNull()?.domain()
    override fun save(record: CaseRecord) = with(record) { q.saveCase(id,title,description,category,address,latitude,longitude,citizenId,department,priority,status.name,createdAt,updatedAt,dueAt,version,escalation,closureBy); Unit }
    override fun tasks(caseId: String) = q.tasks(caseId).executeAsList().map { CaseTask(it.id,it.caseId,it.department,it.title,it.required != 0L,it.complete != 0L) }
    override fun saveTask(task: CaseTask) = with(task) { q.saveTask(id,caseId,department,title,if(required) 1 else 0,if(complete) 1 else 0); Unit }
    override fun events(caseId: String) = q.events(caseId).executeAsList().map { CaseEvent(it.id,it.caseId,it.actorId,it.action,it.note,it.at) }
    override fun append(event: CaseEvent) = with(event) { q.appendEvent(id,caseId,actorId,action,note,at); Unit }
    override fun receipt(actorId: String, key: String) = q.receipt(actorId,key).executeAsOneOrNull()?.let { it.fingerprint to it.caseId }
    override fun saveReceipt(actorId: String,key: String,fingerprint: String,caseId: String) { q.saveReceipt(actorId,key,fingerprint,caseId) }
    override fun link(caseId: String,targetId: String,kind: String) { q.link(caseId,targetId,kind) }
    private fun com.example.theekhai.data.db.PilotCase.domain() = CaseRecord(id,title,description,category,address,latitude,longitude,citizenId,department,priority,CaseStatus.valueOf(status),createdAt,updatedAt,dueAt,version,escalation,closureBy)
}
