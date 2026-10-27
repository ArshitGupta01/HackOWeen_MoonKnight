export type Language = 'en'|'hi';
export let language:Language = localStorage.getItem('theekhai-language') === 'hi' ? 'hi' : 'en';
export const t = (en:string,hi:string) => language === 'en' ? en : hi;
export function switchLanguage() { language = language === 'en' ? 'hi' : 'en'; localStorage.setItem('theekhai-language',language); document.documentElement.lang = language; }
export function el<K extends keyof HTMLElementTagNameMap>(tag:K, className='', text=''):HTMLElementTagNameMap[K] { const node=document.createElement(tag); node.className=className; node.textContent=text; return node; }
export function button(text:string, action:()=>void, className='button') { const node=el('button',className,text); node.type='button'; node.addEventListener('click',action); return node; }
export function link(text:string,path:string,className='') { const node=el('a',className,text); node.href=path; return node; }
export function notice(container:HTMLElement,message:string,error=false) { const node=el('div',`notice ${error?'error':''}`,message); node.setAttribute('role',error?'alert':'status'); container.replaceChildren(node); }
export function field(label:string,name:string,type='text',required=true) { const wrapper=el('label','field'); wrapper.append(el('span','field-label',label)); const input=el('input'); input.name=name; input.type=type; input.required=required; wrapper.append(input); return {wrapper,input}; }
export function date(value:number) { return new Intl.DateTimeFormat(language==='en'?'en-IN':'hi-IN',{day:'numeric',month:'short',hour:'numeric',minute:'2-digit'}).format(value); }
export const statusText = (value:string) => ({SUBMITTED:t('Submitted','दर्ज'),ASSIGNED:t('Assigned','आवंटित'),IN_PROGRESS:t('In progress','कार्य जारी'),PENDING_VERIFICATION:t('Awaiting review','समीक्षा बाकी'),CLOSED:t('Closed','बंद'),REOPENED:t('Reopened','फिर खोला गया')}[value] ?? value);
