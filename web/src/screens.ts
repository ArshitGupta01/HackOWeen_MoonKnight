import {
  api,
  identity,
  signIn,
  registerAccount,
  requestPasswordReset,
  getProfile,
  saveProfile,
  getSavedSources,
  saveSourceBookmark,
  removeSavedSource,
  triageComplaint,
  CaseRecord,
  CaseDetail,
  LegalAnswer,
  LegalSource,
  LegalMonitor,
  MonitorRunResult,
  SavedSource,
} from './api';
import {el,button,link,field,notice,t,date,statusText} from './ui';
import {render} from './main';

function heading(parent:HTMLElement,eyebrow:string,title:string,subtitle:string) { const block=el('div','page-heading');block.append(el('p','eyebrow',eyebrow),el('h1','',title),el('p','subtitle',subtitle));parent.append(block); }
function navigate(path:string) {history.pushState({},'',path);void render();window.scrollTo(0,0)}
function errorMessage(error:unknown) {return error instanceof Error?error.message:t('Something went wrong. Try again.','कुछ गलत हुआ। फिर कोशिश करें।')}
function authGate(parent:HTMLElement) {if(identity)return false;const block=el('div','empty-state'),actions=el('div','actions');actions.style.justifyContent='center';actions.append(link(t('Sign in','साइन इन'),'/login','button'),link(t('Create account','खाता बनाएँ'),'/signup','button secondary'));block.append(el('h2','',t('Your work, in one place.','आपका काम, एक जगह।')),el('p','',t('Sign in to submit a report or see your case history. You can prepare a draft first.','रिपोर्ट भेजने या इतिहास देखने के लिए साइन इन करें। पहले ड्राफ्ट तैयार कर सकते हैं।')),actions);parent.append(block);return true;}

function passwordField(label: string, name: string, autocomplete: AutoFill = 'current-password') {
  const wrapper = el('label', 'field');
  wrapper.append(el('span', 'field-label', label));
  const row = el('div', 'password-field-wrap');
  const input = el('input');
  input.type = 'password';
  input.name = name;
  input.required = true;
  input.autocomplete = autocomplete;
  input.placeholder = t('Enter password', 'पासवर्ड दर्ज करें');
  const toggleBtn = el('button', 'password-toggle-btn');
  toggleBtn.type = 'button';
  toggleBtn.title = t('Show password', 'पासवर्ड दिखाएँ');
  toggleBtn.setAttribute('aria-label', t('Show password', 'पासवर्ड दिखाएँ'));
  const eyeIcon = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
  const eyeOffIcon = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';
  toggleBtn.innerHTML = eyeIcon;
  toggleBtn.addEventListener('click', () => {
    const isPassword = input.type === 'password';
    input.type = isPassword ? 'text' : 'password';
    toggleBtn.innerHTML = isPassword ? eyeOffIcon : eyeIcon;
    toggleBtn.setAttribute('aria-label', isPassword ? t('Hide password', 'पासवर्ड छिपाएँ') : t('Show password', 'पासवर्ड दिखाएँ'));
  });
  row.append(input, toggleBtn);
  wrapper.append(row);
  return { wrapper, input };
}

export function home(parent:HTMLElement) {
  const hero=el('section','hero');const copy=el('div','hero-copy');copy.append(el('p','eyebrow',t('SMALL REPORTS. MEANINGFUL CHANGE.','छोटी रिपोर्ट। सार्थक बदलाव।')));
  const title=el('h1');title.append(document.createTextNode(t('A better city\nstarts with ','बेहतर शहर की\nशुरुआत ')),el('em','',t('you.','आपसे।')));copy.append(title,el('p','hero-description',t('The broken streetlight. The overflowing drain. The road that needs care. Report it, follow the work, and help make it right.','खराब स्ट्रीट लाइट। भरी हुई नाली। मरम्मत की राह देखती सड़क। रिपोर्ट करें, काम पर नज़र रखें और समाधान में साथ दें।')));
  const actions=el('div','actions');actions.append(link(t('Report a neighbourhood issue  ↗','पड़ोस की समस्या दर्ज करें  ↗'),'/report','button'),link(t('Follow your report  →','अपनी रिपोर्ट देखें  →'),'/workspace','text-link'));copy.append(actions,el('p','microcopy',t('A clear owner. A visible deadline. A resolution you can question.','स्पष्ट जिम्मेदारी। दिखाई देती समय-सीमा। समाधान पर सवाल करने का अधिकार।')));
  const art=el('div','city-art');art.setAttribute('aria-label',t('Illustrated neighbourhood with streets, buildings and a park','सड़कों, इमारतों और पार्क वाला चित्रित पड़ोस'));
  // Fixed decorative SVG contains no external data.
  art.innerHTML='<svg viewBox="0 0 520 440" role="img" aria-label="Neighbourhood illustration"><defs><pattern id="lines" width="8" height="8" patternUnits="userSpaceOnUse"><path d="M0 8 8 0" stroke="#c4d2b9" stroke-width="1"/></pattern></defs><rect width="520" height="440" fill="#e9e9dc"/><path d="M-20 145 540 320M100-30 270 470M470-30 80 470" stroke="#f8f6ed" stroke-width="37" fill="none"/><path d="M-20 145 540 320M100-30 270 470M470-30 80 470" stroke="#c9cbbb" stroke-width="1" stroke-dasharray="5 7" fill="none"/><path d="m287 35 115 45-90 113-95-32z" fill="url(#lines)"/><g fill="#c8d2b9" stroke="#aab89b"><circle cx="280" cy="90" r="21"/><circle cx="315" cy="108" r="27"/><circle cx="342" cy="69" r="17"/><circle cx="278" cy="143" r="18"/></g><g fill="#d1bba0" stroke="#ab977d" stroke-width="1.5"><path d="m24 45 66-19 32 80-66 22z"/><path d="m375 144 75 29-24 61-75-29z"/><path d="m54 254 69 28-28 67-69-28z"/><path d="m293 326 76-23 24 77-76 22z"/></g><g fill="#f6f0df" stroke="#b9a88d"><path d="m30 38 66-19 32 80-66 22z"/><path d="m381 137 75 29-24 61-75-29z"/><path d="m60 247 69 28-28 67-69-28z"/><path d="m299 319 76-23 24 77-76 22z"/></g><g fill="#b8c1b4"><rect x="188" y="232" width="35" height="18" rx="4" transform="rotate(18 188 232)"/><rect x="359" y="260" width="30" height="15" rx="4" transform="rotate(18 359 260)"/></g><g transform="translate(238 215)"><circle r="32" fill="#123e39" opacity=".1"/><circle r="21" fill="#123e39"/><path d="m-9 0 6 6 12-13" fill="none" stroke="#faf8ed" stroke-width="3" stroke-linecap="round"/></g><g transform="translate(398 163)"><circle r="17" fill="#bb762f"/><path d="M0-7v9m0 4v1" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/></g></svg>';
  const caption=el('div','map-caption');caption.append(el('span','dot'),el('span','',t('Every neighbourhood deserves care.','हर पड़ोस को देखभाल का हक है।')),el('small','',t('ILLUSTRATION · NOT LIVE DATA','चित्र · लाइव डेटा नहीं')));art.append(caption);hero.append(copy,art);parent.append(hero);
  const promise=el('section','promise-strip');for(const [n,title,body] of [['01',t('Tell us what happened','बताएँ क्या हुआ'),t('Write it. Say it. Add the location.','लिखें। बोलें। जगह बताएँ।')],['02',t('Know who owns it','जिम्मेदारी जानें'),t('Follow the department and the deadline.','विभाग और समय-सीमा देखें।')],['03',t('See it through','समाधान तक साथ'),t('Review the outcome. Challenge a closure.','परिणाम देखें। बंद होने पर सवाल करें।')]]){const item=el('div','promise');item.append(el('span','step-number',n),el('h3','',title),el('p','',body));promise.append(item)}parent.append(promise);
  const lower=el('section','home-bottom');const intro=el('div');intro.append(el('p','eyebrow',t('ONE SHARED RESPONSIBILITY','एक साझा जिम्मेदारी')),el('h2','',t('Less wondering.\nMore knowing.','कम अनिश्चितता।\nअधिक जानकारी।')),el('p','',t('Your report stays connected to the people, tasks and evidence behind its resolution.','आपकी रिपोर्ट उसके समाधान से जुड़े लोगों, कार्यों और प्रमाणों से जुड़ी रहती है।')));const panel=el('div','editorial-panel');panel.append(el('span','label',t('UNDERSTAND YOUR OPTIONS','अपने विकल्प समझें')),el('h3','',t('Clear guidance, with the source attached.','स्पष्ट मार्गदर्शन, स्रोत के साथ।')),el('p','',t('Explore approved civic guidance and the Haryana reference pack. Know what is available and when it was checked.','स्वीकृत नागरिक मार्गदर्शन और हरियाणा संदर्भ देखें। जानें क्या उपलब्ध है और कब जाँचा गया।')),link(t('Open the reference library  ↗','संदर्भ लाइब्रेरी खोलें  ↗'),'/library','text-link'));lower.append(intro,panel);parent.append(lower);
}

