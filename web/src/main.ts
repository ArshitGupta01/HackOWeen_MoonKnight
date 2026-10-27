import './style.css';
import {identity,loadIdentity,signOut} from './api';
import {el,link,button,t,switchLanguage,language,theme,switchTheme,scrollToTop,refreshScroll} from './ui';
import {home,report,workspace,detail,login,signup,account,library,information,civicMap,leaderboard} from './screens';

function initialsForUser(name?: string | null, email?: string | null): string {
  const source = (name || email || 'TH').trim();
  const parts = source.split(/[\s._@-]+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return source.slice(0, 2).toUpperCase();
}

let cleanup: (()=>void)|undefined;
export async function render() {
  cleanup?.(); cleanup=undefined;
  document.documentElement.lang=language;
  const root=document.getElementById('app')!;
  const sidebar=el('aside','sidebar');
  sidebar.setAttribute('data-lenis-prevent', '');
  const brand=link('','/','brand'); const logo=el('img'); logo.src='/mark.svg'; logo.alt=''; brand.append(logo,el('span','','TheekHai')); sidebar.append(brand,el('p','brand-caption',t('A BETTER EVERYDAY','बेहतर हर दिन')));
  const nav=el('nav','nav'); nav.setAttribute('aria-label',t('Main navigation','मुख्य नेविगेशन'));
  const route=location.pathname;
  const links: [string, string, string][] = [
    ['/','01',t('Overview','परिचय')],
    ['/report','02',t('Report an issue','समस्या दर्ज करें')],
    ['/workspace','03',t('My workspace','मेरा कार्यक्षेत्र')],
    ['/map','04',t('Civic Map','नागरिक मानचित्र')],
    ['/leaderboard','05',t('Leaderboard','लीडरबोर्ड')],
    ['/library','06',t('Know your options','अपने विकल्प जानें')],
  ];
  if (identity) {
    links.push(['/account','07',t('My account','मेरा खाता')]);
  }
  for(const [path,num,label] of links) {
    const isActive = route === path || (path === '/workspace' && route.startsWith('/cases/'));
    const item=link('',path,`nav-item ${isActive?'active':''}`);
    item.append(el('span','nav-number',num),el('span','',label));
    if(isActive)item.setAttribute('aria-current','page');
    nav.append(item);
  }
  sidebar.append(nav);
  const sideFoot=el('div','side-foot');sideFoot.append(el('span','pilot-dot'),el('strong','',t('Community civic pilot','समुदाय-केंद्रित नागरिक पायलट')),el('p','',t('City-neutral. Community-focused.','हर शहर के लिए। समुदाय के साथ।')),link(t('Help & permissions','सहायता और अनुमतियाँ'),'/help'));sidebar.append(sideFoot);
  const page=el('div','page');const header=el('header','topbar');header.append(el('span','location-label',t('YOUR NEIGHBOURHOOD, BETTER TOGETHER','आपका पड़ोस, मिलकर बेहतर')));
  const tools=el('div','header-tools');
  const themeButton=button(theme==='dark'?t('☀ Light mode','☀ हल्का मोड'):t('☾ Dark mode','☾ डार्क मोड'),()=>{switchTheme();void render()},'language theme-toggle');themeButton.setAttribute('aria-label',theme==='dark'?t('Switch to light mode','लाइट मोड चालू करें'):t('Switch to dark mode','डार्क मोड चालू करें'));themeButton.setAttribute('aria-pressed',String(theme==='dark'));tools.append(themeButton,button(language==='en'?'हिन्दी':'English',()=>{switchLanguage();void render()},'language'));
  if(identity) {
    const accountChip = link('', '/account', 'account-chip');
    accountChip.append(
      el('span', 'avatar-mini', initialsForUser(identity.fullName, identity.email)),
      el('span', '', identity.fullName || identity.email || t('Account', 'खाता')),
      el('small', 'role-pill', identity.role),
    );
    tools.append(accountChip, button(t('Sign out','साइन आउट'),()=>{void signOut().then(()=>render()).catch(()=>{})},'quiet'));
  } else {
    tools.append(link(t('Sign in','साइन इन'),'/login','quiet'), link(t('Create account','खाता बनाएँ'),'/signup','quiet'));
  }
  tools.append(link(t('Report an issue','समस्या दर्ज करें'),'/report','button small'));header.append(tools);
  const main=el('main');main.id='main';main.tabIndex=-1;
  const footer=el('footer','footer');
  const topBtn=button(t('↑ Top','↑ ऊपर'),()=>scrollToTop(false),'quiet footer-top-btn');
  footer.append(el('span','','TheekHai · Civic pilot'),link(t('Privacy','गोपनीयता'),'/privacy'),link(t('Terms','शर्तें'),'/terms'),link(t('Civic Map','नागरिक मानचित्र'),'/map'),el('span','footer-note',t('Built around accountability.','जवाबदेही के लिए बनाया गया।')),topBtn);
  page.append(header,main,footer);root.replaceChildren(sidebar,page);
  if(route==='/') home(main);
  else if(route==='/report') cleanup=report(main);
  else if(route==='/workspace') await workspace(main);
  else if(route.startsWith('/cases/'))await detail(main,route.split('/')[2]);
  else if(route==='/login')login(main);
  else if(route==='/signup')signup(main);
  else if(route==='/account')await account(main);
  else if(route==='/map') await civicMap(main);
  else if(route==='/leaderboard') await leaderboard(main);
  else if(route==='/library')library(main);
  else information(main,route);
  refreshScroll();
}
document.addEventListener('click',event=>{const anchor=(event.target as Element).closest('a');if(anchor?.origin===location.origin && !anchor.hash && !anchor.target && !event.ctrlKey && !event.metaKey){event.preventDefault();history.pushState({},'',anchor.pathname+anchor.search);void render();scrollToTop(true)}});
window.addEventListener('popstate',()=>{void render();scrollToTop(true)});
void loadIdentity().finally(()=>render());
