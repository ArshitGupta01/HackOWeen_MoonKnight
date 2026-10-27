export interface Identity { id: string; role: 'CITIZEN'|'DEPARTMENT'|'REVIEWER'|'ADMIN'; department: string|null; csrf: string|null }
export interface CaseRecord { id:string; title:string; description:string; category:string; address:string; latitude:number|null; longitude:number|null; citizenId:string; department:string; priority:string; status:string; createdAt:number; updatedAt:number; dueAt:number; version:number; escalation:number; closureBy:string|null }
export interface CaseTask { id:string; caseId:string; department:string; title:string; required:boolean; complete:boolean }
export interface CaseEvent { id:string; caseId:string; actorId:string; action:string; note:string; at:number }
export interface CaseDetail { case:CaseRecord; tasks:CaseTask[]; events:CaseEvent[] }
export interface LegalSource { id:string; title:string; url:string; jurisdiction:string; language:string; content:string; retrievedAt:number; status:string; hash:string; supersedes:string|null }
export interface LegalAnswer { answer:string; citations:LegalSource[]; mode:string; limitation:string }
export let identity: Identity|null = null;
export async function api<T>(path:string, options:RequestInit = {}):Promise<T> {
  const headers = new Headers(options.headers);
  if(options.body) headers.set('Content-Type','application/json');
  if(identity?.csrf) headers.set('X-CSRF-Token',identity.csrf);
  const response = await fetch(`/api/v1${path}`,{...options,headers,credentials:'same-origin',signal:options.signal ?? AbortSignal.timeout(25000)});
  const data = await response.json().catch(() => null);
  if(!response.ok) throw new Error(data?.message ?? `Request failed (${response.status}). Please retry.`);
  return data as T;
}
export async function loadIdentity() { try { identity = await api<Identity>('/auth/me'); } catch { identity = null; } return identity; }
export async function signIn(email:string,password:string) { await api('/auth/login',{method:'POST',body:JSON.stringify({email,password})}); return loadIdentity(); }
export async function signOut() { await api('/auth/logout',{method:'POST'}); identity = null; }