export function report(parent:HTMLElement):()=>void {
  heading(parent,t('YOUR VOICE MATTERS','आपकी बात महत्वपूर्ण है'),t('Let’s make it right.','आइए इसे ठीक करें।'),t('A few details help the right team understand the problem.','कुछ विवरण सही टीम को समस्या समझने में मदद करते हैं।'));
  const layout=el('div','form-layout'),form=el('form','panel report-form'),aside=el('aside','form-aside'),alert=el('div');
  const title=field(t('Give the issue a short title','समस्या का संक्षिप्त शीर्षक'),'title');title.input.minLength=5;title.input.maxLength=120;title.input.placeholder=t('e.g. Streetlight out near the crossing','जैसे चौराहे के पास स्ट्रीट लाइट खराब');
  const descriptionLabel=el('label','field');descriptionLabel.append(el('span','field-label',t('What is happening?','क्या हो रहा है?')));const description=el('textarea');description.name='description';description.rows=5;description.required=true;description.minLength=10;description.maxLength=5000;description.placeholder=t('Describe the issue and how it affects the neighbourhood.','समस्या और पड़ोस पर उसका असर बताएँ।');descriptionLabel.append(description);
  const categoryLabel=el('label','field');categoryLabel.append(el('span','field-label',t('Type of issue','समस्या का प्रकार')));const category=el('select');category.name='category';for(const [value,en,hi] of [['ROADS','Roads & footpaths','सड़क और फुटपाथ'],['WATER','Water & drains','पानी और नालियाँ'],['WASTE','Waste & sanitation','कचरा और सफाई'],['LIGHTING','Street lighting','स्ट्रीट लाइट'],['OTHER','Something else','अन्य']]){const option=el('option','',t(en,hi));option.value=value;category.append(option)}categoryLabel.append(category);
  const address=field(t('Address or landmark','पता या पहचान की जगह'),'address');address.input.minLength=3;address.input.maxLength=500;
  let coords:{latitude:number,longitude:number}|null=null;const geoText=el('p','microcopy',t('Location is optional. You can describe the address instead.','स्थान वैकल्पिक है। आप पता लिख सकते हैं।'));
  const geo=button(t('Use my location','मेरा स्थान लें'),()=>{if(!navigator.geolocation){notice(alert,t('Location is unavailable. Enter the address.','स्थान उपलब्ध नहीं है। पता लिखें।'),true);return}geo.disabled=true;navigator.geolocation.getCurrentPosition(pos=>{coords={latitude:pos.coords.latitude,longitude:pos.coords.longitude};geoText.textContent=`${coords.latitude.toFixed(5)}, ${coords.longitude.toFixed(5)} · ±${Math.round(pos.coords.accuracy)} m`;geo.disabled=false},()=>{geo.disabled=false;notice(alert,t('Location was not shared. Your address is enough to continue.','स्थान साझा नहीं हुआ। पता लिखकर आगे बढ़ें।'),true)},{timeout:10000,maximumAge:60000})},'button secondary');
  const consent=el('label','consent'),check=el('input');check.type='checkbox';check.required=true;consent.append(check,el('span','',t('I agree to share these details with the teams handling this report.','मैं यह विवरण रिपोर्ट पर काम करने वाली टीमों के साथ साझा करने के लिए सहमत हूँ।')));
  let media:MediaRecorder|null=null,stream:MediaStream|null=null;let chunks:BlobPart[]=[];let audio:Blob|null=null;
  const audioPanel=el('div','audio-panel'),audioStatus=el('p','microcopy',t('Prefer speaking? Record, then transcribe and edit before submitting.','बोलना पसंद है? रिकॉर्ड करें, फिर लिखित रूप जाँचकर भेजें।'));
  const record=button(t('Record a voice note','आवाज़ रिकॉर्ड करें'),()=>{void (async()=>{try{if(media?.state==='recording'){media.stop();return}stream=await navigator.mediaDevices.getUserMedia({audio:true});media=new MediaRecorder(stream);chunks=[];media.ondataavailable=e=>{if(e.data.size)chunks.push(e.data)};media.onstop=()=>{audio=new Blob(chunks,{type:media!.mimeType});stream?.getTracks().forEach(track=>track.stop());record.textContent=t('Record again','फिर रिकॉर्ड करें');transcribe.disabled=false;audioStatus.textContent=t('Recording ready. Transcribe it or continue with typed text.','रिकॉर्डिंग तैयार है। लिखित रूप लें या टाइप करें।')};media.start();record.textContent=t('Stop recording','रिकॉर्डिंग रोकें');audioStatus.textContent=t('Microphone is recording. Stop when finished.','माइक्रोफोन रिकॉर्ड कर रहा है। पूरा होने पर रोकें।');setTimeout(()=>{if(media?.state==='recording')media.stop()},60000)}catch{notice(alert,t('Microphone unavailable or permission denied. You can type your report.','माइक्रोफोन उपलब्ध नहीं या अनुमति नहीं मिली। रिपोर्ट टाइप करें।'),true)}})()},'button secondary');
  const transcribe=button(t('Transcribe','लिखित रूप लें'),()=>{void(async()=>{if(!audio)return;transcribe.disabled=true;try{const dataUrl=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=reject;reader.readAsDataURL(audio!)});const result=await api<{text:string}>('/voice/transcribe',{method:'POST',body:JSON.stringify({audioBase64:dataUrl.split(',')[1],mimeType:audio.type})});description.value=result.text;notice(alert,t('Review and edit the transcript before submitting.','भेजने से पहले लिखित रूप जाँचें और सुधारें।'))}catch(error){notice(alert,errorMessage(error),true)}finally{transcribe.disabled=false}})()},'quiet');transcribe.disabled=true;audioPanel.append(record,transcribe,audioStatus);
  const submit=el('button','button',t('Review & submit report  ↗','जाँचें और रिपोर्ट भेजें  ↗'));submit.type='submit';const draft=button(t('Save draft on this device','इस डिवाइस पर ड्राफ्ट सहेजें'),()=>{try{localStorage.setItem('theekhai-draft',JSON.stringify({title:title.input.value,description:description.value,address:address.input.value,category:category.value}));notice(alert,t('Draft saved on this device. It has not been submitted.','ड्राफ्ट इस डिवाइस पर सहेजा गया। अभी भेजा नहीं गया है。'))}catch{notice(alert,t('This browser could not save the draft. Copy your text before leaving.','यह ब्राउज़र ड्राफ़्ट नहीं सहेज सका। जाने से पहले अपना पाठ कॉपी करें।'),true)}},'quiet');
  try{const saved=JSON.parse(localStorage.getItem('theekhai-draft')??'null');if(saved){title.input.value=saved.title??'';description.value=saved.description??'';address.input.value=saved.address??'';category.value=saved.category??'ROADS'}}catch{try{localStorage.removeItem('theekhai-draft')}catch{/* Storage may be disabled. */}}
  const triageBox=el('div');
  const aiTriageBtn=button(t('AI Triage & Check Duplicates (OpenRouter)','AI विश्लेषण और डुप्लीकेट जाँच (OpenRouter)'),()=>{
    if(description.value.trim().length<5){notice(alert,t('Describe the issue first (at least 5 characters) to run OpenRouter AI triage.','ओपनराउटर AI विश्लेषण चलाने के लिए पहले समस्या का विवरण लिखें।'),true);description.focus();return}
    aiTriageBtn.disabled=true;
    notice(triageBox,t('Analyzing issue & checking neighbourhood duplicates via OpenRouter AI…','OpenRouter AI के माध्यम से समस्या और डुप्लीकेट मामलों की जाँच की जा रही है…'));
    void triageComplaint({title:title.input.value,description:description.value,address:address.input.value,language:document.documentElement.lang||'en'})
      .then(res=>{
        if(!title.input.value.trim()&&res.suggestedTitle)title.input.value=res.suggestedTitle;
        if(res.suggestedCategory)category.value=res.suggestedCategory;
        triageBox.replaceChildren();
        const card=el('div','demo-credential-card');
        card.append(
          el('span','label',`${res.provider} · ${res.suggestedCategory} · ${res.suggestedPriority}`),
          el('p','microcopy',`${t('Suggested Lead Department:','सुझाया गया विभाग:')} ${res.suggestedDepartment}`),
          el('p','microcopy',res.summary),
        );
        if(res.duplicateCaseIds.length){
          const dupRow=el('p','microcopy');
          dupRow.append(document.createTextNode(t('Potential existing neighbourhood reports: ','संभावित मौजूदा पड़ोस की रिपोर्ट: ')));
          res.duplicateCaseIds.forEach((dupId,idx)=>{
            if(idx>0)dupRow.append(document.createTextNode(', '));
            dupRow.append(link(dupId,`/cases/${dupId}`,'text-link'));
          });
          card.append(dupRow);
        }
        triageBox.append(card);
      })
      .catch(err=>notice(triageBox,errorMessage(err),true))
      .finally(()=>{aiTriageBtn.disabled=false});
  },'button secondary');
  const actions=el('div','actions');actions.append(submit,aiTriageBtn,draft);form.append(title.wrapper,categoryLabel,descriptionLabel,audioPanel,address.wrapper,geo,geoText,triageBox,consent,alert,actions);
  const submissionKey=crypto.randomUUID();form.addEventListener('submit',event=>{event.preventDefault();if(!identity){notice(alert,t('Save your draft, then sign in to submit.','ड्राफ्ट सहेजें, फिर भेजने के लिए साइन इन करें।'),true);return}void(async()=>{submit.disabled=true;try{const record=await api<CaseRecord>('/complaints',{method:'POST',headers:{'Idempotency-Key':submissionKey},body:JSON.stringify({title:title.input.value,description:description.value,category:category.value,address:address.input.value,latitude:coords?.latitude??null,longitude:coords?.longitude??null,consent:check.checked})});localStorage.removeItem('theekhai-draft');navigate(`/cases/${record.id}`)}catch(error){notice(alert,errorMessage(error),true)}finally{submit.disabled=false}})()});
  aside.append(el('p','eyebrow',t('WHAT HAPPENS NEXT','इसके बाद क्या होगा')),el('h2','',t('A report with\na way forward.','एक रिपोर्ट,\nआगे का रास्ता।')),el('p','',t('Your report goes to an accountable department. You can follow tasks, due dates and the final review from your workspace.','आपकी रिपोर्ट जिम्मेदार विभाग तक जाती है। कार्यक्षेत्र में काम, समय-सीमा और अंतिम समीक्षा देखें।')),el('div','aside-note',t('This is a civic pilot. It is not an emergency service.','यह नागरिक पायलट है। यह आपातकालीन सेवा नहीं है।')),link(t('How we use your information','आपकी जानकारी का उपयोग'),'/privacy','text-link'));
  layout.append(form,aside);parent.append(layout);return()=>{if(media?.state==='recording')media.stop();stream?.getTracks().forEach(track=>track.stop())};
}

