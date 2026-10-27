import './style.css';
import {identity,loadIdentity,signOut} from './api';
import {el,link,button,t,switchLanguage,language} from './ui';
import {home,report,workspace,detail,login,library,information} from './screens';

let cleanup: (()=>void)|undefined;
export async function render() {
  cleanup?.(); cleanup=undefined;
  document.documentElement.lang=language;
  const root=document.getElementById('app')!;
  const sidebar=el('aside','sidebar');
  const brand=link('','/','brand'); const logo=el('img'); logo.src='/mark.svg'; logo.alt=''; brand.append(logo,el('span','','TheekHai')); sidebar.append(brand,el('p','brand-caption',t('A BETTER EVERYDAY','बेहतर हर दिन')));
  const nav=el('nav','nav'); nav.setAttribute('aria-label',t('Main navigation','मुख्य नेविगेशन'));
  const route=location.pathname;
  const links=[['/','01',t('Overview','परिचय')],['/report','02',t('Report an issue','समस्या दर्ज करें')],['/workspace','03',t('My workspace','मेरा कार्यक्षेत्र')],['/library','04',t('Know your options','अपने विकल्प जानें')]];
  for(const [path,num,label] of links) { const item=link('',path,`nav-item ${route===path?'active':''}`); item.append(el('span','nav-number',num),el('span','',label)); if(route===path)item.setAttribute('aria-current','page'); nav.append(item); }
  sidebar.append(nav);
  const sideFoot=el('div','side-foot');sideFoot.append(el('span','pilot-dot'),el('strong','',t('Civic pilot','नागरिक पायलट')),el('p','',t('City-neutral. Community-focused.','हर शहर के लिए। समुदाय के साथ।')),link(t('Help & permissions','सहायता और अनुमतियाँ'),'/help'));sidebar.append(sideFoot);
  const page=el('div','page');const header=el('header','topbar');header.append(el('span','location-label',t('YOUR NEIGHBOURHOOD, BETTER TOGETHER','आपका पड़ोस, मिलकर बेहतर')));
  const tools=el('div','header-tools');tools.append(button(language==='en'?'हिन्दी':'English',()=>{switchLanguage();void render()},'language'));
  if(identity)tools.append(button(t('Sign out','साइन आउट'),()=>{void signOut().then(()=>render()).catch(()=>{})},'quiet'));
  else tools.append(link(t('Sign in','साइन इन'),'/login','quiet'));
  tools.append(link(t('Report an issue','समस्या दर्ज करें'),'/report','button small'));header.append(tools);
  const main=el('main');main.id='main';main.tabIndex=-1;
  const footer=el('footer','footer');footer.append(el('span','','TheekHai · Civic pilot'),link(t('Privacy','गोपनीयता'),'/privacy'),link(t('Terms','शर्तें'),'/terms'),el('span','footer-note',t('Built around accountability.','जवाबदेही के लिए बनाया गया।')));
  page.append(header,main,footer);root.replaceChildren(sidebar,page);
  if(route==='/') home(main);
  else if(route==='/report') cleanup=report(main);
  else if(route==='/workspace') await workspace(main);
  else if(route.startsWith('/cases/'))await detail(main,route.split('/')[2]);
  else if(route==='/login')login(main);
  else if(route==='/library')library(main);
  else information(main,route);
}
document.addEventListener('click',event=>{const anchor=(event.target as Element).closest('a');if(anchor?.origin===location.origin && !anchor.hash && !anchor.target && !event.ctrlKey && !event.metaKey){event.preventDefault();history.pushState({},'',anchor.pathname+anchor.search);void render();window.scrollTo(0,0)}});
window.addEventListener('popstate',()=>void render());
void render();void loadIdentity().then(()=>render());
