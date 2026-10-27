package com.example.theekhai.domain.workflow

enum class CaseStatus { SUBMITTED, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION, CLOSED, REOPENED }
enum class Role { CITIZEN, DEPARTMENT, REVIEWER, ADMIN }
data class Actor(val id: String, val role: Role, val department: String? = null)
data class CaseRecord(
    val id: String, val title: String, val description: String, val category: String,
    val address: String, val latitude: Double?, val longitude: Double?, val citizenId: String,
    val department: String, val priority: String, val status: CaseStatus,
    val createdAt: Long, val updatedAt: Long, val dueAt: Long, val version: Long = 1,
    val escalation: Long = 0, val closureBy: String? = null,
)
data class CaseTask(val id: String, val caseId: String, val department: String, val title: String, val required: Boolean, val complete: Boolean)
data class CaseEvent(val id: String, val caseId: String, val actorId: String, val action: String, val note: String, val at: Long)
data class ReportInput(val title: String, val description: String, val category: String, val address: String, val latitude: Double?, val longitude: Double?, val consent: Boolean)
data class CaseAction(val action: String, val version: Long, val note: String, val target: String = "", val department: String = "")
class WorkflowFailure(val code: String, override val message: String) : RuntimeException(message)

interface WorkflowStore {
    fun <T> transaction(block: () -> T): T
    fun list(): List<CaseRecord>
    fun get(id: String): CaseRecord?
    fun save(record: CaseRecord)
    fun tasks(caseId: String): List<CaseTask>
    fun saveTask(task: CaseTask)
    fun events(caseId: String): List<CaseEvent>
    fun append(event: CaseEvent)
    fun receipt(actorId: String, key: String): Pair<String, String>?
    fun saveReceipt(actorId: String, key: String, fingerprint: String, caseId: String)
    fun link(caseId: String, targetId: String, kind: String)
}

class Workflow(private val store: WorkflowStore, private val now: () -> Long, private val newId: () -> String) {
    fun list(actor: Actor): List<CaseRecord> = store.list().filter { canRead(actor, it) }
    fun detail(actor: Actor, id: String): CaseRecord = store.get(id)?.also {
        if (!canRead(actor, it)) fail("FORBIDDEN", "You cannot access this case.")
    } ?: fail("NOT_FOUND", "Case not found.")
    fun tasks(actor: Actor, id: String) = detail(actor, id).let { store.tasks(id) }
    fun events(actor: Actor, id: String) = detail(actor, id).let { store.events(id) }

    fun submit(actor: Actor, input: ReportInput, key: String): CaseRecord = store.transaction {
        if (key.length !in 8..128) fail("INVALID_INPUT", "A submission key is required.")
        if (!input.consent || input.title.trim().length !in 5..120 || input.description.trim().length !in 10..5000 || input.address.trim().length !in 3..500) {
            fail("INVALID_INPUT", "Provide a title, description, address and consent.")
        }
        if (input.category !in departments.keys) fail("INVALID_INPUT", "Choose a supported category.")
        if ((input.latitude == null) != (input.longitude == null) || input.latitude?.let { !it.isFinite() || it !in -90.0..90.0 } == true || input.longitude?.let { !it.isFinite() || it !in -180.0..180.0 } == true) fail("INVALID_INPUT", "Invalid coordinates.")
        val fingerprint = input.toString()
        store.receipt(actor.id, key)?.let { (previous, caseId) ->
            if (previous != fingerprint) fail("CONFLICT", "Submission key was already used for different content.")
            return@transaction detail(actor, caseId)
        }
        val time = now()
        val record = CaseRecord(newId(), input.title.trim(), input.description.trim(), input.category, input.address.trim(), input.latitude, input.longitude, actor.id, departments.getValue(input.category), "MEDIUM", CaseStatus.ASSIGNED, time, time, time + 72 * HOUR)
        store.save(record)
        event(record.id, actor.id, "SUBMITTED", "Report received; consent recorded.")
        event(record.id, "system", "ASSIGNED", "Routed by category to ${record.department} under pilot policy.")
        store.saveTask(CaseTask(newId(), record.id, record.department, "Inspect and resolve reported issue", true, false))
        store.saveReceipt(actor.id, key, fingerprint, record.id)
        record
    }