export function login(parent:HTMLElement) {
  heading(
    parent,
    t('CIVIC ACCOUNT ACCESS · SUPABASE RLS', 'नागरिक खाता पहुँच · सुपाबेस RLS'),
    t('Keep the conversation going.', 'बात आगे बढ़ाएँ।'),
    t('Sign in with your verified account or use a 1-click shared pilot role below.', 'अपने सत्यापित खाते से साइन इन करें या नीचे दिए गए पायलट रोल का उपयोग करें।'),
  );
  const shell = el('div', 'auth-shell');
  const form = el('form', 'panel login-form');
  const email = field(t('Email address', 'ईमेल पता'), 'email', 'email');
  email.input.autocomplete = 'email';
  email.input.placeholder = 'citizen@theekhai.in';
  const password = passwordField(t('Password', 'पासवर्ड'), 'password', 'current-password');
  const alert = el('div');

  const metaRow = el('div', 'auth-meta-row');
  const forgotBtn = button(
    t('Forgot password?', 'पासवर्ड भूल गए?'),
    () => {
      if (!email.input.value.trim()) {
        notice(alert, t('Enter your email address first so we can send a reset link.', 'रीसेट लिंक भेजने के लिए पहले अपना ईमेल पता लिखें।'), true);
        email.input.focus();
        return;
      }
      forgotBtn.disabled = true;
      void requestPasswordReset(email.input.value.trim())
        .then(() => notice(alert, t('Password recovery email requested via Supabase Auth.', 'सुपाबेस ऑथ के माध्यम से पासवर्ड रीसेट ईमेल भेजा गया।')))
        .catch(err => notice(alert, errorMessage(err), true))
        .finally(() => { forgotBtn.disabled = false; });
    },
    'quiet',
  );
  metaRow.append(el('span', 'microcopy', t('HttpOnly session + PostgreSQL RLS', 'HttpOnly सत्र + PostgreSQL RLS')), forgotBtn);

  const submit = el('button', 'button', t('Sign in to workspace  →', 'कार्यक्षेत्र में साइन इन करें  →'));
  submit.type = 'submit';

  // Shared Demo Accounts (inspired by TenderLens signin.html)
  const demoBox = el('div', 'demo-credential-card');
  demoBox.append(
    el('span', 'label', t('SHARED PILOT DEMO ACCOUNTS (PASSWORD: TheekHai@2026)', 'साझा पायलट डेमो खाते (पासवर्ड: TheekHai@2026)')),
    el('p', 'microcopy', t('Click any role below to fill credentials and test the full citizen → department → reviewer workflow:', 'नागरिक → विभाग → समीक्षक कार्यप्रणाली जाँचने के लिए किसी भी भूमिका पर क्लिक करें:')),
  );
  const demoGrid = el('div', 'demo-grid');
  const demoRoles: [string, string, string][] = [
    ['citizen@theekhai.in', t('Citizen (Ward 4)', 'नागरिक (वार्ड 4)'), 'CITIZEN'],
    ['pwd@theekhai.in', t('Public Works Staff', 'लोक निर्माण विभाग'), 'DEPARTMENT'],
    ['reviewer@theekhai.in', t('Independent Reviewer', 'स्वतंत्र समीक्षक'), 'REVIEWER'],
    ['admin@theekhai.in', t('Operations Admin', 'संचालन व्यवस्थापक'), 'ADMIN'],
  ];
  for (const [demoEmail, roleTitle, badge] of demoRoles) {
    const btn = el('button', 'demo-role-btn');
    btn.type = 'button';
    btn.append(el('strong', '', roleTitle), el('small', '', `${demoEmail} · ${badge}`));
    btn.addEventListener('click', () => {
      email.input.value = demoEmail;
      password.input.value = 'TheekHai@2026';
      notice(alert, t(`Filled ${roleTitle} credentials. Click Sign in to continue.`, `${roleTitle} विवरण भर दिया गया है। आगे बढ़ने के लिए साइन इन दबाएँ।`));
    });
    demoGrid.append(btn);
  }
  demoBox.append(demoGrid);

  const switchRow = el('p', 'microcopy');
  switchRow.append(
    document.createTextNode(t('New to TheekHai? ', 'ठीकहै पर नए हैं? ')),
    link(t('Create a citizen account  →', 'नया नागरिक खाता बनाएँ  →'), '/signup', 'text-link'),
  );

  form.append(email.wrapper, password.wrapper, metaRow, alert, submit, demoBox, switchRow);
  form.addEventListener('submit', e => {
    e.preventDefault();
    submit.disabled = true;
    void signIn(email.input.value, password.input.value)
      .then(user => {
        if (!user) throw new Error('Session could not be verified.');
        navigate('/workspace');
      })
      .catch(error => notice(alert, errorMessage(error), true))
      .finally(() => { submit.disabled = false; });
  });

  const aside = el('aside', 'panel auth-aside');
  aside.append(
    el('span', 'label', t('TENDERLENS-INSPIRED SECURITY ARCHITECTURE', 'टेंडरलेंस-प्रेरित सुरक्षा संरचना')),
    el('h2', '', t('Identity backed by\nSupabase PostgreSQL.', 'सुपाबेस PostgreSQL द्वारा\nसुरक्षित पहचान।')),
    el('p', '', t('Every account is authenticated against Supabase Auth and synced with Row-Level Security (RLS) protected tables.', 'प्रत्येक खाता सुपाबेस ऑथ द्वारा सत्यापित होता है और RLS-सुरक्षित तालिकाओं के साथ सिंक रहता है।')),
  );
  const pillars = el('div', 'trust-pillars');
  for (const [title, desc] of [
    [t('PostgreSQL Row-Level Security', 'PostgreSQL रो-लेवल सुरक्षा (RLS)'), t('Profiles and saved legal references enforce `(select auth.uid()) = user_id` at the database layer.', 'प्रोफ़ाइल और सहेजे गए कानूनी संदर्भ डेटाबेस स्तर पर `(select auth.uid()) = user_id` लागू करते हैं।')],
    [t('Separation of Duties', 'जिम्मेदारियों का पृथक्करण'), t('Department staff can request closure, but only an independent reviewer can approve or reopen a case.', 'विभागीय कर्मचारी बंद करने का अनुरोध कर सकते हैं, लेकिन केवल स्वतंत्र समीक्षक ही स्वीकृति दे सकता है।')],
    [t('HttpOnly Session + CSRF Protection', 'HttpOnly सत्र + CSRF सुरक्षा'), t('Browser sessions stay in strict HttpOnly cookies rather than exposing tokens to scripts.', 'ब्राउज़र सत्र सुरक्षित HttpOnly कुकीज़ में रहते हैं।')],
  ]) {
    const p = el('div', 'trust-pillar');
    p.append(el('strong', '', title), el('p', 'microcopy', desc));
    pillars.append(p);
  }
  aside.append(pillars);
  shell.append(form, aside);
  parent.append(shell);
}

