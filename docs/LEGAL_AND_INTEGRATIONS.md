# Legal channels and integration references

Recorded: 2026-10-27. Status: research and documentation only; no integration, outreach, appeal, or court filing has been performed.

This document records the user's supplied legal-channel requirements and the official references supporting them. It is an integration research record, not the platform's Terms of Service, Privacy Policy, an integration agreement, or confirmation of legal eligibility.

## 1. Haryana Auto Appeal System (AAS)

**Priority:** first government integration to investigate.

**User-supplied context:** AAS automates appeals for eligible applications within supported service-delivery workflows. Its published documentation describes integration with Antyodaya SARAL and other service portals. A public developer API, sandbox, or self-service developer signup has not been verified.

**Official references:**

- [Haryana Right to Service Commission guidelines](https://haryana-rtsc.gov.in/guidelines): the official index lists the Auto Appeal System Manual dated 7 March 2024 and related instructions.
- [NIC Haryana overview of the Auto Appeal System](https://informatics.nic.in/files/websites/october-2024/haryana.php): describes automatic appeals when eligible service application timelines are breached and manual appeals through SARAL.
- [Auto Appeal System portal](https://aas.saralharyana.nic.in/).

**Pending investigation:** ask the responsible department or NIC Haryana whether a documented API, sandbox, or other authorized integration route exists. Confirm covered services and eligibility, permitted actions, identity requirements, data-sharing terms, authentication, test access, acknowledgements, and status tracking before defining an adapter. This is a research task; the supplied text does not authorize sending messages or submitting appeals.

**Project boundary:** TheekHai's internal SLA breach or escalation does not itself establish eligibility for a statutory AAS appeal. Keep internal case IDs and events distinct from official application/appeal references. Do not claim an appeal was submitted, accepted, or escalated in AAS without an authorized channel and its acknowledgement.

## 2. eCourts e-Filing Services

**Purpose:** actual judicial proceedings through a formal filing channel.

**User-supplied context:** the official portal supports electronic filing of cases and applications, document uploads, e-signing, and associated filing workflows. It must not be treated as a general complaint API.

**Official references:**

- [eCourts e-Filing portal](https://efiling.ecourts.gov.in/).
- [Official e-Filing Services About Us](https://filing.ecourts.gov.in/pdedev/?p=otherPages%2FAboutUs): describes filing pleadings and applications, uploaded documents, e-signing, and court-related facilities.

**Required boundary:** use an authorized filing workflow and obtain appropriate legal review before any filing. Confirm the court, applicable filing rules, authorized filer, required documents, signatures, fees, and acceptance process for the specific proceeding. No public integration API or automation permission has been established by this research.

**Project boundary:** a missed department deadline, rejected closure, or contradictory evidence must not automatically initiate judicial proceedings. An internal TheekHai case, a prepared document, an uploaded filing, and a court-accepted filing are distinct states. Record official acknowledgements and identifiers only when actually received.

## 3. TinyFish

**User decision:** include TinyFish as an optional integration candidate. Current scope remains documentation only; no account setup, credentials, paid runs, or automation has been performed.

**Official reference:** [TinyFish developer documentation](https://docs.tinyfish.ai/) describes Search and Fetch for discovering and extracting sources, Research for source-backed reports, Agent for goal-driven web workflows, and Browser for controlled remote browser sessions. This overview was accessible and reviewed on 2026-10-27.

**Potential project use:** research official service information, extract relevant guidance, and investigate authorized portal workflows. A future server-side adapter may support permitted status lookups or assisted navigation after the target portal's access rules and authorization are established. Select the smallest capability needed for the specific task.

**Integration boundary:** TinyFish supplies browser/web tooling; it does not grant permission to access a government system, establish AAS eligibility, or authorize a court filing. Keep the AAS and eCourts restrictions above. Require specific authorization for submissions and the appropriate legal review for judicial filings. Do not treat an agent's success message as an official filing acknowledgement.

**Future implementation notes:** keep any `TINYFISH_API_KEY` in server configuration; expose no key to clients. Limit permitted destinations and actions, minimize personal data sent to the service, and record the source URL, retrieval time, and official acknowledgement where applicable. Review provider data handling, costs, run limits, and failure/retry behavior before enabling it. Ktor remains the workflow authority; TinyFish results are external observations, not authority to change case status.

## Research evidence and limitations

The official-domain search results reviewed on 2026-10-27 support the AAS and eCourts descriptions above. Direct page-open requests for those services timed out or were inaccessible during that review. The full AAS manual and court-specific filing rules have not been reviewed. Availability of an authorized government developer interface remains unverified; absence of a verified public API is not proof that no interface exists. TinyFish's documentation overview was read directly; its integration has not been tested.

Before changing an integration from research to implementation, record the specific official documentation/version, eligibility and permitted actions, authorization, handling of personal documents, and a verified test route here. Until then, keep these as source references and pending investigation items.
