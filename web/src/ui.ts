import Lenis from 'lenis';

export type Language = 'en'|'hi';
function readPreference(key:string):string|null { try { return localStorage.getItem(key); } catch { return null; } }
function writePreference(key:string,value:string) { try { localStorage.setItem(key,value); } catch { /* Preferences remain usable for this session. */ } }
export let language:Language = readPreference('theekhai-language') === 'hi' ? 'hi' : 'en';
export const t = (en:string,hi:string) => language === 'en' ? en : hi;
export function switchLanguage() { language = language === 'en' ? 'hi' : 'en'; writePreference('theekhai-language',language); document.documentElement.lang = language; }
export type Theme = 'light'|'dark';
function systemTheme():Theme { return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'; }
export let theme:Theme = readPreference('theekhai-theme') === 'dark' || readPreference('theekhai-theme') === 'light' ? readPreference('theekhai-theme') as Theme : systemTheme();
const THEME_FADE_MS = 1740;
const THEME_FADE_STYLE_ID = 'theekhai-theme-fade';
const ARC_RADIUS = 115;
const HALF_ARC = Math.PI / 2;
const themeFlights = new WeakMap<HTMLButtonElement, Animation[]>();

function applyTheme(animatePage = false) {
  if (animatePage && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    const style = document.getElementById(THEME_FADE_STYLE_ID) ?? document.head.appendChild(Object.assign(document.createElement('style'), { id: THEME_FADE_STYLE_ID }));
    style.textContent = `*,*::before,*::after{transition-property:color,background-color,border-color,outline-color,text-decoration-color,fill,stroke!important;transition-duration:${THEME_FADE_MS}ms!important;transition-timing-function:cubic-bezier(.65,0,.35,1)!important;transition-delay:0s!important}`;
    window.setTimeout(() => style.remove(), THEME_FADE_MS + 100);
  }
  document.documentElement.dataset.theme = theme;
  document.documentElement.classList.toggle('dark', theme === 'dark');
  document.documentElement.style.colorScheme = theme;
}

function bodyFrames(side: -1 | 1, spin: number, rising: boolean): Keyframe[] {
  return Array.from({ length: 31 }, (_, i) => {
    const t = i / 30;
    const progress = rising ? 1 - (1 + 2.4 * (t - 1) ** 3 + 1.4 * (t - 1) ** 2) : t ** 3;
    const x = side * ARC_RADIUS * Math.sin(progress * HALF_ARC);
    const y = ARC_RADIUS * (1 - Math.cos(progress * HALF_ARC));
    return {
      offset: t,
      transform: `translate(${x}%,${y}%) rotate(${side * spin * progress}deg)${rising ? ` scale(${1 - 0.28 * progress})` : ''}`,
      opacity: rising ? Math.min(1, t / 0.3) : t < 0.55 ? 1 : (1 - t) / 0.45,
    };
  });
}

function svgIcon(markup: string): SVGElement {
  const wrapper = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  wrapper.setAttribute('viewBox', '0 0 24 24');
  wrapper.setAttribute('aria-hidden', 'true');
  wrapper.innerHTML = markup;
  return wrapper;
}

function createThemeToggle(): HTMLButtonElement {
  const control = document.createElement('button');
  control.type = 'button';
  control.className = 'theme-toggle';
  control.setAttribute('aria-pressed', String(theme === 'dark'));
  control.setAttribute('aria-label', t(theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme', theme === 'dark' ? 'लाइट थीम चालू करें' : 'डार्क थीम चालू करें'));

  const night = document.createElement('span'); night.className = 'theme-sky theme-sky-night'; night.setAttribute('aria-hidden', 'true');
  const day = document.createElement('span'); day.className = 'theme-sky theme-sky-day'; day.setAttribute('aria-hidden', 'true');
  for (const [x, y, r] of [[24,22,1.6],[66,16,1.2],[80,42,1.8],[38,58,1.1],[58,74,1.4],[14,72,1.2]]) {
    const star = document.createElement('i'); star.className = 'theme-star'; star.style.left = `${x}%`; star.style.top = `${y}%`; star.style.width = `${r * 2}px`; star.style.height = `${r * 2}px`; night.append(star);
  }
  const moon = document.createElement('span'); moon.className = 'theme-body theme-moon'; moon.setAttribute('aria-hidden', 'true');
  moon.append(svgIcon('<path d="M20.8 15.5A8.6 8.6 0 0 1 8.5 3.2 8.7 8.7 0 1 0 20.8 15.5Z" fill="currentColor" stroke="currentColor" stroke-linejoin="round" stroke-width="1.5"/>'));
  const sun = document.createElement('span'); sun.className = 'theme-body theme-sun'; sun.setAttribute('aria-hidden', 'true');
  sun.append(svgIcon('<circle cx="12" cy="12" r="4" fill="currentColor"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" fill="none" stroke="currentColor" stroke-linecap="round" stroke-width="1.8"/>'));
  control.append(night, day, moon, sun);
  const setRestingBodies = () => {
    const dark = theme === 'dark';
    moon.style.transform = dark ? 'none' : 'translate(-115%,115%)'; moon.style.opacity = dark ? '1' : '0';
    sun.style.transform = dark ? 'translate(115%,115%)' : 'none'; sun.style.opacity = dark ? '0' : '1';
    night.style.opacity = dark ? '1' : '0'; day.style.opacity = dark ? '0' : '1';
    control.setAttribute('aria-pressed', String(dark));
    control.setAttribute('aria-label', t(dark ? 'Switch to light theme' : 'Switch to dark theme', dark ? 'लाइट थीम चालू करें' : 'डार्क थीम चालू करें'));
  };
  setRestingBodies();
  control.addEventListener('click', () => {
    const next: Theme = theme === 'dark' ? 'light' : 'dark';
    const flights = themeFlights.get(control) ?? [];
    if (flights.length) flights.forEach(animation => animation.reverse());
    else if (!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      const toLight = next === 'light';
      const settingBody = toLight ? moon : sun;
      const risingBody = toLight ? sun : moon;
      const setting = settingBody.animate(bodyFrames(toLight ? -1 : 1, toLight ? 25 : 120, false), { duration: THEME_FADE_MS * 0.65, fill: 'forwards' });
      const rising = risingBody.animate(bodyFrames(toLight ? 1 : -1, toLight ? 120 : 25, true), { duration: THEME_FADE_MS * 0.64, delay: THEME_FADE_MS * 0.36, fill: 'forwards' });
      const nightFade = night.animate([{ opacity: toLight ? 1 : 0 }, { opacity: toLight ? 0 : 1 }], { duration: THEME_FADE_MS, fill: 'forwards' });
      const dayFade = day.animate([{ opacity: toLight ? 0 : 1 }, { opacity: toLight ? 1 : 0 }], { duration: THEME_FADE_MS, fill: 'forwards' });
      const active = [setting, rising, nightFade, dayFade];
      themeFlights.set(control, active);
      Promise.all(active.map(animation => animation.finished)).then(() => {
        if (themeFlights.get(control) !== active) return;
        themeFlights.delete(control);
        active.forEach(animation => animation.cancel());
        setRestingBodies();
      }).catch(() => undefined);
    }
    theme = next;
    writePreference('theekhai-theme', theme);
    applyTheme(true);
    setRestingBodies();
  });
  return control;
}

applyTheme();
if(readPreference('theekhai-theme') === null && window.matchMedia) window.matchMedia('(prefers-color-scheme: dark)').addEventListener?.('change',event=>{theme=event.matches?'dark':'light';applyTheme()});

export const lenis: Lenis | null =
  typeof window !== 'undefined' && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    ? new Lenis({
        autoRaf: true,
        lerp: 0.1,
        duration: 1.15,
        smoothWheel: true,
        wheelMultiplier: 1,
        touchMultiplier: 1.5,
      })
    : null;

export function scrollToTop(immediate = false) {
  if (lenis) {
    lenis.scrollTo(0, { immediate });
  } else {
    window.scrollTo({ top: 0, left: 0, behavior: immediate ? 'auto' : 'smooth' });
  }
}

export function refreshScroll() {
  if (lenis) {
    requestAnimationFrame(() => lenis.resize());
  }
}

export { createThemeToggle };

export function el<K extends keyof HTMLElementTagNameMap>(tag:K, className='', text=''):HTMLElementTagNameMap[K] { const node=document.createElement(tag); node.className=className; node.textContent=text; return node; }
export function button(text:string, action:()=>void, className='button') { const node=el('button',className,text); node.type='button'; node.addEventListener('click',action); return node; }
export function link(text:string,path:string,className='') { const node=el('a',className,text); node.href=path; return node; }
export function notice(container:HTMLElement,message:string,error=false) { const node=el('div',`notice ${error?'error':''}`,message); node.setAttribute('role',error?'alert':'status'); container.replaceChildren(node); refreshScroll(); }
export function field(label:string,name:string,type='text',required=true) { const wrapper=el('label','field'); wrapper.append(el('span','field-label',label)); const input=el('input'); input.name=name; input.type=type; input.required=required; wrapper.append(input); return {wrapper,input}; }
export function date(value:number) { const parsed=typeof value==='number'?value:Date.parse(String(value)); if(!Number.isFinite(parsed)) return t('Date unavailable','तारीख़ उपलब्ध नहीं'); return new Intl.DateTimeFormat(language==='en'?'en-IN':'hi-IN',{day:'numeric',month:'short',hour:'numeric',minute:'2-digit'}).format(parsed); }
export const statusText = (value:string) => ({SUBMITTED:t('Submitted','दर्ज'),ASSIGNED:t('Assigned','आवंटित'),IN_PROGRESS:t('In progress','कार्य जारी'),PENDING_VERIFICATION:t('Awaiting review','समीक्षा बाकी'),CLOSED:t('Closed','बंद'),REOPENED:t('Reopened','फिर खोला गया')}[value] ?? value);