export function signup(parent:HTMLElement) {
  heading(
    parent,
    t('CREATE YOUR CIVIC ACCOUNT', 'अपना नागरिक खाता बनाएँ'),
    t('Join your neighbourhood workspace.', 'अपने पड़ोस के कार्यक्षेत्र से जुड़ें।'),
    t('Create a verified citizen account backed by Supabase Auth and RLS-protected profile storage.', 'सुपाबेस ऑथ और RLS-सुरक्षित प्रोफ़ाइल के साथ अपना नागरिक खाता बनाएँ।'),
  );
  const shell = el('div', 'auth-shell');
  const form = el('form', 'panel login-form');
  const fullName = field(t('Full name', 'पूरा नाम'), 'fullName');
  fullName.input.placeholder = t('e.g. Priya Sharma', 'जैसे प्रिया शर्मा');
  fullName.input.minLength = 2;
  const email = field(t('Email address', 'ईमेल पता'), 'email', 'email');
  email.input.autocomplete = 'email';
  email.input.placeholder = 'priya@example.com';

  const ward = field(t('Neighbourhood / Ward', 'पड़ोस / वार्ड'), 'ward');
  ward.input.value = 'Ward 4';
  const organization = field(t('Resident welfare association / Organization (optional)', 'निवासी कल्याण संघ / संस्था (वैकल्पिक)'), 'organization');
  organization.input.required = false;
  organization.input.placeholder = t('Citizen Resident', 'नागरिक निवासी');

  const password = passwordField(t('Create password (min. 8 characters)', 'पासवर्ड बनाएँ (कम से कम 8 अक्षर)'), 'password', 'new-password');
  password.input.minLength = 8;

  // TenderLens-inspired 3-bar Password Strength Meter
  const strengthWrap = el('div', 'strength-meter');
  const bars = [el('span', 'strength-bar'), el('span', 'strength-bar'), el('span', 'strength-bar')];
  const strengthLabel = el('small', 'microcopy', t('Use at least 8 characters with letters and numbers.', 'अक्षरों और संख्याओं के साथ कम से कम 8 वर्णों का उपयोग करें।'));
  const barRow = el('div', 'strength-bars');
  barRow.append(...bars);
  strengthWrap.append(barRow, strengthLabel);

  password.input.addEventListener('input', () => {
    const val = password.input.value;
    let score = 0;
    if (val.length >= 8) score++;
    if (/[A-Z]/.test(val) && /[0-9]/.test(val)) score++;
    if (val.length >= 12 || /[^A-Za-z0-9]/.test(val)) score++;
    bars.forEach((b, idx) => {
      b.className = 'strength-bar' + (idx < score ? ` active-${score}` : '');
    });
    if (!val) strengthLabel.textContent = t('Use at least 8 characters.', 'कम से कम 8 अक्षरों का उपयोग करें।');
    else if (score === 1) strengthLabel.textContent = t('Strength: Weak', 'मजबूती: कमज़ोर');
    else if (score === 2) strengthLabel.textContent = t('Strength: Good', 'मजबूती: अच्छी');
    else strengthLabel.textContent = t('Strength: Strong', 'मजबूती: मजबूत');
  });

  const confirmPassword = passwordField(t('Confirm password', 'पासवर्ड की पुष्टि करें'), 'confirmPassword', 'new-password');
  const alert = el('div');
  const submit = el('button', 'button', t('Create account & continue  →', 'खाता बनाएँ और आगे बढ़ें  →'));
  submit.type = 'submit';

  const switchRow = el('p', 'microcopy');
  switchRow.append(
    document.createTextNode(t('Already have an account? ', 'पहले से खाता है? ')),
    link(t('Sign in instead  →', 'साइन इन करें  →'), '/login', 'text-link'),
  );

  form.append(fullName.wrapper, email.wrapper, ward.wrapper, organization.wrapper, password.wrapper, strengthWrap, confirmPassword.wrapper, alert, submit, switchRow);
  form.addEventListener('submit', e => {
    e.preventDefault();
    if (password.input.value.length < 8) {
      notice(alert, t('Password must be at least 8 characters long.', 'पासवर्ड कम से कम 8 अक्षरों का होना चाहिए।'), true);
      return;
    }
    if (password.input.value !== confirmPassword.input.value) {
      notice(alert, t('Passwords do not match.', 'पासवर्ड मेल नहीं खाते।'), true);
      return;
    }
    submit.disabled = true;
    void registerAccount({
      fullName: fullName.input.value.trim(),
      email: email.input.value.trim(),
      password: password.input.value,
      organization: organization.input.value.trim() || 'Citizen Resident',
      ward: ward.input.value.trim() || 'Ward 1',
      preferredLanguage: document.documentElement.lang || 'en',
    })
      .then(user => {
        if (!user) throw new Error('Account created, please sign in.');
        navigate('/account');
      })
      .catch(err => notice(alert, errorMessage(err), true))
      .finally(() => { submit.disabled = false; });
  });

  const aside = el('aside', 'panel auth-aside');
  aside.append(
    el('span', 'label', t('CITIZEN WORKSPACE BENEFITS', 'नागरिक कार्यक्षेत्र लाभ')),
    el('h2', '', t('Track every step\nfrom report to fix.', 'रिपोर्ट से समाधान तक\nहर कदम देखें।')),
    el('p', '', t('Your citizen account connects your neighbourhood reports, department SLA deadlines, closure challenges, and bookmarked legal references.', 'आपका नागरिक खाता आपकी रिपोर्ट, समय-सीमा, बंद होने पर सवाल और सहेजे गए कानूनी संदर्भों को जोड़ता है।')),
    link(t('Need to test staff or reviewer queues? Use shared demo accounts on Sign In →', 'स्टाफ या समीक्षक कतार जाँचनी है? साइन इन पृष्ठ पर डेमो खातों का उपयोग करें →'), '/login', 'text-link'),
  );
  shell.append(form, aside);
  parent.append(shell);
}

