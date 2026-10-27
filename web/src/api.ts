export interface Identity {
  id: string;
  role: 'CITIZEN' | 'DEPARTMENT' | 'REVIEWER' | 'ADMIN';
  department: string | null;
  csrf: string | null;
  email?: string | null;
  fullName?: string | null;
  organization?: string | null;
  ward?: string | null;
  preferredLanguage?: string | null;
}

export interface AuthConfig {
  supabaseUrl: string;
  publishableKey: string;
  configured: boolean;
}

export interface RegisterInput {
  fullName: string;
  email: string;
  password: string;
  organization?: string;
  ward?: string;
  preferredLanguage?: string;
}

export interface Profile {
  userId: string;
  email: string;
  fullName: string;
  organization: string;
  role: string;
  department: string | null;
  ward: string;
  preferredLanguage: string;
  updatedAt?: string | null;
}

export interface SavedSource {
  id: string;
  userId: string;
  title: string;
  sourceUrl: string;
  jurisdiction: string;
  snippet: string;
  createdAt?: string | null;
}

export interface CaseLink {
  caseId: string;
  targetId: string;
  kind: string;
}

export interface VerificationResult {
  id: string;
  complaintId: string;
  state: 'IN_PROGRESS' | 'CONFIRMED_RESOLVED' | 'CONTRADICTION_FOUND' | 'MANUAL_INSPECTION_REQUIRED';
  verified: boolean;
  scrapedPortalUrl?: string | null;
  scrapedEvidenceSnippet?: string | null;
  contradictionDetected: boolean;
  explanation: string;
  verifiedAtEpochMillis: number;
}

export interface CaseRecord { id:string; title:string; description:string; category:string; address:string; latitude:number|null; longitude:number|null; citizenId:string; department:string; priority:string; status:string; createdAt:number; updatedAt:number; dueAt:number; version:number; escalation:number; closureBy:string|null }
export interface CaseTask { id:string; caseId:string; department:string; title:string; required:boolean; complete:boolean }
export interface CaseEvent { id:string; caseId:string; actorId:string; action:string; note:string; at:number }
export interface CaseDetail { case:CaseRecord; tasks:CaseTask[]; events:CaseEvent[]; links?:CaseLink[] }
export interface LegalSource { id:string; title:string; url:string; jurisdiction:string; language:string; content:string; retrievedAt:number; status:string; hash:string; supersedes:string|null }
export interface LegalAnswer { answer:string; citations:LegalSource[]; mode:string; limitation:string }
export interface LegalMonitor { id:string; remoteMonitorId:string|null; name:string; monitorType:string; target:string; jurisdiction:string; language:string; scheduleCron:string; status:string; lastCheckedAt:number|null; lastStatus:string|null }
export interface MonitorRunResult { monitor:LegalMonitor; queuedSources:LegalSource[]; providerMode:string }
export interface TriageSuggestion {
  suggestedTitle: string;
  suggestedCategory: string;
  suggestedPriority: string;
  suggestedDepartment: string;
  duplicateCaseIds: string[];
  summary: string;
  provider: string;
}

export interface TranscribeResponse {
  text: string;
  suggestedTitle?: string;
  suggestedCategory?: string;
  provider?: string;
}

export interface ReverseGeocodeResult {
  latitude: number;
  longitude: number;
  address: string;
  ward: string;
  landmark: string;
  source: string;
}

export let identity: Identity | null = null;

const TOKEN_STORAGE_KEY = 'theekhai_supabase_token';

function getStoredToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

function setStoredToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
    else sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  } catch {
    // ignore storage errors
  }
}