    fun act(actor: Actor, id: String, command: CaseAction): CaseRecord = store.transaction {
        val record = detail(actor, id)
        if (record.version != command.version) fail("CONFLICT", "This case changed. Refresh before trying again.")
        if (command.note.trim().length !in 3..2000) fail("INVALID_INPUT", "Provide a reason between 3 and 2000 characters.")
        var next = record
        val staff = actor.role == Role.ADMIN || actor.role == Role.DEPARTMENT && (actor.department == record.department || store.tasks(id).any { it.department == actor.department })
        val reviewer = actor.role in setOf(Role.REVIEWER, Role.ADMIN)
        when (command.action) {
            "START" -> { requireRule(staff && record.status in setOf(CaseStatus.ASSIGNED, CaseStatus.REOPENED), "Cannot start this case."); next = record.copy(status = CaseStatus.IN_PROGRESS) }
            "ADD_TASK" -> {
                requireRule(staff && record.status in setOf(CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS, CaseStatus.REOPENED), "Cannot add tasks now.")
                requireRule(command.department in departments.values, "Choose a supported department.")
                store.saveTask(CaseTask(newId(), id, command.department, command.note.trim(), true, false))
            }
            "COMPLETE_TASK" -> {
                val task = store.tasks(id).find { it.id == command.target } ?: fail("NOT_FOUND", "Task not found.")
                requireRule(record.status == CaseStatus.IN_PROGRESS && (actor.role == Role.ADMIN || actor.role == Role.DEPARTMENT && actor.department == task.department), "Cannot complete this task.")
                store.saveTask(task.copy(complete = true))
            }
            "REQUEST_CLOSURE" -> {
                requireRule(staff && (actor.role == Role.ADMIN || actor.department == record.department) && record.status == CaseStatus.IN_PROGRESS, "Cannot request closure.")
                requireRule(store.tasks(id).none { it.required && !it.complete }, "Complete every required task first.")
                next = record.copy(status = CaseStatus.PENDING_VERIFICATION, closureBy = actor.id)
            }
            "APPROVE_CLOSURE", "REJECT_CLOSURE" -> {
                requireRule(reviewer && record.status == CaseStatus.PENDING_VERIFICATION && record.closureBy != actor.id, "An independent reviewer must decide closure.")
                next = record.copy(status = if (command.action == "APPROVE_CLOSURE") CaseStatus.CLOSED else CaseStatus.IN_PROGRESS)
            }
            "CHALLENGE" -> requireRule(actor.id == record.citizenId && record.status == CaseStatus.CLOSED, "Only the reporting citizen can challenge this closure.")
            "REOPEN" -> {
                requireRule(reviewer && record.status == CaseStatus.CLOSED && store.events(id).any { it.action == "CHALLENGE" && it.at >= record.updatedAt }, "A closed case with a challenge must be reviewed first.")
                next = record.copy(status = CaseStatus.REOPENED, dueAt = now() + 24 * HOUR, closureBy = null)
                store.saveTask(CaseTask(newId(), id, record.department, "Corrective action: ${command.note}", true, false))
            }
            "LINK_DUPLICATE", "LINK_RELATED" -> {
                requireRule(staff || reviewer, "Staff must confirm incident links.")
                requireRule(command.target != id, "Cannot link a case to itself.")
                detail(actor, command.target)
                store.link(id, command.target, command.action)
            }
            else -> fail("INVALID_INPUT", "Unsupported action.")
        }
        next = next.copy(version = record.version + 1, updatedAt = now())
        store.save(next)
        event(id, actor.id, command.action, command.note.trim())
        next
    }

    fun checkDeadlines() = store.transaction {
        val time = now()
        store.list().filter { it.status != CaseStatus.CLOSED }.forEach { record ->
            val cycle = record.dueAt.toString()
            fun once(action: String, note: String) {
                if (store.events(record.id).none { it.action == action && it.note.endsWith("[$cycle]") }) event(record.id, "system", action, "$note [$cycle]")
            }
            if (time >= record.createdAt + (record.dueAt - record.createdAt) * 3 / 4) once("SLA_WARNING", "Approaching pilot deadline")
            if (time >= record.dueAt) {
                val level = if (time >= record.dueAt + 24 * HOUR) 2L else 1L
                once("SLA_BREACH", "Pilot deadline missed; lead department notified")
                if (level == 2L) once("SLA_ESCALATION", "Overdue work escalated for operational review")
                if (record.escalation < level) store.save(record.copy(escalation = level, version = record.version + 1, updatedAt = time))
            }
        }
    }
    private fun canRead(actor: Actor, record: CaseRecord): Boolean = actor.role in setOf(Role.ADMIN, Role.REVIEWER) || record.citizenId == actor.id || actor.role == Role.DEPARTMENT && (record.department == actor.department || store.tasks(record.id).any { it.department == actor.department })
    private fun event(id: String, actor: String, action: String, note: String) = store.append(CaseEvent(newId(), id, actor, action, note, now()))
    private fun requireRule(value: Boolean, message: String) { if (!value) fail("FORBIDDEN", message) }
    private fun fail(code: String, message: String): Nothing = throw WorkflowFailure(code, message)
    companion object {
        const val HOUR = 3_600_000L
        val departments = mapOf("ROADS" to "Public works", "WATER" to "Water services", "WASTE" to "Sanitation", "LIGHTING" to "Electrical services", "OTHER" to "Civic triage")
    }
}
