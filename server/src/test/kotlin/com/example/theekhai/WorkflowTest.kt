package com.example.theekhai

import com.example.theekhai.contract.*
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.data.workflow.SqlWorkflowStore
import com.example.theekhai.domain.workflow.*
import com.example.theekhai.legal.LegalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
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

    @Test fun tinyFishAgentMonitorTavilyAndBackupFallbackChain() = testApplication {
        var tinyFishHealthy = true
        var tavilyHealthy = true
        externalServices {
            hosts("https://mock.tinyfish.local") {
                routing {
                    post("/v1/automation/run") {
                        if (!tinyFishHealthy) {
                            call.respond(HttpStatusCode.ServiceUnavailable, """{"error":"busy"}""")
                            return@post
                        }
                        val body = call.receiveText()
                        if (body.contains("Extract the latest official guidance")) {
                            call.respondText(
                                """{"result":{"title":"Updated Haryana AAS Timeline Circular","content":"Under the Haryana Right to Service Commission guidelines and the Auto Appeal System (AAS) connected to Antyodaya SARAL, updated 2026 timelines require automatic escalation within 5 working days."}}""",
                                ContentType.Application.Json,
                            )
                        } else {
                            call.respondText(
                                """{"result":{"answer":"Under the Haryana Right to Service Commission guidelines and the Auto Appeal System (AAS) connected to Antyodaya SARAL, notified public services carry stipulated delivery timelines."}}""",
                                ContentType.Application.Json,
                            )
                        }
                    }
                    post("/v1/monitors") {
                        call.respondText("""{"id":"tf-mon-101","status":"active"}""", ContentType.Application.Json, HttpStatusCode.Created)
                    }
                    post("/v1/monitors/{id}/runs") {
                        call.respondText("""{"run_id":"tf-run-202","status":"completed"}""", ContentType.Application.Json)
                    }
                }
            }
            hosts("https://mock.tavily.local") {
                routing {
                    post("/search") {
                        if (!tavilyHealthy) {
                            call.respond(HttpStatusCode.ServiceUnavailable, """{"error":"down"}""")
                            return@post
                        }
                        call.respondText(
                            """{"answer":"Under the Haryana Right to Service Commission guidelines and the Auto Appeal System (AAS), notified public services carry stipulated delivery timelines.","results":[{"url":"https://haryana-rtsc.gov.in/guidelines","content":"Under the Haryana Right to Service Commission guidelines and the Auto Appeal System (AAS), notified public services carry stipulated delivery timelines for SARAL applications."}]}""",
                            ContentType.Application.Json,
                        )
                    }
                }
            }
        }

        val driver = createJvmSqliteDriver(null)
        try {
            val db = AppDatabase(driver)
            val legal = LegalService(
                db = db,
                client = createClient { },
                tinyFishApiKey = "tf-test-key",
                tavilyApiKey = "tvly-test-key",
                tinyFishBaseUrl = "https://mock.tinyfish.local",
                tavilyBaseUrl = "https://mock.tavily.local",
            )

            // 1. Primary TinyFish Agent Mode
            val agentAnswer = legal.ask(LegalQuestionDto("How does Haryana AAS handle service timelines?", "HARYANA"))
            assertEquals("TINYFISH_AGENT", agentAnswer.mode)
            assertTrue(agentAnswer.citations.isNotEmpty())

            // 2. Secondary Tavily Fallback when TinyFish fails
            tinyFishHealthy = false
            val tavilyAnswer = legal.ask(LegalQuestionDto("How does Haryana AAS handle service timelines?", "HARYANA"))
            assertEquals("TAVILY_FALLBACK", tavilyAnswer.mode)

            // 3. Deterministic Local Backup Fallback (SOURCES_ONLY) when both fail
            tavilyHealthy = false
            val backupAnswer = legal.ask(LegalQuestionDto("How does Haryana AAS handle service timelines?", "HARYANA"))
            assertEquals("SOURCES_ONLY", backupAnswer.mode)
            assertTrue(backupAnswer.answer.contains("Auto Appeal System"))

            // 4. TinyFish Monitor Mode + Agent extraction queues PENDING revision without replacing APPROVED source
            tinyFishHealthy = true
            tavilyHealthy = true
            val monitor = legal.createMonitor(
                CreateMonitorDto(
                    name = "Haryana RTSC Guidelines Monitor",
                    monitorType = "fetch",
                    target = "https://haryana-rtsc.gov.in/guidelines",
                    jurisdiction = "HARYANA",
                    language = "en",
                )
            )
            assertEquals("tf-mon-101", monitor.remoteMonitorId)

            val runResult = legal.runMonitor(monitor.id)
            assertEquals("TINYFISH_MONITOR_AND_AGENT", runResult.providerMode)
            assertEquals(1, runResult.queuedSources.size)
            val pendingRevision = runResult.queuedSources.first()
            assertEquals("PENDING", pendingRevision.status)

            // Approved search still returns the original approved content until reviewer approves
            assertFalse(legal.search("timeline", "HARYANA").any { it.id == pendingRevision.id })
            legal.review(pendingRevision.id, "reviewer-1", true)
            assertTrue(legal.search("timeline", "HARYANA").any { it.id == pendingRevision.id })
        } finally {
            driver.close()
        }
    }
}