export async function account(parent:HTMLElement) {
  heading(
    parent,
    t('ACCOUNT & SUPABASE RLS PROFILE', 'खाता और सुपाबेस RLS प्रोफ़ाइल'),
    t('Your civic profile & saved references.', 'आपकी नागरिक प्रोफ़ाइल और सहेजे गए संदर्भ।'),
    t('Synced with Supabase PostgreSQL (`public.profiles` and `public.saved_sources`) under strict Row-Level Security.', 'सख्त रो-लेवल सुरक्षा (RLS) के अंतर्गत सुपाबेस PostgreSQL (`public.profiles` और `public.saved_sources`) के साथ सिंक किया गया।'),
  );
  if (authGate(parent)) return;

  const container = el('div', 'detail-grid');
  parent.append(container);
  notice(container, t('Loading your Supabase profile & saved sources…', 'आपकी सुपाबेस प्रोफ़ाइल और सहेजे गए स्रोत लोड हो रहे हैं…'));

  try {
    const [profile, savedSources] = await Promise.all([getProfile(), getSavedSources()]);
    container.replaceChildren();

    const leftCol = el('div');
    const profileCard = el('section', 'panel');
    const headerRow = el('div', 'account-header');
    const initials = (profile.fullName || profile.email || 'TH')
      .split(/[\s._@-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(s => s[0]?.toUpperCase() ?? '')
      .join('');
    const avatar = el('div', 'account-avatar', initials || 'TH');
    const meta = el('div');
    meta.append(
      el('span', 'status status-closed', t('Signed in', 'साइन इन')),
      el('h2', '', profile.fullName || t('Civic User', 'नागरिक उपयोगकर्ता')),
      el('p', 'microcopy', `${profile.email} · Role: ${profile.role}${profile.department ? ` (${profile.department})` : ''}`),
    );
    headerRow.append(avatar, meta);
    profileCard.append(headerRow);

    const form = el('form', 'report-form');
    const fullName = field(t('Full name', 'पूरा नाम'), 'fullName');
    fullName.input.value = profile.fullName;
    const organization = field(t('Organization / RWA', 'संस्था / निवासी संघ'), 'organization');
    organization.input.value = profile.organization || 'Citizen Resident';
    organization.input.required = false;
    const ward = field(t('Default neighbourhood / Ward', 'डिफ़ॉल्ट पड़ोस / वार्ड'), 'ward');
    ward.input.value = profile.ward || 'Ward 1';

    const langLabel = el('label', 'field');
    langLabel.append(el('span', 'field-label', t('Preferred language', 'पसंदीदा भाषा')));
    const langSelect = el('select');
    for (const [val, label] of [['en', 'English'], ['hi', 'हिन्दी (Hindi)']]) {
      const opt = el('option', '', label);
      opt.value = val;
      if (profile.preferredLanguage === val) opt.selected = true;
      langSelect.append(opt);
    }
    langLabel.append(langSelect);

    const alert = el('div');
    const saveBtn = el('button', 'button', t('Save profile to Supabase', 'सुपाबेस में प्रोफ़ाइल सहेजें'));
    saveBtn.type = 'submit';
    form.append(fullName.wrapper, organization.wrapper, ward.wrapper, langLabel, alert, saveBtn);

    form.addEventListener('submit', e => {
      e.preventDefault();
      saveBtn.disabled = true;
      void saveProfile({
        userId: profile.userId,
        email: profile.email,
        fullName: fullName.input.value.trim(),
        organization: organization.input.value.trim(),
        role: profile.role,
        department: profile.department,
        ward: ward.input.value.trim(),
        preferredLanguage: langSelect.value,
      })
        .then(() => {
          notice(alert, t('Profile synced to Supabase `public.profiles`.', 'प्रोफ़ाइल सुपाबेस `public.profiles` में सहेजी गई।'));
        })
        .catch(err => notice(alert, errorMessage(err), true))
        .finally(() => { saveBtn.disabled = false; });
    });

    profileCard.append(form);
    leftCol.append(profileCard);

    const rightCol = el('aside', 'panel');
    rightCol.append(
      el('p', 'eyebrow', t('BOOKMARKED CIVIC & LEGAL SOURCES', 'सहेजे गए नागरिक और कानूनी संदर्भ')),
      el('h2', '', t('Saved reference pack', 'सहेजे गए संदर्भ')),
      el('p', 'microcopy', t('Stored in Supabase `public.saved_sources` with per-user RLS policies.', 'प्रति-उपयोगकर्ता RLS नीतियों के साथ सुपाबेस `public.saved_sources` में संग्रहीत।')),
    );
    const listWrap = el('div', 'case-list');
    const drawBookmarks = (items: SavedSource[]) => {
      listWrap.replaceChildren();
      if (!items.length) {
        const empty = el('div', 'empty-state');
        empty.append(
          el('p', '', t('No bookmarked legal sources yet. Save passages from the Reference Library for quick access.', 'अभी कोई कानूनी संदर्भ सहेजा नहीं गया है। त्वरित पहुँच के लिए संदर्भ लाइब्रेरी से स्रोत सहेजें।')),
          link(t('Explore reference library  ↗', 'संदर्भ लाइब्रेरी देखें  ↗'), '/library', 'button secondary'),
        );
        listWrap.append(empty);
        return;
      }
      for (const item of items) {
        const row = el('div', 'task-row');
        const info = el('div');
        const a = link(item.title, item.sourceUrl, 'text-link');
        a.target = '_blank';
        a.rel = 'noopener noreferrer';
        info.append(el('span', 'label', item.jurisdiction), a, el('p', 'microcopy', item.snippet.slice(0, 180)));
        const removeBtn = button(
          t('Remove', 'हटाएँ'),
          () => {
            void removeSavedSource(item.id).then(() => { const idx = items.findIndex(x => x.id === item.id); if (idx !== -1) items.splice(idx, 1); drawBookmarks(items); });
          },
          'quiet',
        );
        row.append(info, removeBtn);
        listWrap.append(row);
      }
    };
    drawBookmarks(savedSources);
    rightCol.append(listWrap);

    container.append(leftCol, rightCol);
  } catch (error) {
    notice(container, errorMessage(error), true);
  }
}

export async function workspace(parent:HTMLElement) {
  heading(parent,t('YOUR WORKSPACE','आपका कार्यक्षेत्र'),t('Every report. A next step.','हर रिपोर्ट। अगला कदम।'),t('Follow ownership, progress and the decisions that matter.','जिम्मेदारी, प्रगति और महत्वपूर्ण फैसले देखें।'));if(authGate(parent))return;
  const container=el('div');parent.append(container);notice(container,t('Loading your cases…','आपके मामले लोड हो रहे हैं…'));
  try{const result=await api<{items:CaseRecord[],total:number}>('/complaints');container.replaceChildren();const stats=el('div','stats');for(const [label,value] of [[t('Visible cases','दिख रहे मामले'),result.total],[t('Awaiting review','समीक्षा बाकी'),result.items.filter(x=>x.status==='PENDING_VERIFICATION').length],[t('Overdue','समय-सीमा पार'),result.items.filter(x=>x.status!=='CLOSED'&&x.dueAt<Date.now()).length]]){const item=el('div');item.append(el('span','label',String(label)),el('strong','stat-value',String(value)));stats.append(item)}container.append(stats);const toolbar=el('div','list-toolbar'),search=el('input');search.type='search';search.placeholder=t('Search your cases','अपने मामले खोजें');search.setAttribute('aria-label',search.placeholder);toolbar.append(el('h2','',t('Case activity','मामलों की गतिविधि')),search);container.append(toolbar);const list=el('div','case-list');container.append(list);
  const draw=()=>{list.replaceChildren();const cases=result.items.filter(x=>(x.title+' '+x.department).toLowerCase().includes(search.value.toLowerCase()));if(!cases.length){const empty=el('div','empty-state');empty.append(el('h3','',t('A fresh start.','एक नई शुरुआत।')),el('p','',t('No matching cases yet. Submit a report to start following its progress.','अभी कोई संबंधित मामला नहीं। प्रगति देखने के लिए रिपोर्ट भेजें।')),link(t('Report an issue','समस्या दर्ज करें'),'/report','button'));list.append(empty)}for(const record of cases){const row=link('',`/cases/${record.id}`,'case-row');const content=el('div');content.append(el('span','label',record.category+' · '+record.id.slice(0,8)),el('h3','',record.title),el('p','',record.address));const meta=el('div','case-meta');meta.append(el('span',`status status-${record.status.toLowerCase()}`,statusText(record.status)),el('span','',record.department),el('small','',t('Due ','समय-सीमा ')+date(record.dueAt)));row.append(content,meta,el('span','row-arrow','↗'));list.append(row)}};search.addEventListener('input',draw);draw();if(result.total>50)container.append(el('p','microcopy',t('Showing the first 50 cases.','पहले 50 मामले दिखाए जा रहे हैं।')));
  }catch(error){notice(container,errorMessage(error),true);container.append(button(t('Retry','फिर कोशिश करें'),()=>void render(),'button secondary'))}
}

export async function detail(parent:HTMLElement,id:string) {
  if(authGate(parent))return;const content=el('div');parent.append(content);notice(content,t('Loading case…','मामला लोड हो रहा है…'));
  try{const data=await api<CaseDetail>(`/complaints/${encodeURIComponent(id)}`);content.replaceChildren();const record=data.case;content.append(link(t('← Back to workspace','← कार्यक्षेत्र में लौटें'),'/workspace','text-link'));heading(content,record.category+' / '+record.id.slice(0,8),record.title,record.address);const layout=el('div','detail-grid'),body=el('div'),summary=el('section','panel'),tasks=el('section','panel'),timeline=el('section','panel');summary.append(el('span',`status status-${record.status.toLowerCase()}`,statusText(record.status)),el('p','case-description',record.description),el('p','',t('Lead department: ','मुख्य विभाग: ')+record.department),el('p','',t('Due: ','समय-सीमा: ')+date(record.dueAt)),el('p','microcopy',t('Pilot service target, not a statutory deadline.','पायलट सेवा लक्ष्य, वैधानिक समय-सीमा नहीं।')));tasks.append(el('h2','',t('Coordinated work','समन्वित कार्य')));for(const task of data.tasks){const row=el('div','task-row');row.append(el('span',task.complete?'task-done':'task-open',task.complete?'✓':'○'),el('span','',task.title),el('small','',task.department));if(!task.complete&&record.status==='IN_PROGRESS'&&(identity?.role==='ADMIN'||identity?.department===task.department))row.append(button(t('Complete','पूरा करें'),()=>void act('COMPLETE_TASK',task.id),'quiet'));tasks.append(row)}timeline.append(el('h2','',t('The complete story','पूरा घटनाक्रम')));for(const event of [...data.events].reverse()){const item=el('div','timeline-event');item.append(el('span','label',event.action.replaceAll('_',' ')),el('p','',event.note),el('time','microcopy',date(event.at)));timeline.append(item)}body.append(summary,tasks,timeline);
  const actions=el('aside','panel action-panel');actions.append(el('p','eyebrow',t('NEXT ACTION','अगला कदम')),el('h2','',t('Move it forward.','आगे बढ़ाएँ।')));const reason=el('textarea');reason.rows=4;reason.placeholder=t('Explain your action or provide review notes.','अपना निर्णय या समीक्षा टिप्पणी लिखें।');reason.setAttribute('aria-label',reason.placeholder);const alert=el('div');actions.append(reason,alert);
  async function act(action:string,target=''){if(reason.value.trim().length<3){notice(alert,t('Add a short reason first.','पहले संक्षिप्त कारण लिखें।'),true);return}try{await api(`/complaints/${id}/actions`,{method:'POST',body:JSON.stringify({action,version:record.version,note:reason.value,target,department:department.value})});await render()}catch(error){notice(alert,errorMessage(error),true)}}
  const department=el('select');department.setAttribute('aria-label',t('Department for task','कार्य का विभाग'));for(const name of ['Public works','Water services','Sanitation','Electrical services','Civic triage'])department.append(el('option','',name));
  const staff=identity?.role==='ADMIN'||identity?.role==='DEPARTMENT';const reviewer=identity?.role==='ADMIN'||identity?.role==='REVIEWER';
  if(staff&&['ASSIGNED','REOPENED'].includes(record.status))actions.append(button(t('Start work','काम शुरू करें'),()=>void act('START')));
  if(staff&&['ASSIGNED','REOPENED','IN_PROGRESS'].includes(record.status)){actions.append(department,button(t('Add required task','जरूरी कार्य जोड़ें'),()=>void act('ADD_TASK'),'button secondary'))}
  if(staff&&record.status==='IN_PROGRESS')actions.append(button(t('Request closure review','बंद करने की समीक्षा माँगें'),()=>void act('REQUEST_CLOSURE')));
  if(reviewer&&record.status==='PENDING_VERIFICATION'&&record.closureBy!==identity?.id){actions.append(button(t('Approve closure','बंद करना स्वीकार करें'),()=>void act('APPROVE_CLOSURE')),button(t('Return for more work','आगे काम के लिए लौटाएँ'),()=>void act('REJECT_CLOSURE'),'button secondary'))}
  if(record.status==='CLOSED'&&record.citizenId===identity?.id)actions.append(button(t('Challenge this closure','बंद होने पर सवाल उठाएँ'),()=>void act('CHALLENGE'),'button secondary'));
  if(reviewer&&record.status==='CLOSED')actions.append(button(t('Uphold challenge & reopen','आपत्ति स्वीकार करें और फिर खोलें'),()=>void act('REOPEN'),'button secondary'));
  actions.append(el('p','microcopy',t('Every decision is recorded. Closure requires an independent reviewer.','हर फैसला दर्ज होता है। बंद करने के लिए स्वतंत्र समीक्षक आवश्यक है।')));layout.append(body,actions);content.append(layout);
  }catch(error){notice(content,errorMessage(error),true)}
}

export function library(parent:HTMLElement){
  heading(parent,t('THE REFERENCE LIBRARY','संदर्भ लाइब्रेरी'),t('Know your next step.','अपना अगला कदम जानें।'),t('Approved civic guidance powered by TinyFish Agent & Monitor, OpenRouter grounded synthesis, Tavily official-domain discovery, and local source backup fallback.','टाइनीफिश एजेंट और मॉनिटर, ओपनराउटर संश्लेषण, टैविली खोज तथा स्थानीय स्वीकृत स्रोत बैकअप के साथ नागरिक मार्गदर्शन।'));
  const provBanner=el('div','stats stats-4');
  parent.append(provBanner);
  const providerNote=el('p','microcopy',t('Configuration indicators only — they do not verify live provider health or database access policies.','ये केवल कॉन्फ़िगरेशन संकेतक हैं — लाइव सेवा स्थिति या डेटाबेस पहुँच नीतियों का सत्यापन नहीं।'));
  provBanner.after(providerNote);
  void api<{supabaseConfigured:boolean,tinyFishConfigured:boolean,tavilyConfigured:boolean,openRouterConfigured:boolean}>('/providers').then(p=>{
    provBanner.replaceChildren();
    for(const [name,ok] of [['Supabase Auth',p.supabaseConfigured],['TinyFish Agent & Monitor',p.tinyFishConfigured],['OpenRouter AI',p.openRouterConfigured],['Tavily Official Search',p.tavilyConfigured]]){
      const item=el('div');
      item.append(el('span','label',String(name)),el('span',ok?'status status-closed':'status status-assigned',ok?t('Configured','कॉन्फ़िगर है'):t('Not configured','कॉन्फ़िगर नहीं')));
      provBanner.append(item);
    }
  }).catch(()=>{provBanner.replaceChildren(el('div','microcopy',t('Provider configuration is unavailable.','प्रदाता कॉन्फ़िगरेशन उपलब्ध नहीं है।')))});
  const form=el('form','panel library-form'),query=field(t('What would you like to understand?','आप क्या समझना चाहते हैं?'),'question');query.input.placeholder=t('e.g. How does the Haryana Auto Appeal System (AAS) handle overdue services?','जैसे हरियाणा ऑटो अपील सिस्टम (AAS) समय-सीमा पार होने पर कैसे काम करता है?');query.input.minLength=3;query.input.maxLength=1000;const jurisdiction=el('select');jurisdiction.setAttribute('aria-label',t('Jurisdiction','क्षेत्राधिकार'));for(const [value,label] of [['CENTRAL',t('Central guidance','केंद्रीय मार्गदर्शन')],['HARYANA',t('Haryana + central guidance','हरियाणा और केंद्रीय मार्गदर्शन')]]){const option=el('option','',label);option.value=value;jurisdiction.append(option)}const search=el('button','button',t('Find guidance','मार्गदर्शन खोजें'));search.type='submit';const answer=el('div','legal-results');form.append(query.wrapper,jurisdiction,search,el('p','microcopy',t('Uses TinyFish Agent Mode on approved official sources, with Tavily official-domain fallback and deterministic local excerpt backup (SOURCES_ONLY).','स्वीकृत आधिकारिक स्रोतों पर टाइनीफिश एजेंट मोड का उपयोग करता है, टैविली फ़ॉलबैक और स्थानीय अंश बैकअप (SOURCES_ONLY) के साथ।')));parent.append(form,answer);
  function sources(items:LegalSource[]){for(const source of items){const card=el('article','panel source-card');const a=link(source.title,source.url,'text-link');a.target='_blank';a.rel='noopener noreferrer';const foot=el('div','source-card-foot');foot.append(el('small','microcopy',t('Retrieved ','प्राप्त ')+date(source.retrievedAt)));if(identity){const saveBtn=button(t('Save to account','खाते में सहेजें'),()=>{saveBtn.disabled=true;void saveSourceBookmark({title:source.title,sourceUrl:source.url,jurisdiction:source.jurisdiction,snippet:source.content.slice(0,400)}).then(()=>{saveBtn.textContent=t('Saved ✓','सहेजा गया ✓')}).catch(()=>{saveBtn.disabled=false})},'quiet');foot.append(saveBtn)}card.append(el('span','label',source.jurisdiction+' · '+source.language),a,el('p','',source.content.slice(0,700)),foot);answer.append(card)}}
  form.addEventListener('submit',event=>{event.preventDefault();search.disabled=true;notice(answer,t('Searching approved sources via TinyFish Agent / fallback…','टाइनीफिश एजेंट / फ़ॉलबैक के माध्यम से स्वीकृत स्रोत खोजे जा रहे हैं…'));void(async()=>{try{if(identity){const result=await api<LegalAnswer>('/legal/ask',{method:'POST',body:JSON.stringify({question:query.input.value,jurisdiction:jurisdiction.value})});const badge=el('span','label',`Mode: ${result.mode}`);answer.replaceChildren(badge,el('p','legal-answer',result.answer),el('p','microcopy',result.limitation));sources(result.citations)}else{const result=await api<LegalSource[]>(`/legal/sources?q=${encodeURIComponent(query.input.value)}&jurisdiction=${jurisdiction.value}`);answer.replaceChildren();if(!result.length)notice(answer,t('No approved matching sources are installed yet.','अभी कोई संबंधित स्वीकृत स्रोत स्थापित नहीं है।'));sources(result)}}catch(error){notice(answer,errorMessage(error),true)}finally{search.disabled=false}})()});
  if(identity?.role==='REVIEWER'||identity?.role==='ADMIN'){
    const ops=el('section','panel');ops.append(el('p','eyebrow',t('TINYFISH AGENT & MONITOR + TAVILY','टाइनीफिश एजेंट और मॉनिटर + टैविली')),el('h2','',t('Source discovery, monitors & review queue','स्रोत खोज, मॉनिटर और समीक्षा कतार')));
    const statusBox=el('div');
    const discForm=el('form','library-form'),discQuery=field(t('Tavily discovery query or TinyFish target goal','टैविली खोज प्रश्न या टाइनीफिश लक्ष्य'),'discQuery'),discUrl=field(t('Allowlisted official URL (optional for Tavily discovery)','आधिकारिक URL (टैविली खोज के लिए वैकल्पिक)'),'discUrl');discUrl.input.required=false;discUrl.input.placeholder='https://haryana-rtsc.gov.in/guidelines';
    const discBtn=el('button','button secondary',t('Run TinyFish Agent + Tavily Discovery','टाइनीफिश एजेंट + टैविली खोज चलाएँ'));discBtn.type='submit';
    discForm.append(discQuery.wrapper,discUrl.wrapper,discBtn);
    const monForm=el('form','library-form'),monName=field(t('Monitor name','मॉनिटर का नाम'),'monName'),monTarget=field(t('Official URL (fetch) or topic query (search)','आधिकारिक URL (fetch) या विषय (search)'),'monTarget');
    const monType=el('select');for(const [v,l] of [['fetch','TinyFish Page Monitor (fetch)'],['search','TinyFish Topic Monitor (search)']]){const o=el('option','',l);o.value=v;monType.append(o)}
    const monBtn=el('button','button secondary',t('Create TinyFish Monitor','टाइनीफिश मॉनिटर बनाएँ'));monBtn.type='submit';
    monForm.append(monName.wrapper,monType,monTarget.wrapper,monBtn);
    const monList=el('div','case-list'),pendingList=el('div','case-list');
    async function refreshOps(){
      try{
        const [monitors,pending]=await Promise.all([api<LegalMonitor[]>('/legal/monitors'),api<LegalSource[]>('/legal/review')]);
        monList.replaceChildren(el('h3','',t('Active TinyFish Monitors','सक्रिय टाइनीफिश मॉनिटर')));
        for(const m of monitors){
          const row=el('div','task-row');row.append(el('span','label',`${m.monitorType.toUpperCase()} · ${m.jurisdiction}`),el('span','',`${m.name} (${m.target})`),el('small','',m.lastStatus??m.status),button(t('Run check now','अभी जाँचें'),()=>{void api<MonitorRunResult>(`/legal/monitors/${m.id}/run`,{method:'POST'}).then(r=>{notice(statusBox,`Monitor run (${r.providerMode}): ${r.queuedSources.length} source(s) queued.`);return refreshOps()}).catch(e=>notice(statusBox,errorMessage(e),true))},'quiet'));
          monList.append(row);
        }
        pendingList.replaceChildren(el('h3','',t('Pending source review queue','स्रोत समीक्षा कतार')));
        for(const p of pending){
          const row=el('div','task-row');row.append(el('span','label',p.jurisdiction),el('span','',p.title),button(t('Approve','स्वीकार करें'),()=>{void api(`/legal/sources/${p.id}/review`,{method:'POST',body:JSON.stringify({approve:true})}).then(()=>refreshOps())},'quiet'),button(t('Reject','अस्वीकार करें'),()=>{void api(`/legal/sources/${p.id}/review`,{method:'POST',body:JSON.stringify({approve:false})}).then(()=>refreshOps())},'quiet'));
          pendingList.append(row);
        }
      }catch(e){notice(statusBox,errorMessage(e),true)}
    }
    discForm.addEventListener('submit',e=>{e.preventDefault();void api<LegalSource[]>('/legal/discover',{method:'POST',body:JSON.stringify({query:discQuery.input.value,url:discUrl.input.value,jurisdiction:jurisdiction.value,language:'en'})}).then(res=>{notice(statusBox,`Queued ${res.length} source(s) for review.`);return refreshOps()}).catch(err=>notice(statusBox,errorMessage(err),true))});
    monForm.addEventListener('submit',e=>{e.preventDefault();void api<LegalMonitor>('/legal/monitors',{method:'POST',body:JSON.stringify({name:monName.input.value,monitorType:monType.value,target:monTarget.input.value,jurisdiction:jurisdiction.value,language:'en',scheduleCron:'0 0 * * *'})}).then(()=>{notice(statusBox,'TinyFish monitor created.');return refreshOps()}).catch(err=>notice(statusBox,errorMessage(err),true))});
    ops.append(statusBox,discForm,monForm,monList,pendingList);parent.append(ops);void refreshOps();
  }
}

export function information(parent:HTMLElement,path:string){const pages:Record<string,[string,string]>={
  '/privacy':[t('Your information, handled with care.','आपकी जानकारी, सावधानी के साथ।'),t('This pilot sends reports to the common backend and limits access by account and department. User profiles and bookmarked sources are stored in Supabase PostgreSQL under Row-Level Security (`(select auth.uid()) = user_id`). Drafts are stored on this browser only when you choose Save draft. Location and microphone access are optional and requested when used. Legal reference queries use approved sources with TinyFish Agent Mode, Tavily official-domain fallback, and a local approved-source backup fallback (SOURCES_ONLY); private complaint details are excluded from source discovery.','यह पायलट रिपोर्ट साझा बैकएंड पर भेजता है और खाते तथा विभाग के अनुसार पहुँच सीमित करता है। उपयोगकर्ता प्रोफ़ाइल और सहेजे गए स्रोत सुपाबेस PostgreSQL में रो-लेवल सुरक्षा (RLS) के अंतर्गत सुरक्षित रहते हैं। ड्राफ्ट केवल आपके चुनने पर इस ब्राउज़र में सहेजा जाता है। स्थान और माइक्रोफोन वैकल्पिक हैं। कानूनी संदर्भ प्रश्न स्वीकृत स्रोतों के साथ टाइनीफिश एजेंट मोड, टैविली फ़ॉलबैक और स्थानीय स्वीकृत स्रोत बैकअप (SOURCES_ONLY) का उपयोग करते हैं।')],
  '/terms':[t('Pilot terms','पायलट शर्तें'),t('This is a development pilot, not an official government filing channel or emergency service. Demonstration service targets are not legal deadlines. Reference excerpts do not establish eligibility or guarantee a legal outcome. Use synthetic information during testing. Public-launch terms remain subject to operator review.','यह विकास पायलट है, आधिकारिक सरकारी आवेदन माध्यम या आपातकालीन सेवा नहीं। प्रदर्शन सेवा लक्ष्य कानूनी समय-सीमा नहीं हैं। संदर्भ अंश पात्रता या कानूनी परिणाम सुनिश्चित नहीं करते। परीक्षण में काल्पनिक जानकारी उपयोग करें। सार्वजनिक लॉन्च की शर्तों की संचालक समीक्षा बाकी है।')],
  '/help':[t('A little guidance.','थोड़ा मार्गदर्शन।'),t('Start with a clear description and an address. Location and recording are optional: deny permission and continue with text. You can revoke permissions in your browser settings. Save a draft explicitly before leaving. A report is submitted only when a case ID is returned. Staff request closure; an independent reviewer decides it.','स्पष्ट विवरण और पते से शुरू करें। स्थान और रिकॉर्डिंग वैकल्पिक हैं: अनुमति न देकर लिखित रूप में आगे बढ़ें। ब्राउज़र सेटिंग्स से अनुमति वापस ले सकते हैं। जाने से पहले ड्राफ्ट सहेजें। मामला ID मिलने पर ही रिपोर्ट जमा मानी जाती है। कर्मचारी बंद करने का अनुरोध करते हैं; स्वतंत्र समीक्षक फैसला करता है।')]
};const page=pages[path];heading(parent,page?t('THE SMALL PRINT, CLEARLY','स्पष्ट जानकारी'):'404',page?.[0]??t('This street leads elsewhere.','यह रास्ता कहीं और जाता है।'),page?.[1]??t('We could not find that page. Let’s get you back on track.','यह पृष्ठ नहीं मिला। वापस चलते हैं।'));parent.append(link(t('Back to overview','परिचय पर लौटें'),'/','button'));}