export async function api<T>(path:string, options:RequestInit & { timeoutMs?: number } = {}):Promise<T> {
  const headers = new Headers(options.headers);
  if(options.body) headers.set('Content-Type','application/json');
  if(identity?.csrf) headers.set('X-CSRF-Token',identity.csrf);
  const bearer = getStoredToken();
  if(bearer && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${bearer}`);
  }
  const timeoutMs = options.timeoutMs ?? 45000;
  const { timeoutMs: _t, ...fetchOptions } = options;
  const response = await fetch(`/api/v1${path}`,{...fetchOptions,headers,credentials:'same-origin',signal:options.signal ?? AbortSignal.timeout(timeoutMs)});
  const data = await response.json().catch(() => null);
  if(!response.ok) throw new Error(data?.message ?? `Request failed (${response.status}). Please retry.`);
  return data as T;
}

export async function loadIdentity() {
  try {
    identity = await api<Identity>('/auth/me');
  } catch {
    identity = null;
  }
  return identity;
}

export async function getAuthConfig() {
  return api<AuthConfig>('/auth/config');
}

export async function signIn(email:string, password:string) {
  const res = await api<{ status: string; accessToken?: string }>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  if (res?.accessToken) setStoredToken(res.accessToken);
  return loadIdentity();
}

export async function registerAccount(input: RegisterInput) {
  const res = await api<{ status: string; accessToken?: string }>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  });
  if (res?.accessToken) setStoredToken(res.accessToken);
  return loadIdentity();
}

export async function requestPasswordReset(email: string) {
  return api<{ status: string }>('/auth/recover', { method: 'POST', body: JSON.stringify({ email }) });
}

export async function signOut() {
  try {
    await api('/auth/logout', { method: 'POST' });
  } finally {
    setStoredToken(null);
    identity = null;
  }
}

export async function getProfile() {
  return api<Profile>('/profile');
}

export async function saveProfile(profile: Partial<Profile>) {
  const updated = await api<Profile>('/profile', { method: 'POST', body: JSON.stringify(profile) });
  await loadIdentity();
  return updated;
}

export async function getSavedSources() {
  return api<SavedSource[]>('/saved-sources');
}

export async function saveSourceBookmark(input: { title: string; sourceUrl: string; jurisdiction: string; snippet: string }) {
  return api<SavedSource>('/saved-sources', { method: 'POST', body: JSON.stringify(input) });
}

export async function removeSavedSource(id: string) {
  return api<{ status: string }>(`/saved-sources/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export async function triageComplaint(input: { title: string; description: string; address: string; language: string }) {
  return api<TriageSuggestion>('/complaints/triage', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function transcribeAudio(input: { audioBase64: string; mimeType: string; language: string; transcriptHint?: string }) {
  return api<TranscribeResponse>('/voice/transcribe', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function reverseGeocode(lat: number, lon: number, lang: string = 'en') {
  return api<ReverseGeocodeResult>(`/geo/reverse?lat=${encodeURIComponent(String(lat))}&lon=${encodeURIComponent(String(lon))}&lang=${encodeURIComponent(lang)}`);
}

export function verifyCaseResolution(complaintId: string, officialPortalUrl?: string) {
  return api<VerificationResult>(`/complaints/${encodeURIComponent(complaintId)}/verify`, {
    method: 'POST',
    body: JSON.stringify({ complaintId, officialPortalUrl }),
  });
}

export function importLegalSource(input: { title: string; url: string; jurisdiction: string; language: string; content: string }) {
  return api<LegalSource>('/legal/sources', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export interface EmailDraft {
  caseId: string;
  senderEmail: string;
  recipientEmail: string;
  recipientDepartment: string;
  recipientAuthority: string;
  officialSourceUrl: string;
  subject: string;
  body: string;
  discoveryMode: string;
  timestamp: number;
}

export interface SendEmailResponse {
  success: boolean;
  messageId: string;
  sender: string;
  recipient: string;
  sentAt: number;
  message: string;
}

export interface GmailOAuthStatus {
  configured: boolean;
  connected: boolean;
  senderEmail: string;
  authUrl?: string;
}

export function getGmailStatus() {
  return api<GmailOAuthStatus>('/gmail/status');
}

export function getGmailAuthUrl() {
  return api<{ authUrl: string }>('/gmail/auth-url');
}

export function draftComplaintEmail(caseId: string) {
  return api<EmailDraft>(`/complaints/${encodeURIComponent(caseId)}/email/draft`, {
    method: 'POST',
    body: JSON.stringify({ caseId }),
  });
}

export function sendComplaintEmail(caseId: string, recipientEmail: string, subject: string, body: string) {
  return api<SendEmailResponse>(`/complaints/${encodeURIComponent(caseId)}/email/send`, {
    method: 'POST',
    body: JSON.stringify({ recipientEmail, subject, body }),
  });
}


export interface DummyUserCredential {
  id: string;
  shortName: string;
  fullName: string;
  email: string;
  password: string;
  role: string;
  userType: 'AUTHORITY' | 'REGULAR';
  department?: string | null;
  ward: string;
  score: number;
}

export interface ScoreEvent {
  id: string;
  userId: string;
  userName: string;
  caseId?: string | null;
  reasonCode: string;
  pointsDelta: number;
  description: string;
  createdAt: number;
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  fullName: string;
  email: string;
  role: string;
  userType: 'AUTHORITY' | 'REGULAR';
  department?: string | null;
  ward: string;
  score: number;
  actionsTakenCount: number;
  complaintsSubmittedCount: number;
  complaintsApprovedCount: number;
  badge: string;
}

export interface LeaderboardResponse {
  entries: LeaderboardEntry[];
  recentActivity: ScoreEvent[];
  totalParticipants: number;
  totalPointsAwarded: number;
  updatedAtEpochMillis: number;
}

export async function instantSignIn(email: string) {
  const res = await api<{ status: string; accessToken?: string }>('/auth/instant-login', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
  if (res?.accessToken) setStoredToken(res.accessToken);
  return loadIdentity();
}

export function getDummyUsers() {
  return api<DummyUserCredential[]>('/auth/dummy-users');
}

export function getLeaderboard(role: string = 'ALL') {
  return api<LeaderboardResponse>(`/leaderboard?role=${encodeURIComponent(role)}`);
}
