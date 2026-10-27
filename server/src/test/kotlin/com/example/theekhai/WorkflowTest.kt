package com.example.theekhai

import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.data.workflow.SqlWorkflowStore
import com.example.theekhai.domain.workflow.*
import kotlin.test.*
import java.util.UUID
import java.nio.file.Files

class WorkflowTest {
    private var clock = 1_800_000_000_000L
    private val citizen = Actor("citizen",Role.CITIZEN)
    private val worker = Actor("worker",Role.DEPARTMENT,"Public works")
    private val reviewer = Actor("reviewer",Role.REVIEWER)
    private val input = ReportInput("Broken road surface","A deep pothole blocks the crossing.","ROADS","Demonstration ward A",null,null,true)
    @Test fun completeReviewAndChallengeCycle() {
        val driver = createJvmSqliteDriver(null)
        try {
            val flow = Workflow(SqlWorkflowStore(AppDatabase(driver)),{ clock }) { UUID.randomUUID().toString() }
            var case = flow.submit(citizen,input,"request-001")
            assertEquals(case.id,flow.submit(citizen,input,"request-001").id)
            assertFailsWith<WorkflowFailure> { flow.submit(citizen,input.copy(title="Different title"),"request-001") }
            assertFailsWith<WorkflowFailure> { flow.detail(Actor("other",Role.CITIZEN),case.id) }
            fun act(actor: Actor, action: String, target: String = "") { case = flow.act(actor,case.id,CaseAction(action,case.version,"Recorded test reason",target)) }
            act(worker,"START")
            assertFailsWith<WorkflowFailure> { act(worker,"REQUEST_CLOSURE") }
            act(worker,"COMPLETE_TASK",flow.tasks(worker,case.id).first().id)
            act(worker,"REQUEST_CLOSURE")
            assertFailsWith<WorkflowFailure> { act(worker.copy(role=Role.ADMIN),"APPROVE_CLOSURE") }
            act(reviewer,"APPROVE_CLOSURE")
            assertEquals(CaseStatus.CLOSED,case.status)
            clock += 1000
            act(citizen,"CHALLENGE")
            act(reviewer,"REOPEN")
            assertEquals(CaseStatus.REOPENED,case.status)
            assertEquals(clock + 24 * Workflow.HOUR,case.dueAt)
            assertTrue(flow.events(citizen,case.id).any { it.action == "APPROVE_CLOSURE" })
        } finally { driver.close() }
    }
    @Test fun restartPreservesCasesAndDeadlineEventsAreIdempotent() {
        val file = Files.createTempFile("theekhai-test-", ".db")
        var driver = createJvmSqliteDriver(file.toString())
        try {
            var flow = Workflow(SqlWorkflowStore(AppDatabase(driver)),{ clock }) { UUID.randomUUID().toString() }
            val case = flow.submit(citizen,input,"request-002")
            clock = case.dueAt + 25 * Workflow.HOUR
            flow.checkDeadlines(); flow.checkDeadlines()
            driver.close(); driver = createJvmSqliteDriver(file.toString())
            flow = Workflow(SqlWorkflowStore(AppDatabase(driver)),{ clock }) { UUID.randomUUID().toString() }
            flow.checkDeadlines()
            assertEquals(2,flow.detail(citizen,case.id).escalation)
            assertEquals(1,flow.events(citizen,case.id).count { it.action == "SLA_BREACH" })
            assertEquals(1,flow.events(citizen,case.id).count { it.action == "SLA_ESCALATION" })
        } finally { driver.close(); Files.deleteIfExists(file) }
    }
}
