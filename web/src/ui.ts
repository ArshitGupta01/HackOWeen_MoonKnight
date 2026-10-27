export type Language = 'en'|'hi';
function readPreference(key:string):string|null { try { return localStorage.getItem(key); } catch { return null; } }
function writePreference(key:string,value:string) { try { localStorage.setItem(key,value); } catch { /* Preferences remain usable for this session. */ } }
export let language:Language = readPreference('theekhai-language') === 'hi' ? 'hi' : 'en';
export const t = (en:string,hi:string) => language === 'en' ? en : hi;
export function switchLanguage() { language = language === 'en' ? 'hi' : 'en'; writePreference('theekhai-language',language); document.documentElement.lang = language; }
export type Theme = 'light'|'dark';
function systemTheme():Theme { return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'; }
export let theme:Theme = readPreference('theekhai-theme') === 'dark' || readPreference('theekhai-theme') === 'light' ? readPreference('theekhai-theme') as Theme : systemTheme();
export function applyTheme() { document.documentElement.dataset.theme = theme; document.documentElement.style.colorScheme = theme; }
export function switchTheme() { theme = theme === 'light' ? 'dark' : 'light'; writePreference('theekhai-theme',theme); applyTheme(); }
applyTheme();
if(readPreference('theekhai-theme') === null && window.matchMedia) window.matchMedia('(prefers-color-scheme: dark)').addEventListener?.('change',event=>{theme=event.matches?'dark':'light';applyTheme()});
export function el<K extends keyof HTMLElementTagNameMap>(tag:K, className='', text=''):HTMLElementTagNameMap[K] { const node=document.createElement(tag); node.className=className; node.textContent=text; return node; }
export function button(text:string, action:()=>void, className='button') { const node=el('button',className,text); node.type='button'; node.addEventListener('click',action); return node; }
export function link(text:string,path:string,className='') { const node=el('a',className,text); node.href=path; return node; }
export function notice(container:HTMLElement,message:string,error=false) { const node=el('div',`notice ${error?'error':''}`,message); node.setAttribute('role',error?'alert':'status'); container.replaceChildren(node); }
export function field(label:string,name:string,type='text',required=true) { const wrapper=el('label','field'); wrapper.append(el('span','field-label',label)); const input=el('input'); input.name=name; input.type=type; input.required=required; wrapper.append(input); return {wrapper,input}; }
export function date(value:number) { const parsed=typeof value==='number'?value:Date.parse(String(value)); if(!Number.isFinite(parsed)) return t('Date unavailable','तारीख़ उपलब्ध नहीं'); return new Intl.DateTimeFormat(language==='en'?'en-IN':'hi-IN',{day:'numeric',month:'short',hour:'numeric',minute:'2-digit'}).format(parsed); }
export const statusText = (value:string) => ({SUBMITTED:t('Submitted','दर्ज'),ASSIGNED:t('Assigned','आवंटित'),IN_PROGRESS:t('In progress','कार्य जारी'),PENDING_VERIFICATION:t('Awaiting review','समीक्षा बाकी'),CLOSED:t('Closed','बंद'),REOPENED:t('Reopened','फिर खोला गया')}[value] ?? value);
