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
  supabasePublishableKey: string;
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

export interface CaseRecord { id:string; title:string; description:string; category:string; address:string; latitude:number|null; longitude:number|null; citizenId:string; department:string; priority:string; status:string; createdAt:number; updatedAt:number; dueAt:number; version:number; escalation:number; closureBy:string|null }
export interface CaseTask { id:string; caseId:string; department:string; title:string; required:boolean; complete:boolean }
export interface CaseEvent { id:string; caseId:string; actorId:string; action:string; note:string; at:number }
export interface CaseDetail { case:CaseRecord; tasks:CaseTask[]; events:CaseEvent[] }
export interface LegalSource { id:string; title:string; url:string; jurisdiction:string; language:string; content:string; retrievedAt:number; status:string; hash:string; supersedes:string|null }
export interface LegalAnswer { answer:string; citations:LegalSource[]; mode:string; limitation:string }
export interface LegalMonitor { id:string; remoteMonitorId:string|null; name:string; monitorType:string; target:string; jurisdiction:string; language:string; scheduleCron:string; status:string; lastCheckedAt:number|null; lastStatus:string|null }
export interface MonitorRunResult { monitor:LegalMonitor; queuedSources:LegalSource[]; providerMode:string }

export let identity: Identity | null = null;

export async function api<T>(path:string, options:RequestInit = {}):Promise<T> {
  const headers = new Headers(options.headers);
  if(options.body) headers.set('Content-Type','application/json');
  if(identity?.csrf) headers.set('X-CSRF-Token',identity.csrf);
  const response = await fetch(`/api/v1${path}`,{...options,headers,credentials:'same-origin',signal:options.signal ?? AbortSignal.timeout(25000)});
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
  await api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
  return loadIdentity();
}

export async function registerAccount(input: RegisterInput) {
  await api('/auth/register', { method: 'POST', body: JSON.stringify(input) });
  return loadIdentity();
}

export async function requestPasswordReset(email: string) {
  return api<{ status: string }>('/auth/recover', { method: 'POST', body: JSON.stringify({ email }) });
}

export async function signOut() {
  await api('/auth/logout', { method: 'POST' });
  identity = null;
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
