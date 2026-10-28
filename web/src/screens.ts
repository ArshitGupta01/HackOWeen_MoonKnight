import {
  api,
  identity,
  signIn,
  registerAccount,
  verifyEmail,
  resendVerification,
  requestPasswordReset,
  getProfile,
  saveProfile,
  getSavedSources,
  saveSourceBookmark,
  removeSavedSource,
  triageComplaint, transcribeAudio, reverseGeocode, verifyCaseResolution, importLegalSource, draftComplaintEmail, sendComplaintEmail, getGmailStatus, requestLoginCode, verifyLoginCode, instantSignIn, getLeaderboard, getDummyUsers, LeaderboardResponse, LeaderboardEntry,
  CaseRecord,
  CaseDetail,
  LegalAnswer,
  LegalSource,
  LegalMonitor,
  MonitorRunResult,
  SavedSource,
} from './api';
import {el,button,link,field,notice,t,date,statusText,scrollToTop,refreshScroll} from './ui';
import {render} from './main';

function heading(parent:HTMLElement,eyebrow:string,title:string,subtitle:string) { const block=el('div','page-heading');block.append(el('p','eyebrow',eyebrow),el('h1','',title),el('p','subtitle',subtitle));parent.append(block); }
function navigate(path:string) {history.pushState({},'',path);void render();scrollToTop(true)}
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
  const actions=el('div','actions');actions.append(link(t('Report a neighbourhood issue  ↗','पड़ोस की समस्या दर्ज करें  ↗'),'/report','button'),link(t('Explore Ward 4 Map  ↗','वार्ड 4 मानचित्र देखें  ↗'),'/map','button secondary'),link(t('Follow your report  →','अपनी रिपोर्ट देखें  →'),'/workspace','text-link'));copy.append(actions,el('p','microcopy',t('A clear owner. A visible deadline. A resolution you can question.','स्पष्ट जिम्मेदारी। दिखाई देती समय-सीमा। समाधान पर सवाल करने का अधिकार।')));
  const art=el('div','city-art');art.setAttribute('aria-label',t('Illustrated neighbourhood with streets, buildings and a park','सड़कों, इमारतों और पार्क वाला चित्रित पड़ोस'));
  // Fixed decorative SVG contains no external data.
  art.innerHTML='<svg viewBox="0 0 520 440" role="img" aria-label="Neighbourhood illustration"><defs><pattern id="lines" width="8" height="8" patternUnits="userSpaceOnUse"><path d="M0 8 8 0" stroke="#c4d2b9" stroke-width="1"/></pattern></defs><rect width="520" height="440" fill="#e9e9dc"/><path d="M-20 145 540 320M100-30 270 470M470-30 80 470" stroke="#f8f6ed" stroke-width="37" fill="none"/><path d="M-20 145 540 320M100-30 270 470M470-30 80 470" stroke="#c9cbbb" stroke-width="1" stroke-dasharray="5 7" fill="none"/><path d="m287 35 115 45-90 113-95-32z" fill="url(#lines)"/><g fill="#c8d2b9" stroke="#aab89b"><circle cx="280" cy="90" r="21"/><circle cx="315" cy="108" r="27"/><circle cx="342" cy="69" r="17"/><circle cx="278" cy="143" r="18"/></g><g fill="#d1bba0" stroke="#ab977d" stroke-width="1.5"><path d="m24 45 66-19 32 80-66 22z"/><path d="m375 144 75 29-24 61-75-29z"/><path d="m54 254 69 28-28 67-69-28z"/><path d="m293 326 76-23 24 77-76 22z"/></g><g fill="#f6f0df" stroke="#b9a88d"><path d="m30 38 66-19 32 80-66 22z"/><path d="m381 137 75 29-24 61-75-29z"/><path d="m60 247 69 28-28 67-69-28z"/><path d="m299 319 76-23 24 77-76 22z"/></g><g fill="#b8c1b4"><rect x="188" y="232" width="35" height="18" rx="4" transform="rotate(18 188 232)"/><rect x="359" y="260" width="30" height="15" rx="4" transform="rotate(18 359 260)"/></g><g transform="translate(238 215)"><circle r="32" fill="#123e39" opacity=".1"/><circle r="21" fill="#123e39"/><path d="m-9 0 6 6 12-13" fill="none" stroke="#faf8ed" stroke-width="3" stroke-linecap="round"/></g><g transform="translate(398 163)"><circle r="17" fill="#bb762f"/><path d="M0-7v9m0 4v1" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/></g></svg>';
  const caption=el('div','map-caption');caption.append(el('span','dot'),el('span','',t('Every neighbourhood deserves care.','हर पड़ोस को देखभाल का हक है।')),link(t('OPEN INTERACTIVE WARD 4 MAP  ↗','इंटरैक्टिव वार्ड 4 मानचित्र खोलें  ↗'),'/map','caption-map-link'));art.append(caption);hero.append(copy,art);parent.append(hero);
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
  let coords:{latitude:number,longitude:number}|null=null;
  const geoSection=el('div','geo-section');
  const geoControls=el('div','geo-controls');
  const geoBadge=el('div','geo-badge hidden');
  const geoText=el('p','microcopy',t('Use GPS or click on the Ward Map below to auto-fill your street address and coordinates.','सड़क का पता और निर्देशांक स्वतः भरने के लिए जीपीएस का उपयोग करें या नीचे वार्ड मानचित्र पर क्लिक करें।'));
  const applyCoordinates=async(lat:number,lon:number,accuracyMeters?:number,prefilledAddr?:string)=>{
    coords={latitude:Number(lat.toFixed(6)),longitude:Number(lon.toFixed(6))};
    geoBadge.classList.remove('hidden');
    geoBadge.textContent=`📍 ${coords.latitude.toFixed(5)}° N, ${coords.longitude.toFixed(5)}° E${accuracyMeters?` · ±${Math.round(accuracyMeters)} m`:''}`;
    if(prefilledAddr){address.input.value=prefilledAddr;geoText.textContent=`${t('Location locked:','स्थान लॉक किया गया:')} ${prefilledAddr}`;return}
    geoText.textContent=t('Resolving street & ward address from coordinates…','निर्देशांकों से सड़क और वार्ड का पता खोजा जा रहा है…');
    try{
      const rev=await reverseGeocode(coords.latitude,coords.longitude,document.documentElement.lang||'en');
      if(rev.address){
        address.input.value=rev.address;
        geoBadge.textContent=`📍 ${coords.latitude.toFixed(5)}° N, ${coords.longitude.toFixed(5)}° E · ${rev.ward}`;
        geoText.textContent=`${t('Auto-filled from geotag:','जियोटैग से स्वतः भरा गया:')} ${rev.address}`;
      }
    }catch{
      if(!address.input.value.trim())address.input.value=`Pinned GPS (${coords.latitude.toFixed(4)}° N, ${coords.longitude.toFixed(4)}° E), Ward 4`;
      geoText.textContent=t('GPS coordinates attached. Review the address field above.','जीपीएस निर्देशांक जुड़ गए हैं। ऊपर पता जाँचें।');
    }
  };
  const mapPickerWrap=el('div','map-picker-wrap hidden');
  const mapPickerHeader=el('div','map-picker-header');
  mapPickerHeader.append(el('span','label',t('INTERACTIVE WARD 4 GEOTAG PICKER · CLICK ANYWHERE TO DROP PIN','इंटरैक्टिव वार्ड 4 जियोटैग पिकर · पिन लगाने के लिए कहीं भी क्लिक करें')));
  const mapSvgWrap=el('div','map-picker-canvas');
  mapSvgWrap.innerHTML=`<svg viewBox="0 0 600 320" class="ward-picker-svg" role="img" aria-label="Interactive Ward 4 Map Picker">
    <rect width="600" height="320" fill="#eef2e8"/>
    <rect x="35" y="25" width="180" height="115" rx="12" fill="#d4e6cd" stroke="#b5cfa8"/>
    <ellipse cx="95" cy="82" rx="45" ry="26" fill="#bfddf3"/>
    <rect x="380" y="28" width="175" height="95" rx="10" fill="#e5ede0" stroke="#cad6c3"/>
    <rect x="370" y="165" width="190" height="125" rx="10" fill="#ede8da" stroke="#d5ccb8"/>
    <rect x="40" y="185" width="170" height="110" rx="10" fill="#e2ebe0" stroke="#c6d4c3"/>
    <path d="M 0 150 L 600 150" stroke="#b8c5b2" stroke-width="26"/>
    <path d="M 0 150 L 600 150" stroke="#fbbf24" stroke-width="2" stroke-dasharray="10 8"/>
    <path d="M 290 0 L 290 320" stroke="#b8c5b2" stroke-width="26"/>
    <path d="M 290 0 L 290 320" stroke="#ffffff" stroke-width="2" stroke-dasharray="10 8"/>
    <circle cx="290" cy="150" r="24" fill="#b8c5b2"/><circle cx="290" cy="150" r="11" fill="#9fb996"/>
    <text x="52" y="48" font-size="11" font-weight="600" fill="#2b4c3f">Central Park (Ward 4)</text>
    <text x="395" y="55" font-size="11" font-weight="600" fill="#2b4c3f">Primary Health Center</text>
    <text x="388" y="192" font-size="11" font-weight="600" fill="#5c4b32">Sector 14 Main Market</text>
    <text x="55" y="212" font-size="11" font-weight="600" fill="#2b4c3f">Ward 4 Primary School</text>
    <text x="302" y="140" font-size="10" font-weight="700" fill="#123e39">Sector 14 Rotary</text>
    <g id="picker-pin-group" transform="translate(290, 150)">
      <circle r="16" fill="#123e39" opacity="0.22"/>
      <circle r="9" fill="#123e39" stroke="#ffffff" stroke-width="2.5"/>
    </g>
  </svg>`;
  const quickZones=el('div','geo-quick-zones');
  const zonePresets:[string,string,number,number][]=[
    [t('Ward 4 Primary School','वार्ड 4 प्राथमिक विद्यालय'),'Main Market Road near Ward 4 Primary School, Ward 4',28.4595,77.0266],
    [t('Central Park Gate','सेंट्रल पार्क गेट'),'5th Cross Road near Central Park East Gate, Ward 4',28.4612,77.0289],
    [t('Sector 14 Crossing','सेक्टर 14 चौराहा'),'12th Main Road Junction, Sector 14 Crossing, Ward 4',28.4680,77.0310],
    [t('Sector 14 Market','सेक्टर 14 बाज़ार'),'Sector 14 Main Market Lane, Ward 4',28.4645,77.0342],
  ];
  for(const [zLabel,zAddr,zLat,zLon] of zonePresets){
    const zBtn=button(`📍 ${zLabel}`,()=>{void applyCoordinates(zLat,zLon,8,zAddr)},'quiet geo-zone-pill');
    quickZones.append(zBtn);
  }
  mapSvgWrap.addEventListener('click',ev=>{
    const svg=mapSvgWrap.querySelector('svg')!;
    const rect=svg.getBoundingClientRect();
    const relX=Math.max(0,Math.min(1,(ev.clientX-rect.left)/rect.width));
    const relY=Math.max(0,Math.min(1,(ev.clientY-rect.top)/rect.height));
    const pinGroup=svg.querySelector('#picker-pin-group');
    if(pinGroup)pinGroup.setAttribute('transform',`translate(${Math.round(relX*600)}, ${Math.round(relY*320)})`);
    const lat=28.4720-relY*0.0180;
    const lon=77.0200+relX*0.0180;
    void applyCoordinates(lat,lon,5);
  });
  mapPickerWrap.append(mapPickerHeader,mapSvgWrap,quickZones);
  const toggleMapBtn=button(t('🗺️ Pick on Ward Map','🗺️ मानचित्र पर स्थान चुनें'),()=>{
    const hidden=mapPickerWrap.classList.toggle('hidden');
    toggleMapBtn.textContent=hidden?t('🗺️ Pick on Ward Map','🗺️ मानचित्र पर स्थान चुनें'):t('Hide Ward Map','मानचित्र छिपाएँ');
  },'button secondary');
  const geo=button(t('📍 Use my location','📍 मेरा स्थान लें'),()=>{if(!navigator.geolocation){mapPickerWrap.classList.remove('hidden');notice(alert,t('Browser GPS is unavailable. Pick a spot on the Ward Map below.','जीपीएस उपलब्ध नहीं है। नीचे वार्ड मानचित्र पर स्थान चुनें।'),true);return}geo.disabled=true;geo.textContent=t('Locating…','स्थान खोजा जा रहा है…');navigator.geolocation.getCurrentPosition(pos=>{geo.disabled=false;geo.textContent=t('📍 Use my location','📍 मेरा स्थान लें');void applyCoordinates(pos.coords.latitude,pos.coords.longitude,pos.coords.accuracy)},()=>{navigator.geolocation.getCurrentPosition(pos2=>{geo.disabled=false;geo.textContent=t('📍 Use my location','📍 मेरा स्थान लें');void applyCoordinates(pos2.coords.latitude,pos2.coords.longitude,pos2.coords.accuracy)},()=>{geo.disabled=false;geo.textContent=t('📍 Use my location','📍 मेरा स्थान लें');mapPickerWrap.classList.remove('hidden');toggleMapBtn.textContent=t('Hide Ward Map','मानचित्र छिपाएँ');notice(alert,t('Browser GPS timed out or was denied. Click anywhere on the interactive Ward Map below to pin your exact location!','जीपीएस अनुमति नहीं मिली या समय समाप्त हुआ। नीचे वार्ड मानचित्र पर क्लिक करके अपना स्थान चुनें!'))},{enableHighAccuracy:false,timeout:8000,maximumAge:300000})},{enableHighAccuracy:true,timeout:6000,maximumAge:60000})},'button secondary');
  geoControls.append(geo,toggleMapBtn,geoBadge);
  geoSection.append(geoControls,geoText,mapPickerWrap);
  const consent=el('label','consent'),check=el('input');check.type='checkbox';check.required=true;consent.append(check,el('span','',t('I agree to share these details with the teams handling this report.','मैं यह विवरण रिपोर्ट पर काम करने वाली टीमों के साथ साझा करने के लिए सहमत हूँ।')));
  let media:MediaRecorder|null=null,stream:MediaStream|null=null,speechRec:any=null,timerInterval:number|null=null,secondsRecorded=0,liveSpeechTranscript='';let chunks:BlobPart[]=[];let audio:Blob|null=null;
  const audioPanel=el('div','audio-panel'),audioTopRow=el('div','audio-top-row'),audioPlayerWrap=el('div','audio-player-wrap hidden'),audioStatus=el('p','microcopy',t('Speak in English or Hindi — voice note will be transcribed and structured automatically.','अंग्रेज़ी या हिन्दी में बोलें — आवाज़ को स्वचालित रूप से लिखित रूप में तैयार किया जाएगा।'));
  const runTranscription=async()=>{
    if(!audio&&!liveSpeechTranscript.trim())return;
    transcribe.disabled=true;
    audioStatus.textContent=t('Transcribing voice note…','आवाज़ को लिखित रूप दिया जा रहा है…');
    try{
      let b64='';
      let mime='audio/webm';
      if(audio&&audio.size>0){
        mime=audio.type||'audio/webm';
        const dataUrl=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=reject;reader.readAsDataURL(audio!)});
        b64=dataUrl.split(',')[1]||'';
      }
      const result=await transcribeAudio({audioBase64:b64,mimeType:mime,language:document.documentElement.lang||'en',transcriptHint:liveSpeechTranscript.trim()});
      if(result.text){
        description.value=result.text;
      }
      if(!title.input.value.trim()&&result.suggestedTitle){
        title.input.value=result.suggestedTitle;
      }
      if(result.suggestedCategory){
        category.value=result.suggestedCategory;
      }
      audioStatus.textContent=`✓ ${t('Voice note transcribed','आवाज़ का लिखित रूप तैयार')} .`;
      notice(alert,t('Voice note transcribed into the form! Review and edit before submitting.','आवाज़ का लिखित रूप फॉर्म में भर दिया गया है! भेजने से पहले जाँच लें।'));
    }catch(error){
      if(liveSpeechTranscript.trim()){
        description.value=liveSpeechTranscript.trim();
        audioStatus.textContent=t('Live speech transcript applied to description.','लाइव स्पीच प्रतिलिपि विवरण में जोड़ दी गई है।');
      }else{
        notice(alert,errorMessage(error),true);
      }
    }finally{
      transcribe.disabled=false;
    }
  };
  const record=button(t('🎙️ Record a voice note','🎙️ आवाज़ रिकॉर्ड करें'),()=>{void (async()=>{try{if(media?.state==='recording'){media.stop();try{speechRec?.stop()}catch{/* ignore */};return}liveSpeechTranscript='';secondsRecorded=0;const SpeechRecognitionCtor=(window as any).SpeechRecognition||(window as any).webkitSpeechRecognition;if(SpeechRecognitionCtor){try{speechRec=new SpeechRecognitionCtor();speechRec.lang=(document.documentElement.lang==='hi')?'hi-IN':'en-IN';speechRec.continuous=true;speechRec.interimResults=true;speechRec.onresult=(ev:any)=>{let combined='';for(let i=0;i<ev.results.length;i++){combined+=ev.results[i][0].transcript+' '}liveSpeechTranscript=combined.trim();if(liveSpeechTranscript){description.value=liveSpeechTranscript;audioStatus.textContent=`🎙️ "${liveSpeechTranscript}"`}};speechRec.start()}catch{/* SpeechRecognition optional */}}stream=await navigator.mediaDevices.getUserMedia({audio:true});media=new MediaRecorder(stream);chunks=[];media.ondataavailable=e=>{if(e.data.size)chunks.push(e.data)};media.onstop=()=>{if(timerInterval){clearInterval(timerInterval);timerInterval=null}try{speechRec?.stop()}catch{/* ignore */}audioPanel.classList.remove('recording');audio=new Blob(chunks,{type:media!.mimeType||'audio/webm'});stream?.getTracks().forEach(track=>track.stop());record.textContent=t('🎙️ Record again','🎙️ फिर रिकॉर्ड करें');transcribe.disabled=false;audioPlayerWrap.replaceChildren();const player=document.createElement('audio');player.controls=true;player.src=URL.createObjectURL(audio);player.className='audio-preview-player';audioPlayerWrap.append(player);audioPlayerWrap.classList.remove('hidden');void runTranscription()};media.start();audioPanel.classList.add('recording');record.textContent=t('⏹ Stop recording (00:00)','⏹ रिकॉर्डिंग रोकें (00:00)');timerInterval=window.setInterval(()=>{secondsRecorded++;const mm=String(Math.floor(secondsRecorded/60)).padStart(2,'0');const ss=String(secondsRecorded%60).padStart(2,'0');record.textContent=`${t('⏹ Stop recording','⏹ रिकॉर्डिंग रोकें')} (${mm}:${ss})`},1000);audioStatus.textContent=t('Listening… Speak clearly into your microphone. Click Stop when finished.','सुन रहा है… माइक्रोफोन में स्पष्ट बोलें। पूरा होने पर रोकें दबाएँ।');setTimeout(()=>{if(media?.state==='recording')media.stop()},60000)}catch{try{speechRec?.stop()}catch{/* ignore */}notice(alert,t('Microphone unavailable or permission denied. You can type your report or use browser speech dictation.','माइक्रोफोन उपलब्ध नहीं या अनुमति नहीं मिली। रिपोर्ट टाइप करें।'),true)}})()},'button secondary');
  const transcribe=button(t('✨ Transcribe & Format','✨ लिखित रूप तैयार करें'),()=>{void runTranscription()},'quiet');transcribe.disabled=true;audioTopRow.append(record,transcribe);audioPanel.append(audioTopRow,audioPlayerWrap,audioStatus);

  // Photo Evidence Capture & Sample Streetlight Option (Section 7 RTSA Compliance)
  let photoDataUrl:string|null=null;
  let photoFileName='';
  const photoSection=el('div','photo-evidence-section');
  const photoHeader=el('div');
  photoHeader.append(
    el('span','label',t('📸 PHOTOGRAPHIC EVIDENCE · RTSA STATUTORY COMPLIANCE','📸 फोटो साक्ष्य · आरटीएसए वैधानिक अनुपालन')),
    el('p','microcopy',t('Geotagged photos provide non-repudiable legal proof under Section 7 of the Haryana Right to Service Act.','जियोटैग की गई तस्वीरें सेवा का अधिकार अधिनियम की धारा 7 के तहत कानूनी सबूत प्रदान करती हैं।'))
  );
  const photoInput=document.createElement('input');
  photoInput.type='file';
  photoInput.accept='image/*';
  photoInput.capture='environment';
  photoInput.style.display='none';

  const photoPreviewWrap=el('div','photo-preview-wrap hidden');
  const previewImg=document.createElement('img');
  previewImg.alt='Geotagged visual evidence preview';
  const geotagOverlay=el('div','photo-geotag-overlay');
  const removePhotoBtn=button('✕ '+t('Remove Photo','फोटो हटाएं'),()=>{
    photoDataUrl=null;
    photoFileName='';
    photoPreviewWrap.classList.add('hidden');
    photoInput.value='';
  },'quiet');
  removePhotoBtn.style.color='#fca5a5';
  removePhotoBtn.style.padding='2px 8px';
  removePhotoBtn.style.fontSize='11px';

  photoPreviewWrap.append(previewImg,geotagOverlay);

  const updatePhotoPreview=(dataUrl:string,name:string)=>{
    photoDataUrl=dataUrl;
    photoFileName=name;
    previewImg.src=dataUrl;
    const nowTime=new Date().toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit'});
    const coordStr=coords?`${coords.latitude.toFixed(5)}° N, ${coords.longitude.toFixed(5)}° E`:'28.4595° N, 77.0266° E';
    geotagOverlay.replaceChildren();
    const infoSpan=el('span','',`📍 ${coordStr} · Ward 4 · ${nowTime} IST`);
    geotagOverlay.append(infoSpan,removePhotoBtn);
    photoPreviewWrap.classList.remove('hidden');
    refreshScroll();
  };

  photoInput.addEventListener('change',()=>{
    const file=photoInput.files?.[0];
    if(!file)return;
    const reader=new FileReader();
    reader.onload=()=>{
      const img=new Image();
      img.onload=()=>{
        const maxDim=1024;
        let w=img.width,h=img.height;
        if(w>maxDim||h>maxDim){
          if(w>h){h=Math.round((h*maxDim)/w);w=maxDim}
          else{w=Math.round((w*maxDim)/h);h=maxDim}
        }
        const canvas=document.createElement('canvas');
        canvas.width=w;canvas.height=h;
        const ctx=canvas.getContext('2d');
        ctx?.drawImage(img,0,0,w,h);
        const compressed=canvas.toDataURL('image/jpeg',0.82);
        updatePhotoPreview(compressed,file.name||'evidence.jpg');
        if(!coords)void applyCoordinates(28.4595,77.0266,5,'Main Market Road near Ward 4 Primary School, Ward 4');
      };
      img.src=String(reader.result);
    };
    reader.readAsDataURL(file);
  });

  const photoBtnRow=el('div','geo-controls');
  const capturePhotoBtn=button(t('📷 Capture / Attach Photo','📷 फोटो खींचें / जोड़ें'),()=>{photoInput.click()},'button secondary');
  const samplePhotoBtn=button(t('💡 Use Sample Broken Streetlight Photo','💡 टूटी स्ट्रीटलाइट का नमूना फोटो लें'),async()=>{
    try{
      samplePhotoBtn.disabled=true;
      samplePhotoBtn.textContent=t('Loading sample photo…','नमूना लोड हो रहा है…');
      const res=await fetch('/broken_streetlight.jpg');
      const blob=await res.blob();
      const reader=new FileReader();
      reader.onload=()=>{
        updatePhotoPreview(String(reader.result),'Broken_Streetlight_Ward4.jpg');
        if(!title.input.value.trim())title.input.value=t('Broken Streetlight & Exposed Wiring near School Gate','स्कूल गेट के पास टूटी स्ट्रीटलाइट और खुला तार');
        category.value='LIGHTING';
        if(!coords)void applyCoordinates(28.4595,77.0266,5,'Main Market Road near Ward 4 Primary School, Ward 4');
        samplePhotoBtn.disabled=false;
        samplePhotoBtn.textContent=t('💡 Use Sample Broken Streetlight Photo','💡 टूटी स्ट्रीटलाइट का नमूना फोटो लें');
      };
      reader.readAsDataURL(blob);
    }catch{
      samplePhotoBtn.disabled=false;
      samplePhotoBtn.textContent=t('💡 Use Sample Broken Streetlight Photo','💡 टूटी स्ट्रीटलाइट का नमूना फोटो लें');
    }
  },'quiet geo-zone-pill');

  photoBtnRow.append(capturePhotoBtn,samplePhotoBtn);
  photoSection.append(photoHeader,photoBtnRow,photoPreviewWrap,photoInput);

  const resolveOfficialAuthority=(cat:string)=>{
    switch(cat){
      case 'LIGHTING':
        return{
          department:t('Dakshin Haryana Bijli Vitran Nigam (DHBVN) & Municipal Electrical Wing','दक्षिण हरियाणा बिजली वितरण निगम (DHBVN) एवं नगरपालिका विद्युत शाखा'),
          designation:t('Superintending Engineer (Operations & Streetlighting)','अधीक्षण अभियंता (परिचालन एवं स्ट्रीटलाइट)'),
          email:'se.electrical.dhbvn@haryana.gov.in',
          slaHours:48,
        };
      case 'ROADS':
        return{
          department:t('Public Works Department (PWD B&R Division Haryana)','लोक निर्माण विभाग (पीडब्ल्यूडी बी एंड आर हरियाणा)'),
          designation:t('Executive Engineer (Infrastructure & Roads)','कार्यकारी अभियंता (सड़क एवं ढांचा)'),
          email:'ee.pwd.ggn@haryana.gov.in',
          slaHours:72,
        };
      case 'WATER':
        return{
          department:t('Municipal Corporation Water Supply & Sewerage Disposal Wing','नगर निगम जलापूर्ति एवं सीवरेज शाखा'),
          designation:t('Executive Engineer (Public Health & Sewerage)','कार्यकारी अभियंता (जनस्वास्थ्य एवं सीवरेज)'),
          email:'xen.water.mcg@haryana.gov.in',
          slaHours:48,
        };
      case 'WASTE':
        return{
          department:t('Municipal Corporation Solid Waste Management & Sanitation Wing','नगर निगम ठोस अपशिष्ट प्रबंधन एवं स्वच्छता शाखा'),
          designation:t('Joint Commissioner (Swachh Bharat Cell)','संयुक्त आयुक्त (स्वच्छ भारत सेल)'),
          email:'commissioner@mcg.gov.in',
          slaHours:24,
        };
      default:
        return{
          department:t('Municipal Corporation Public Grievance Redressal Cell','नगर निगम लोक शिकायत निवारण प्रकोष्ठ'),
          designation:t('Nodal Public Grievance Officer','नोडल लोक शिकायत अधिकारी'),
          email:'commissioner@mcg.gov.in',
          slaHours:72,
        };
    }
  };

  const submit=el('button','button',t('Review & submit report  ↗','जाँचें और रिपोर्ट भेजें  ↗'));submit.type='submit';
  const draft=button(t('Save draft on this device','इस डिवाइस पर ड्राफ्ट सहेजें'),()=>{try{localStorage.setItem('theekhai-draft',JSON.stringify({title:title.input.value,description:description.value,address:address.input.value,category:category.value}));notice(alert,t('Draft saved on this device. It has not been submitted.','ड्राफ्ट इस डिवाइस पर सहेजा गया। अभी भेजा नहीं गया है。'))}catch{notice(alert,t('This browser could not save the draft. Copy your text before leaving.','यह ब्राउज़र ड्राफ़्ट नहीं सहेज सका। जाने से पहले अपना पाठ कॉपी करें।'),true)}},'quiet');
  try{const saved=JSON.parse(localStorage.getItem('theekhai-draft')??'null');if(saved){title.input.value=saved.title??'';description.value=saved.description??'';address.input.value=saved.address??'';category.value=saved.category??'ROADS'}}catch{try{localStorage.removeItem('theekhai-draft')}catch{/* Storage may be disabled. */}}
  const triageBox=el('div');
  let suggestedPriority = 'MEDIUM';
  const aiTriageBtn=button(t('Check Duplicates & Categorize','डुप्लीकेट जाँचें और श्रेणी चुनें'),()=>{
    if(description.value.trim().length<5){notice(alert,t('Describe the issue first (at least 5 characters) to check for duplicate reports.','डुप्लीकेट जाँचने के लिए पहले समस्या का विवरण लिखें।'),true);description.focus();return}
    aiTriageBtn.disabled=true;
    notice(triageBox,t('Checking neighbourhood reports for duplicates…','पड़ोस के मामलों में डुप्लीकेट की जाँच की जा रही है…'));
    void triageComplaint({title:title.input.value,description:description.value,address:address.input.value,language:document.documentElement.lang||'en'})
      .then(res=>{
        if(!title.input.value.trim()&&res.suggestedTitle)title.input.value=res.suggestedTitle;
        if(res.suggestedCategory)category.value=res.suggestedCategory;
        if(res.suggestedPriority)suggestedPriority=res.suggestedPriority;
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
  const params=new URLSearchParams(location.search);
  const qLat=parseFloat(params.get('lat')||'');const qLon=parseFloat(params.get('lng')||'');const qAddr=params.get('address')||'';const qCat=params.get('category')||'';
  if(qCat&&['ROADS','WATER','WASTE','LIGHTING','OTHER'].includes(qCat))category.value=qCat;
  if(Number.isFinite(qLat)&&Number.isFinite(qLon))void applyCoordinates(qLat,qLon,5,qAddr||undefined);
  
  const draftShowcase=el('div','email-draft-showcase-card hidden');
  draftShowcase.id='email-draft-card';

  const actions=el('div','actions');actions.append(submit,aiTriageBtn,draft);
  form.append(title.wrapper,categoryLabel,descriptionLabel,audioPanel,photoSection,address.wrapper,geoSection,triageBox,consent,alert,actions,draftShowcase);

  const renderDraftShowcase=(caseId:string|null)=>{
    const auth=resolveOfficialAuthority(category.value);
    const dateFormatted=new Date().toLocaleDateString(document.documentElement.lang==='hi'?'hi-IN':'en-IN',{day:'numeric',month:'short',year:'numeric'});
    const coordStr=coords?`${coords.latitude.toFixed(5)}° N, ${coords.longitude.toFixed(5)}° E (Ward 4 Murthal)`:'Ward 4 Murthal / Sector 14 Crossing';
    const caseRef=caseId?` [Case #${caseId.slice(-6)}]`:'';
    const subject=`[Civic Grievance RTSA-2026]${caseRef} ${title.input.value.trim()} - Ward 4`;

    const petitionBody=`To:
${auth.designation}
${auth.department}
Municipal Corporation / Government of Haryana
Official Grievance Email: ${auth.email}

Date: ${dateFormatted}
Subject: ${subject}

Respected Sir/Madam,

I am submitting this formal statutory civic grievance under the Haryana Right to Service Act (RTSA), 2026, for time-bound municipal intervention in Ward 4.

1. Issue: ${title.input.value.trim()}
2. Category: ${category.value}
3. Location / Landmark: ${address.input.value.trim()||'Ward 4, Murthal'}
4. Geotagged GPS Coordinates: ${coordStr}
5. Prescribed Statutory SLA: ${auth.slaHours} Hours (RTSA Citizen Charter)

6. Detailed Grievance Description:
${description.value.trim()}

7. Photographic Evidence:
${photoDataUrl?'Geotagged photographic visual evidence captured on-site and attached with this email dispatch.':'On-site photographic inspection requested.'}

Under Section 7 of the Haryana Right to Service Act 2026, designated municipal officers are bound to resolve this civic defect within ${auth.slaHours} hours or furnish written justification. Kindly register this petition, dispatch the field team, and issue an official acknowledgment receipt.

Yours sincerely,
Resident / Complainant of Ward 4
Dispatched via: TheekHai Municipal Civic Portal
Platform: https://theekhai.in`;

    draftShowcase.replaceChildren();

    const header=el('div');
    header.append(
      el('span','label',t('🏛️ OFFICIAL CIVIC GRIEVANCE PETITION · DISPATCH READY','🏛️ आधिकारिक नागरिक शिकायत याचिका · प्रेषण तैयार')),
      el('h3','',t('Haryana Right to Service Act (RTSA 2026) Official Draft','हरियाणा सेवा का अधिकार अधिनियम (RTSA 2026) आधिकारिक ड्राफ्ट')),
      el('p','microcopy',t('The official grievance letter has been prepared for dispatch to the accountable municipal department.','जवाबदेह नगरपालिका विभाग को भेजने के लिए आधिकारिक शिकायत पत्र तैयार कर लिया गया है।'))
    );

    const authBox=el('div','authority-info-box');
    const authTop=el('div');
    authTop.style.display='flex';
    authTop.style.justifyContent='space-between';
    authTop.style.alignItems='flex-start';
    authTop.style.flexWrap='wrap';
    authTop.style.gap='8px';

    const authDetails=el('div');
    const authRole=el('div','',auth.designation);
    authRole.style.fontWeight='700';
    authRole.style.fontSize='14px';
    const authDept=el('div','microcopy',auth.department);
    authDetails.append(authRole,authDept);

    const slaBadge=el('span','geo-badge',`⏱️ ${auth.slaHours}h ${t('Statutory SLA','वैधानिक समय-सीमा')}`);
    slaBadge.style.background='#dcfce7';
    slaBadge.style.color='#15803d';
    authTop.append(authDetails,slaBadge);

    const emailRow=el('div','authority-email-row');
    const emailText=el('span','',`To: ${auth.email}`);
    emailText.style.fontWeight='600';
    const copyEmailBtn=button(t('📋 Copy Email','📋 ईमेल कॉपी करें'),()=>{
      navigator.clipboard.writeText(auth.email);
      copyEmailBtn.textContent=t('✓ Copied!','✓ कॉपी हुआ!');
      setTimeout(()=>{copyEmailBtn.textContent=t('📋 Copy Email','📋 ईमेल कॉपी करें')},2000);
    },'quiet');
    copyEmailBtn.style.fontSize='11px';
    copyEmailBtn.style.padding='2px 8px';
    emailRow.append(emailText,copyEmailBtn);

    const subjRow=el('div');
    subjRow.style.marginTop='8px';
    subjRow.style.fontSize='13px';
    subjRow.innerHTML=`<strong>${t('Subject:','विषय:')}</strong> ${subject}`;

    authBox.append(authTop,emailRow,subjRow);

    const instructionBox=el('div','authority-instruction-box');
    instructionBox.innerHTML=`<strong>⚠️ ${t('Important Citizen Step:','महत्वपूर्ण नागरिक निर्देश:')}</strong><br/>
      ${t('Click "Open in Gmail App" to launch your email client with the authority email, subject, and petition body pre-filled. Attach your captured geotagged photo before sending so municipal authorities cannot reject the complaint under Section 7 of RTSA.','ईमेल क्लाइंट खोलने के लिए "जीमेल ऐप में खोलें" पर क्लिक करें। भेजने से पहले अपनी खींची गई जियोटैग फोटो अवश्य जोड़ें ताकि अधिकारी आरटीएसए की धारा 7 के तहत इसे खारिज न कर सकें।')}`;

    const preLabel=el('label','field-label',t('LEGAL PETITION LETTER PREVIEW','कानूनी याचिका पत्र पूर्वावलोकन'));
    preLabel.style.fontSize='11px';
    preLabel.style.fontWeight='700';
    preLabel.style.color='var(--muted)';
    const pre=el('pre','draft-preview-pre',petitionBody);

    const draftActions=el('div','actions');
    draftActions.style.marginTop='16px';

    const gmailBtn=button(t('📧 Open in Gmail App','📧 जीमेल ऐप में खोलें'),()=>{
      const mailto=`mailto:${encodeURIComponent(auth.email)}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(petitionBody)}`;
      window.open(mailto,'_blank');
    },'button gmail-dispatch-btn');

    const downloadBtn=button(t('📥 Download Draft (.txt)','📥 ड्राफ्ट डाउनलोड करें (.txt)'),()=>{
      const blob=new Blob([petitionBody],{type:'text/plain;charset=utf-8'});
      const url=URL.createObjectURL(blob);
      const a=document.createElement('a');
      a.href=url;
      a.download=`Civic_Grievance_Ward4_${caseId||Date.now()}.txt`;
      a.click();
      URL.revokeObjectURL(url);
    },'button secondary');

    const copyLetterBtn=button(t('📋 Copy Petition Letter','📋 याचिका पत्र कॉपी करें'),()=>{
      navigator.clipboard.writeText(petitionBody);
      copyLetterBtn.textContent=t('✓ Copied to clipboard!','✓ क्लिपबोर्ड पर कॉपी हुआ!');
      setTimeout(()=>{copyLetterBtn.textContent=t('📋 Copy Petition Letter','📋 याचिका पत्र कॉपी करें')},2500);
    },'quiet');

    draftActions.append(gmailBtn,downloadBtn,copyLetterBtn);
    if(caseId){
      const viewCaseBtn=link(t(`👉 View Case #${caseId.slice(-6)} File & Tasks →`,`👉 मामला #${caseId.slice(-6)} देखें →`),`/cases/${caseId}`,'button secondary');
      draftActions.append(viewCaseBtn);
    }

    draftShowcase.append(header,authBox,instructionBox,preLabel,pre,draftActions);
    draftShowcase.classList.remove('hidden');
    refreshScroll();
    draftShowcase.scrollIntoView({behavior:'smooth',block:'start'});
  };

  const submissionKey=crypto.randomUUID();
  form.addEventListener('submit',event=>{
    event.preventDefault();
    if(title.input.value.trim().length<5){
      notice(alert,t('Please enter a short title (at least 5 characters).','कृपया कम से कम 5 अक्षरों का शीर्षक लिखें।'),true);
      title.input.focus();
      return;
    }
    if(description.value.trim().length<10){
      notice(alert,t('Please describe the issue (at least 10 characters).','कृपया समस्या का विवरण लिखें (कम से कम 10 अक्षर)।'),true);
      description.focus();
      return;
    }
    if(!check.checked){
      notice(alert,t('Please check the consent box to proceed.','आगे बढ़ने के लिए सहमति बॉक्स चुनें।'),true);
      check.focus();
      return;
    }

    void(async()=>{
      submit.disabled=true;
      submit.textContent=t('Preparing mail draft…','मेल ड्राफ्ट तैयार हो रहा है…');
      try{
        if(identity){
          const record=await api<CaseRecord>('/complaints',{
            method:'POST',
            headers:{'Idempotency-Key':submissionKey},
            body:JSON.stringify({
              title:title.input.value,
              description:description.value,
              category:category.value,
              address:address.input.value,
              latitude:coords?.latitude??null,
              longitude:coords?.longitude??null,
              consent:check.checked,
              priority:suggestedPriority,
              photoUrl:photoDataUrl
            })
          });
          localStorage.removeItem('theekhai-draft');
          notice(alert,t(`✓ Report registered as Case #${record.id.slice(-6)}! Official grievance petition & Gmail dispatch draft is ready on screen below.`,'✓ रिपोर्ट दर्ज हो गई! आधिकारिक याचिका और जीमेल ड्राफ्ट नीचे स्क्रीन पर तैयार है。'));
          renderDraftShowcase(record.id);
        }else{
          // Render immediate guest grievance mail draft on screen
          renderDraftShowcase(null);
          notice(alert,t('✓ Official authority grievance draft generated on screen below! You can send it directly via Gmail or download it now. (Sign in to also track live tasks).','✓ आधिकारिक शिकायत ड्राफ्ट नीचे स्क्रीन पर तैयार है! आप इसे सीधे जीमेल से भेज सकते हैं या डाउनलोड कर सकते हैं।'));
        }
      }catch(error){
        // Even if network or API errors, generate the mail draft directly on screen so citizen is never blocked
        renderDraftShowcase(null);
        notice(alert,`${errorMessage(error)} — ${t('Offline statutory draft rendered below for immediate Gmail dispatch.','तत्काल जीमेल प्रेषण के लिए ऑफलाइन वैधानिक ड्राफ्ट नीचे प्रस्तुत है।')}`);
      }finally{
        submit.disabled=false;
        submit.textContent=t('Review & submit report  ↗','जाँचें और रिपोर्ट भेजें  ↗');
      }
    })();
  });

  aside.append(el('p','eyebrow',t('WHAT HAPPENS NEXT','इसके बाद क्या होगा')),el('h2','',t('A report with\na way forward.','एक रिपोर्ट,\nआगे का रास्ता।')),el('p','',t('Your report goes to an accountable department. You can follow tasks, due dates and the final review from your workspace.','आपकी रिपोर्ट जिम्मेदार विभाग तक जाती है। कार्यक्षेत्र में काम, समय-सीमा और अंतिम समीक्षा देखें।')),el('div','aside-note',t('This is a civic pilot. It is not an emergency service.','यह नागरिक पायलट है। यह आपातकालीन सेवा नहीं है।')),link(t('How we use your information','आपकी जानकारी का उपयोग'),'/privacy','text-link'));
  layout.append(form,aside);parent.append(layout);return()=>{if(media?.state==='recording')media.stop();stream?.getTracks().forEach(track=>track.stop())};

}

export function login(parent:HTMLElement) {
  heading(
    parent,
    t('CIVIC ACCOUNT ACCESS', 'नागरिक खाता पहुँच'),
    t('Keep the conversation going.', 'बात आगे बढ़ाएँ।'),
    t('Sign in directly with your email and password, or choose a role-based civic account.', 'अपने ईमेल और पासवर्ड से सीधे साइन इन करें, या भूमिका-आधारित नागरिक खाता चुनें।'),
  );
  const shell = el('div', 'auth-shell');
  const form = el('form', 'panel login-form');
  const alert = el('div');

  const email = field(t('Email address', 'ईमेल पता'), 'email', 'email');
  email.input.autocomplete = 'email';
  email.input.placeholder = 'citizen@theekhai.in';

  const password = passwordField(t('Password', 'पासवर्ड'), 'password', 'current-password');
  if (email.input.value.includes('@theekhai.in') && !password.input.value) {
    password.input.value = 'TheekHai@2026';
  }

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
  metaRow.append(el('span', 'microcopy', t('Direct Sign-In · HttpOnly Session', 'सीधा साइन-इन · HttpOnly सत्र')), forgotBtn);

  const submit = el('button', 'button', t('Sign in to workspace  →', 'कार्यक्षेत्र में साइन इन करें  →'));
  submit.type = 'submit';

  // Shared Demo Accounts (1-Click Instant Sign-In)
  const demoBox = el('div', 'demo-credential-card');
  demoBox.append(
    el('span', 'label', t('ROLE-BASED CIVIC ACCESS · 1-CLICK INSTANT SIGN-IN', 'भूमिका-आधारित नागरिक पहुँच · 1-क्लिक त्वरित साइन-इन')),
    el('p', 'microcopy', t('Click any account below to sign in instantly without entering password:', 'बिना पासवर्ड दर्ज किए तुरंत साइन इन करने के लिए नीचे किसी भी खाते पर क्लिक करें:')),
  );
  const demoGrid = el('div', 'demo-grid demo-grid-10');
  const dummyUsers: [string, string, string, string, string][] = [
    ['mohit@theekhai.in', 'Mohit Sharma', t('Citizen (Ward 4)', 'नागरिक (वार्ड 4)'), 'CITIZEN', '395 pts'],
    ['arshit@theekhai.in', 'Arshit Gupta', t('Municipal Admin', 'नगरपालिका व्यवस्थापक'), 'ADMIN', '485 pts'],
    ['atri@theekhai.in', 'Atri Chatterjee', t('Civic Reviewer', 'स्वतंत्र समीक्षक'), 'REVIEWER', '430 pts'],
    ['himanshu@theekhai.in', 'Himanshu Verma', t('PWD Division', 'लोक निर्माण विभाग'), 'DEPARTMENT', '360 pts'],
    ['priya@theekhai.in', 'Priya Nair', t('Citizen (Ward 4)', 'नागरिक (वार्ड 4)'), 'CITIZEN', '290 pts'],
    ['rohan@theekhai.in', 'Rohan Deshmukh', t('Electrical Ops', 'विद्युत सेवाएं'), 'DEPARTMENT', '310 pts'],
    ['ananya@theekhai.in', 'Ananya Iyer', t('Citizen (Ward 2)', 'नागरिक (वार्ड 2)'), 'CITIZEN', '235 pts'],
    ['vikram@theekhai.in', 'Vikramaditya Singh', t('Water Services', 'जल आपूर्ति विभाग'), 'DEPARTMENT', '265 pts'],
    ['neha@theekhai.in', 'Neha Kapoor', t('Citizen (Ward 4)', 'नागरिक (वार्ड 4)'), 'CITIZEN', '185 pts'],
    ['kabir@theekhai.in', 'Kabir Mehta', t('Citizen (Ward 3)', 'नागरिक (वार्ड 3)'), 'CITIZEN', '135 pts'],
  ];
  for (const [demoEmail, name, roleTitle, badge, pts] of dummyUsers) {
    const btn = el('button', 'demo-role-btn');
    btn.type = 'button';
    btn.append(
      el('strong', '', `${name} (${pts})`),
      el('small', '', `${roleTitle} · ${badge}`),
    );
    btn.addEventListener('click', async () => {
      btn.disabled = true;
      notice(alert, t(`Signing in as ${name}…`, `${name} के रूप में साइन इन किया जा रहा है…`));
      try {
        const user = await instantSignIn(demoEmail);
        if (!user) throw new Error('Could not establish session.');
        notice(alert, t('✓ Signed in successfully! Loading workspace…', '✓ सफलतापूर्वक साइन इन हुआ! कार्यक्षेत्र लोड हो रहा है…'));
        navigate('/workspace');
      } catch (err) {
        notice(alert, errorMessage(err), true);
        btn.disabled = false;
      }
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

  form.onsubmit = async e => {
    e.preventDefault();
    const em = email.input.value.trim();
    if (!em) {
      notice(alert, t('Please enter your email address.', 'कृपया अपना ईमेल पता लिखें।'), true);
      return;
    }
    submit.disabled = true;
    notice(alert, t('Signing in…', 'साइन इन किया जा रहा है…'));
    try {
      const user = await signIn(em, password.input.value);
      if (!user) throw new Error('Session could not be established.');
      notice(alert, t('✓ Signed in successfully! Loading workspace…', '✓ सफलतापूर्वक साइन इन हुआ! कार्यक्षेत्र लोड हो रहा है…'));
      navigate('/workspace');
    } catch (error) {
      notice(alert, errorMessage(error), true);
      submit.disabled = false;
    }
  };

  const aside = el('aside', 'panel auth-aside');
  aside.append(
    el('span', 'label', t('CIVIC AUTHENTICATION ARCHITECTURE', 'नागरिक प्रमाणीकरण संरचना')),
    el('h2', '', t('Direct Access & Verified Identity\nBacked by Supabase RLS.', 'सीधी पहुँच और सत्यापित पहचान\nसुपाबेस RLS द्वारा सुरक्षित।')),
    el('p', '', t('Direct sign-in for registered accounts. New citizen registrations verify email validity with an activation link dispatched from arshitgupta34@gmail.com.', 'पंजीकृत खातों के लिए सीधा साइन-इन। नए पंजीकरण ईमेल की वैधता की पुष्टि के लिए आधिकारिक लिंक का उपयोग करते हैं।')),
  );
  const pillars = el('div', 'trust-pillars');
  for (const [title, desc] of [
    [t('Sign-Up Email Verification Link', 'साइन-अप ईमेल सत्यापन लिंक'), t('All new registrations verify email validity with an activation link dispatched via Gmail API (arshitgupta34@gmail.com).', 'सभी नए पंजीकरण ईमेल की वैधता की पुष्टि के लिए जीमेल द्वारा भेजे गए लिंक का उपयोग करते हैं।')],
    [t('PostgreSQL Row-Level Security', 'PostgreSQL रो-लेवल सुरक्षा (RLS)'), t('Profiles and saved legal references enforce `(select auth.uid()) = user_id` at the database layer.', 'प्रोफ़ाइल और सहेजे गए कानूनी संदर्भ डेटाबेस स्तर पर `(select auth.uid()) = user_id` लागू करते हैं।')],
    [t('HttpOnly Session + CSRF Protection', 'HttpOnly सत्र + CSRF सुरक्षा'), t('Browser sessions stay in strict HttpOnly cookies rather than exposing tokens to scripts.', 'ब्राउज़र सत्र सुरक्षित HttpOnly कुकीज़ में रहते हैं।')],
    [t('Separation of Duties', 'जिम्मेदारियों का पृथक्करण'), t('Department staff can request closure, but only an independent reviewer can approve or reopen a case.', 'विभागीय कर्मचारी बंद करने का अनुरोध कर सकते हैं, लेकिन केवल स्वतंत्र समीक्षक ही स्वीकृति दे सकता है।')],
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
    t('Register your account. We will send an official verification link to confirm your email is valid.', 'अपना खाता पंजीकृत करें। आपका ईमेल मान्य है या नहीं, इसकी पुष्टि के लिए हम एक आधिकारिक सत्यापन लिंक भेजेंगे।'),
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
  const submit = el('button', 'button', t('Create Account & Send Verification Link  →', 'खाता बनाएँ और सत्यापन लिंक भेजें  →'));
  submit.type = 'submit';

  const switchRow = el('p', 'microcopy');
  switchRow.append(
    document.createTextNode(t('Already have an account? ', 'पहले से खाता है? ')),
    link(t('Sign in instead  →', 'साइन इन करें  →'), '/login', 'text-link'),
  );

  form.append(fullName.wrapper, email.wrapper, ward.wrapper, organization.wrapper, password.wrapper, strengthWrap, confirmPassword.wrapper, alert, submit, switchRow);

  function showVerificationCard(res: { email: string; message: string; devVerificationLink?: string | null; devToken?: string | null }) {
    form.replaceChildren();
    alert.replaceChildren();

    const card = el('div', 'verify-step-header');
    card.append(
      el('span', 'label', t('✉️ SIGN-UP EMAIL VERIFICATION LINK SENT', '✉️ साइन-अप ईमेल सत्यापन लिंक भेजा गया')),
      el('h2', '', t('Verify Your Email Address', 'अपना ईमेल पता सत्यापित करें')),
      el('p', '', t('We sent an official email verification link to confirm this email is valid: ', 'यह ईमेल मान्य है या नहीं, इसकी पुष्टि के लिए हमने सत्यापन लिंक भेजा है: ')),
      el('strong', 'target-email-badge', res.email),
      el('p', 'microcopy', t('Dispatched via Gmail API from arshitgupta34@gmail.com · Valid for 24 hours', 'arshitgupta34@gmail.com से प्रेषित · 24 घंटे के लिए मान्य'))
    );

    const tokenField = field(t('Enter verification token from email:', 'ईमेल से प्राप्त सत्यापन टोकन दर्ज करें:'), 'tokenInput', 'text');
    tokenField.input.placeholder = 'Paste verification token here';

    const verifyBtn = el('button', 'button', t('Verify Email & Activate Account  →', 'ईमेल सत्यापित करें और खाता सक्रिय करें  →'));
    verifyBtn.type = 'button';

    const resendBtn = button(t('🔄 Resend Verification Link', '🔄 सत्यापन लिंक पुनः भेजें'), async () => {
      resendBtn.disabled = true;
      notice(alert, t('Resending verification email to ', 'सत्यापन ईमेल पुनः भेजा जा रहा है: ') + res.email);
      try {
        await resendVerification(res.email);
        notice(alert, t('✓ A new verification link has been sent to your email.', '✓ नया सत्यापन लिंक आपके ईमेल पर भेज दिया गया है।'));
      } catch (err) {
        notice(alert, errorMessage(err), true);
      } finally {
        resendBtn.disabled = false;
      }
    }, 'quiet');

    verifyBtn.addEventListener('click', async () => {
      const tok = tokenField.input.value.trim();
      if (!tok) {
        notice(alert, t('Please enter the verification token.', 'कृपया सत्यापन टोकन दर्ज करें।'), true);
        return;
      }
      verifyBtn.disabled = true;
      notice(alert, t('Validating token and activating your civic account…', 'टोकन की जाँच हो रही है…'));
      try {
        const user = await verifyEmail(tok, res.email);
        if (!user) throw new Error('Account verification could not be confirmed.');
        notice(alert, t('✓ Email successfully verified! Welcome to TheekHai.', '✓ ईमेल सफलतापूर्वक सत्यापित! ठीकहै में आपका स्वागत है।'));
        navigate('/workspace');
      } catch (err) {
        notice(alert, errorMessage(err), true);
        verifyBtn.disabled = false;
      }
    });

    const linksRow = el('div', 'auth-meta-row');
    linksRow.style.marginTop = '16px';
    linksRow.append(resendBtn, link(t('Sign in instead  →', 'साइन इन करें  →'), '/login', 'text-link'));

    form.append(card, tokenField.wrapper, alert, verifyBtn, linksRow);
  }

  form.addEventListener('submit', async e => {
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
    notice(alert, t('Checking email validity and sending verification link…', 'ईमेल की वैधता की पुष्टि और सत्यापन लिंक भेजा जा रहा है…'));
    try {
      const res = await registerAccount({
        fullName: fullName.input.value.trim(),
        email: email.input.value.trim(),
        password: password.input.value,
        organization: organization.input.value.trim() || 'Citizen Resident',
        ward: ward.input.value.trim() || 'Ward 1',
        preferredLanguage: document.documentElement.lang || 'en',
      });
      if (res.verificationSent) {
        showVerificationCard(res);
      } else {
        navigate('/account');
      }
    } catch (err) {
      notice(alert, errorMessage(err), true);
      submit.disabled = false;
    }
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

export async function verifyEmailScreen(parent: HTMLElement) {
  heading(
    parent,
    t('EMAIL VERIFICATION', 'ईमेल सत्यापन'),
    t('Confirm your email address.', 'अपने ईमेल पते की पुष्टि करें।'),
    t('Verifying that your email address is valid and activating your civic citizen account.', 'जाँच की जा रही है कि आपका ईमेल पता मान्य है और आपका नागरिक खाता सक्रिय किया जा रहा है।'),
  );

  const container = el('div', 'auth-shell');
  const panel = el('div', 'panel');
  panel.style.maxWidth = '580px';
  panel.style.margin = '0 auto';

  const alert = el('div');
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token') || '';
  const emailParam = params.get('email') || '';
  const isAlreadySuccess = params.get('status') === 'success';

  if (isAlreadySuccess || token) {
    const statusBox = el('div', 'verify-step-header');
    statusBox.append(
      el('span', 'label', t('VERIFYING ACCOUNT', 'खाते का सत्यापन')),
      el('h2', '', t('Validating Email Address…', 'ईमेल पते की पुष्टि हो रही है…')),
      el('p', '', t('Please wait while we confirm your email and issue your secure session.', 'कृपया प्रतीक्षा करें जब तक हम आपके ईमेल की पुष्टि करते हैं।'))
    );
    panel.append(statusBox, alert);
    container.append(panel);
    parent.append(container);

    try {
      const user = token ? await verifyEmail(token, emailParam || undefined) : identity;
      statusBox.replaceChildren(
        el('span', 'label', '✓ ' + t('EMAIL VERIFIED', 'ईमेल सत्यापित')),
        el('h2', '', t('Email Address Successfully Verified!', 'ईमेल पता सफलतापूर्वक सत्यापित हुआ!')),
        el('p', '', t('Your email is valid and your citizen account is now active. Loading your civic workspace…', 'आपका ईमेल मान्य है और आपका नागरिक खाता अब सक्रिय है। कार्यक्षेत्र लोड हो रहा है…')),
      );
      const actionRow = el('div', 'button-row');
      actionRow.style.marginTop = '16px';
      actionRow.append(link(t('Open Workspace Now  →', 'अभी कार्यक्षेत्र खोलें  →'), '/workspace', 'button'));
      panel.append(actionRow);

      setTimeout(() => navigate('/workspace'), 1800);
    } catch (err) {
      statusBox.replaceChildren(
        el('span', 'label', '✕ ' + t('VERIFICATION FAILED', 'सत्यापन विफल')),
        el('h2', '', t('Invalid or Expired Link', 'अमान्य या समाप्त लिंक')),
        el('p', '', errorMessage(err)),
      );
      const actionRow = el('div', 'button-row');
      actionRow.style.marginTop = '16px';
      actionRow.append(
        link(t('Sign Up Again  →', 'पुनः खाता बनाएँ  →'), '/signup', 'button'),
        link(t('Go to Sign In', 'साइन इन पर जाएँ'), '/login', 'quiet')
      );
      panel.append(actionRow);
    }
  } else {
    const header = el('div', 'verify-step-header');
    header.append(
      el('span', 'label', t('MANUAL VERIFICATION', 'मैनुअल सत्यापन')),
      el('h2', '', t('Enter Verification Token', 'सत्यापन टोकन दर्ज करें')),
      el('p', '', t('Enter the token from the email sent by arshitgupta34@gmail.com to activate your account:', 'खाता सक्रिय करने के लिए ईमेल से प्राप्त टोकन दर्ज करें:'))
    );
    const tokenField = field(t('Verification token', 'सत्यापन टोकन'), 'manualToken', 'text');
    const submitBtn = el('button', 'button', t('Verify Email & Continue  →', 'ईमेल सत्यापित करें और आगे बढ़ें  →'));
    submitBtn.addEventListener('click', async () => {
      const tok = tokenField.input.value.trim();
      if (!tok) {
        notice(alert, t('Please enter the token.', 'कृपया टोकन दर्ज करें।'), true);
        return;
      }
      submitBtn.disabled = true;
      notice(alert, t('Verifying…', 'जाँच हो रही है…'));
      try {
        const user = await verifyEmail(tok);
        if (!user) throw new Error('Verification failed.');
        navigate('/workspace');
      } catch (err) {
        notice(alert, errorMessage(err), true);
        submitBtn.disabled = false;
      }
    });

    panel.append(header, tokenField.wrapper, alert, submitBtn);
    container.append(panel);
    parent.append(container);
  }
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
  try{const result=await api<{items:CaseRecord[],total:number}>('/complaints');container.replaceChildren();const stats=el('div','stats');for(const [label,value] of [[t('Visible cases','दिख रहे मामले'),result.total],[t('Awaiting review','समीक्षा बाकी'),result.items.filter(x=>x.status==='PENDING_VERIFICATION').length],[t('Overdue','समय-सीमा पार'),result.items.filter(x=>x.status!=='CLOSED'&&x.dueAt<Date.now()).length]]){const item=el('div');item.append(el('span','label',String(label)),el('strong','stat-value',String(value)));stats.append(item)}container.append(stats);const toolbar=el('div','list-toolbar'),search=el('input');search.type='search';search.placeholder=t('Search by title, address, category or department…','शीर्षक, पता, श्रेणी या विभाग से खोजें…');search.setAttribute('aria-label',search.placeholder);toolbar.append(el('h2','',t('Case activity','मामलों की गतिविधि')),search);const statusBar=el('div','map-filter-bar');let activeStatus='ALL';container.append(toolbar,statusBar);const list=el('div','case-list');container.append(list);
  const drawFilters=()=>{statusBar.replaceChildren();for(const [code,label] of [['ALL',t('All cases','सभी मामले')],['OPEN',t('Active / Open','सक्रिय')],['PENDING_VERIFICATION',t('Awaiting review','समीक्षा बाकी')],['CLOSED',t('Closed','बंद')]]){statusBar.append(button(label,()=>{activeStatus=code;drawFilters();draw()},activeStatus===code?'button small':'button secondary small'))}};
  const draw=()=>{list.replaceChildren();const q=search.value.toLowerCase().trim();const cases=result.items.filter(x=>{const matchText=!q||(x.title+' '+x.department+' '+x.address+' '+x.category+' '+x.id).toLowerCase().includes(q);const matchStatus=activeStatus==='ALL'||(activeStatus==='OPEN'&&x.status!=='CLOSED')||x.status===activeStatus;return matchText&&matchStatus});if(!cases.length){const empty=el('div','empty-state');empty.append(el('h3','',t('A fresh start.','एक नई शुरुआत।')),el('p','',t('No matching cases yet. Submit a report to start following its progress.','अभी कोई संबंधित मामला नहीं। प्रगति देखने के लिए रिपोर्ट भेजें।')),link(t('Report an issue','समस्या दर्ज करें'),'/report','button'));list.append(empty)}for(const record of cases){const row=link('',`/cases/${record.id}`,'case-row');const content=el('div');content.append(el('span','label',record.category+' · '+record.id.slice(0,8)),el('h3','',record.title),el('p','',record.address));const meta=el('div','case-meta');meta.append(el('span',`status status-${record.status.toLowerCase()}`,statusText(record.status)),el('span','',record.department),el('small','',t('Due ','समय-सीमा ')+date(record.dueAt)));row.append(content,meta,el('span','row-arrow','↗'));list.append(row)}refreshScroll()};search.addEventListener('input',draw);drawFilters();draw();if(result.total>50)container.append(el('p','microcopy',t('Showing the first 50 cases.','पहले 50 मामले दिखाए जा रहे हैं।')));
  }catch(error){notice(container,errorMessage(error),true);container.append(button(t('Retry','फिर कोशिश करें'),()=>void render(),'button secondary'))}
}

export async function detail(parent:HTMLElement,id:string) {
  if(authGate(parent))return;const content=el('div');parent.append(content);notice(content,t('Loading case…','मामला लोड हो रहा है…'));
  try{const data=await api<CaseDetail>(`/complaints/${encodeURIComponent(id)}`);content.replaceChildren();const record=data.case;content.append(link(t('← Back to workspace','← कार्यक्षेत्र में लौटें'),'/workspace','text-link'));heading(content,record.category+' / '+record.id.slice(0,8),record.title,record.address);const layout=el('div','detail-grid'),body=el('div'),summary=el('section','panel'),tasks=el('section','panel'),timeline=el('section','panel');summary.append(el('span',`status status-${record.status.toLowerCase()}`,statusText(record.status)),el('p','case-description',record.description),el('p','',t('Lead department: ','मुख्य विभाग: ')+record.department),el('p','',t('Due: ','समय-सीमा: ')+date(record.dueAt)));if(record.latitude!=null&&record.longitude!=null){const geoRow=el('div','geo-controls');const badge=el('span','geo-badge',`📍 ${record.latitude.toFixed(5)}° N, ${record.longitude.toFixed(5)}° E`);geoRow.append(badge,link(t('View on Civic Map  ↗','नागरिक मानचित्र पर देखें  ↗'),'/map','text-link'));summary.append(geoRow)}
if(data.links && data.links.length){
  const linksWrap=el('div','case-links');
  linksWrap.append(el('strong','',t('Linked / Duplicate cases: ','संबद्ध / डुप्लिकेट मामले: ')));
  for(const l of data.links){
    linksWrap.append(link(`#${l.targetId.slice(0,8)} (${l.kind}) `,`/cases/${l.targetId}`,'text-link'));
  }
  summary.append(linksWrap);
}
summary.append(el('p','microcopy',t('Pilot service target, not a statutory deadline.','पायलट सेवा लक्ष्य, वैधानिक समय-सीमा नहीं।')));tasks.append(el('h2','',t('Coordinated work','समन्वित कार्य')));for(const task of data.tasks){const row=el('div','task-row');row.append(el('span',task.complete?'task-done':'task-open',task.complete?'✓':'○'),el('span','',task.title),el('small','',task.department));if(!task.complete&&record.status==='IN_PROGRESS'&&(identity?.role==='ADMIN'||identity?.department===task.department))row.append(button(t('Complete','पूरा करें'),()=>void act('COMPLETE_TASK',task.id),'quiet'));tasks.append(row)}
  // Official Civic Email Dispatch Section (Sender: arshitgupta34@gmail.com, Receiver dynamically discovered via TinyFish)
  const emailSection = el('section', 'panel email-dispatch-panel');
  emailSection.append(
    el('span', 'label', '⚡ TINYFISH CIVIC DISCOVERY & EMAIL DISPATCH'),
    el('h2', '', t('Official Civic Email Dispatch', 'आधिकारिक नागरिक ईमेल प्रेषण')),
    el('p', 'microcopy', t('Send statutory grievance petition directly to the responsible authority dynamically discovered by TinyFish from government sites. Dispatched directly from arshitgupta34@gmail.com.', 'सरकारी वेबसाइटों से टाइनीफिश द्वारा खोजे गए जिम्मेदार प्राधिकारी को सीधे वैधानिक शिकायत भेजें। arshitgupta34@gmail.com से सीधे प्रेषित।')),
    el('div', 'geo-badge', '📤 ' + t('Verified Sender: arshitgupta34@gmail.com · Automated Direct Delivery', 'सत्यापित प्रेषक: arshitgupta34@gmail.com · स्वचालित सीधा प्रेषण'))
  );

  const emailAlert = el('div');
  const emailFormWrap = el('div', 'email-form-wrap');
  const draftBtn = button(t('🤖 Discover Authority via TinyFish & Draft Petition', '🤖 टाइनीफिश द्वारा अधिकारी खोजें और ड्राफ्ट करें'), async () => {
    draftBtn.disabled = true;
    notice(emailAlert, t('TinyFish scanning government portals for responsible authority…', 'टाइनीफिश सरकारी पोर्टलों पर जिम्मेदार अधिकारी खोज रहा है…'));
    try {
      const draft = await draftComplaintEmail(record.id);
      renderEmailComposer(draft);
    } catch (err) {
      notice(emailAlert, errorMessage(err), true);
    } finally {
      draftBtn.disabled = false;
    }
  }, 'button secondary');

  function renderEmailComposer(draft: import('./api').EmailDraft) {
    emailFormWrap.replaceChildren();
    emailAlert.replaceChildren();
    const senderBadge = el('p', 'microcopy', '📤 ' + t('Sender: ', 'प्रेषक: ') + (draft.senderEmail || 'arshitgupta34@gmail.com') + ' · ' + t('Discovery Mode: ', 'खोज मोड: ') + draft.discoveryMode);
    const toField = field(t('Authority Email (Discovered)', 'प्राधिकारी ईमेल (खोजा गया)'), 'toEmail');
    toField.input.value = draft.recipientEmail;
    const authNote = el('p', 'microcopy', `🏢 ${draft.recipientAuthority} (${draft.recipientDepartment}) · Source: ${draft.officialSourceUrl}`);
    const subjField = field(t('Subject', 'विषय'), 'emailSubject');
    subjField.input.value = draft.subject;
    const bodyField = el('div', 'field');
    const bodyLabel = el('label', 'label', t('Petition Letter', 'शिकायत पत्र'));
    const bodyText = el('textarea');
    bodyText.rows = 8;
    bodyText.value = draft.body;
    bodyField.append(bodyLabel, bodyText);

    const sendBtn = button(t('🚀 Dispatch Official Email', '🚀 आधिकारिक ईमेल भेजें'), async () => {
      sendBtn.disabled = true;
      notice(emailAlert, t('Dispatching official email from arshitgupta34@gmail.com…', 'ईमेल भेजा जा रहा है…'));
      try {
        const res = await sendComplaintEmail(record.id, toField.input.value, subjField.input.value, bodyText.value);
        notice(emailAlert, `✓ ${res.message}`);
        emailFormWrap.replaceChildren();
        const newEvent = el('div', 'timeline-event');
        newEvent.append(
          el('span', 'label', 'GRIEVANCE EMAILED'),
          el('p', '', `Official petition dispatched to ${toField.input.value} (${res.messageId})`),
          el('time', 'microcopy', date(res.sentAt || Date.now()))
        );
        const firstEvent = timeline.querySelector('.timeline-event');
        if (firstEvent) {
          timeline.insertBefore(newEvent, firstEvent);
        } else {
          timeline.append(newEvent);
        }
      } catch (err) {
        notice(emailAlert, errorMessage(err), true);
        sendBtn.disabled = false;
      }
    });

    emailFormWrap.append(senderBadge, toField.wrapper, authNote, subjField.wrapper, bodyField, sendBtn);
  }

  emailSection.append(draftBtn, emailAlert, emailFormWrap);
  timeline.append(el('h2','',t('The complete story','पूरा घटनाक्रम')));
for(const event of [...data.events].reverse()){const item=el('div','timeline-event');item.append(el('span','label',event.action.replaceAll('_',' ')),el('p','',event.note),el('time','microcopy',date(event.at)));timeline.append(item)}body.append(summary,tasks,emailSection,timeline);
  const actions=el('aside','panel action-panel');actions.append(el('p','eyebrow',t('NEXT ACTION','अगला कदम')),el('h2','',t('Move it forward.','आगे बढ़ाएँ।')));const reason=el('textarea');reason.rows=4;reason.placeholder=t('Explain your action or provide review notes (optional).','अपना निर्णय या समीक्षा टिप्पणी लिखें (वैकल्पिक)।');reason.setAttribute('aria-label',reason.placeholder);const alert=el('div');actions.append(reason,alert);
  async function act(action:string,target=''){
    const defaultNote = action==='START'?t('Field crew commenced remediation work.','फील्ड टीम ने मरम्मत कार्य शुरू किया।'):
      action==='REQUEST_CLOSURE'?t('Field work completed, requesting formal verification.','कार्य पूर्ण, औपचारिक सत्यापन का अनुरोध।'):
      action==='APPROVE_CLOSURE'?t('Resolution verified and approved.','समाधान सत्यापित और स्वीकृत।'):
      action==='REJECT_CLOSURE'?t('Incomplete resolution; returned for further work.','अधूरा समाधान; कार्य के लिए लौटाया गया।'):
      action==='CHALLENGE'?t('Citizen reported issue remains unresolved.','नागरिक ने बताया कि समस्या हल नहीं हुई।'):
      action==='REOPEN'?t('Case reopened following challenge review.','आपत्ति समीक्षा के बाद मामला पुनः खोला गया।'):
      action==='ADD_TASK'?t('Added coordinated municipal task.','समन्वित नगरपालिका कार्य जोड़ा गया।'):
      t('Workflow state updated.','स्थिति अद्यतन की गई।');
    const note = reason.value.trim() || defaultNote;
    try{await api(`/complaints/${id}/actions`,{method:'POST',body:JSON.stringify({action,version:record.version,note,target,department:department.value})});await render()}catch(error){notice(alert,errorMessage(error),true)}
  }
  const department=el('select');department.setAttribute('aria-label',t('Department for task','कार्य का विभाग'));for(const name of ['Public works','Water services','Sanitation','Electrical services','Civic triage'])department.append(el('option','',name));
  const staff=identity?.role==='ADMIN'||identity?.role==='DEPARTMENT';const reviewer=identity?.role==='ADMIN'||identity?.role==='REVIEWER';
  const hasPendingChallenge = data.events.some(ev => ev.action === 'CHALLENGE' && ev.at >= record.updatedAt);
  if(staff&&['ASSIGNED','REOPENED'].includes(record.status))actions.append(button(t('Start work','काम शुरू करें'),()=>void act('START')));
  if(staff&&['ASSIGNED','REOPENED','IN_PROGRESS'].includes(record.status)){actions.append(department,button(t('Add required task','जरूरी कार्य जोड़ें'),()=>void act('ADD_TASK'),'button secondary'))}
  if(staff&&record.status==='IN_PROGRESS')actions.append(button(t('Request closure review','बंद करने की समीक्षा माँगें'),()=>void act('REQUEST_CLOSURE')));
  if(reviewer&&record.status==='PENDING_VERIFICATION'&&record.closureBy!==identity?.id){actions.append(button(t('Approve closure','बंद करना स्वीकार करें'),()=>void act('APPROVE_CLOSURE')),button(t('Return for more work','आगे काम के लिए लौटाएँ'),()=>void act('REJECT_CLOSURE'),'button secondary'))}
  if(record.status==='CLOSED'&&record.citizenId===identity?.id&&!hasPendingChallenge)actions.append(button(t('Challenge this closure','बंद होने पर सवाल उठाएँ'),()=>void act('CHALLENGE'),'button secondary'));
  if(record.status==='CLOSED'&&hasPendingChallenge){actions.append(el('span','status status-assigned',t('Citizen challenge submitted — awaiting reviewer decision.','नागरिक आपत्ति दर्ज — स्वतंत्र समीक्षक के निर्णय की प्रतीक्षा है।')))}
  if(reviewer&&record.status==='CLOSED'&&hasPendingChallenge)actions.append(button(t('Uphold challenge & reopen','आपत्ति स्वीकार करें और फिर खोलें'),()=>void act('REOPEN'),'button secondary'));
  if(staff||reviewer){
    actions.append(button(t('🌐 Verify Portal Resolution','🌐 पोर्टल पर समाधान सत्यापित करें'),async ()=>{
      notice(alert,t('Verifying resolution against municipal standards…','नगर निगम मानकों के विरुद्ध समाधान की जाँच जारी है…'));
      try{
        const verif = await verifyCaseResolution(record.id);
        notice(alert,`TinyFish (${verif.state}): ${verif.explanation}`);
        await render();
      }catch(err){notice(alert,errorMessage(err),true)}
    },'button secondary'));
  }
  actions.append(el('p','microcopy',t('Every decision is recorded. Closure requires an independent reviewer.','हर फैसला दर्ज होता है। बंद करने के लिए स्वतंत्र समीक्षक आवश्यक है।')));layout.append(body,actions);content.append(layout);
  }catch(error){notice(content,errorMessage(error),true)}
}

export function library(parent:HTMLElement){
  heading(parent,t('THE REFERENCE LIBRARY','संदर्भ लाइब्रेरी'),t('Know your next step.','अपना अगला कदम जानें।'),t('Approved civic guidance, statutory rights, and autonomous regulatory intelligence powered by TinyFish.','नागरिक अधिकारों, वैधानिक नियमों और टाइनीफिश स्वायत्त विनियामक खोज का स्वीकृत केंद्र।'));
  const provBanner=el('div','stats stats-4');
  parent.append(provBanner);
  const providerNote=el('p','microcopy',t('Configuration indicators only — verify live statutory queries and agent runs below.','कॉन्फ़िगरेशन संकेतक — नीचे दिए गए अनुभाग में लाइव विनियामक खोज और टाइनीफिश एजेंट चलाएँ।'));
  provBanner.after(providerNote);
  void api<{supabaseConfigured:boolean,tinyFishConfigured:boolean,tavilyConfigured:boolean,openRouterConfigured:boolean,sarvamConfigured?:boolean}>('/providers').then(p=>{
    provBanner.replaceChildren();
    for(const [name,ok] of [['Supabase Auth',p.supabaseConfigured],['TinyFish Agent & Monitor',p.tinyFishConfigured],['OpenRouter AI',p.openRouterConfigured],['Tavily Official Search',p.tavilyConfigured],['Sarvam AI Speech Fallback',Boolean(p.sarvamConfigured)]]){
      const item=el('div');
      item.append(el('span','label',String(name)),el('span',ok?'status status-closed':'status status-assigned',ok?t('Configured','कॉन्फ़िगर है'):t('Not configured','कॉन्फ़िगर नहीं')));
      provBanner.append(item);
    }
  }).catch(()=>{provBanner.replaceChildren(el('div','microcopy',t('Provider configuration is unavailable.','प्रदाता कॉन्फ़िगरेशन उपलब्ध नहीं है।')))});

  // Statutory Q&A Search Form
  const qaSection = el('section', 'panel');
  qaSection.append(
    el('span', 'label', t('STATUTORY CITATION SEARCH', 'वैधानिक उद्धरण खोज')),
    el('h2', '', t('Ask official guidance', 'आधिकारिक मार्गदर्शन खोजें'))
  );
  const form=el('form','library-form'),query=field(t('What would you like to understand?','आप क्या समझना चाहते हैं?'),'question');
  query.input.placeholder=t('e.g. How does the Haryana Auto Appeal System (AAS) handle overdue services?','जैसे हरियाणा ऑटो अपील सिस्टम (AAS) समय-सीमा पार होने पर कैसे काम करता है?');
  query.input.minLength=3;query.input.maxLength=1000;
  const jurisdiction=el('select');jurisdiction.setAttribute('aria-label',t('Jurisdiction','क्षेत्राधिकार'));
  for(const [value,label] of [['CENTRAL',t('Central guidance','केंद्रीय मार्गदर्शन')],['HARYANA',t('Haryana + central guidance','हरियाणा और केंद्रीय मार्गदर्शन')]]){
    const option=el('option','',label);option.value=value;jurisdiction.append(option);
  }
  const search=el('button','button',t('Find guidance','मार्गदर्शन खोजें'));search.type='submit';
  const answer=el('div','legal-results');
  form.append(query.wrapper,jurisdiction,search,el('p','microcopy',t('Official regulatory guidance grounded in verified statutory sources.','सत्यापित आधिकारिक और वैधानिक स्रोतों पर आधारित मार्गदर्शन।')));
  qaSection.append(form, answer);
  parent.append(qaSection);

  function sources(items:LegalSource[]){
    for(const source of items){
      const card=el('article','panel source-card');
      const a=link(source.title,source.url,'text-link');a.target='_blank';a.rel='noopener noreferrer';
      const foot=el('div','source-card-foot');foot.append(el('small','microcopy',t('Retrieved ','प्राप्त ')+date(source.retrievedAt)));
      if(identity){
        const saveBtn=button(t('Save to account','खाते में सहेजें'),()=>{
          saveBtn.disabled=true;
          void saveSourceBookmark({title:source.title,sourceUrl:source.url,jurisdiction:source.jurisdiction,snippet:source.content.slice(0,400)}).then(()=>{saveBtn.textContent=t('Saved ✓','सहेजा गया ✓')}).catch(()=>{saveBtn.disabled=false});
        },'quiet');
        foot.append(saveBtn);
      }
      card.append(el('span','label',source.jurisdiction+' · '+source.language),a,el('p','',source.content.slice(0,700)),foot);
      answer.append(card);
    }
  }

  form.addEventListener('submit',event=>{
    event.preventDefault();search.disabled=true;
    notice(answer,t('Searching approved official guidance…','स्वीकृत आधिकारिक मार्गदर्शन खोजा जा रहा है…'));
    void(async()=>{
      try{
        if(identity){
          const result=await api<LegalAnswer>('/legal/ask',{method:'POST',body:JSON.stringify({question:query.input.value,jurisdiction:jurisdiction.value})});
          const badge=el('span','label',`Mode: ${result.mode}`);
          answer.replaceChildren(badge,el('p','legal-answer',result.answer),el('p','microcopy',result.limitation));
          sources(result.citations);
        }else{
          const result=await api<LegalSource[]>(`/legal/sources?q=${encodeURIComponent(query.input.value)}&jurisdiction=${jurisdiction.value}`);
          answer.replaceChildren();
          if(!result.length)notice(answer,t('No approved matching sources are installed yet.','अभी कोई संबंधित स्वीकृत स्रोत स्थापित नहीं है।'));
          sources(result);
        }
      }catch(error){notice(answer,errorMessage(error),true)}finally{search.disabled=false}
    })();
  });

  // Dedicated TinyFish Civic Intelligence & Autonomous Agent Hub
  const fishHub = el('section', 'tinyfish-hub');
  const hubHeader = el('div');
  hubHeader.append(
    el('span', 'tinyfish-badge', '⚡ TINYFISH CIVIC INTELLIGENCE ENGINE'),
    el('h2', '', t('Autonomous Web Agent & Regulatory Monitors', 'स्वायत्त वेब एजेंट और विनियामक मॉनिटर')),
    el('p', 'microcopy', t('TinyFish autonomous web agents navigate official government portals, extract citizen service charters, monitor gazettes for regulatory changes, and discover responsible nodal officers.', 'टाइनीफिश स्वायत्त वेब एजेंट सरकारी वेबसाइटों से नागरिक चार्टर निकालते हैं, गजट परिवर्तनों पर नजर रखते हैं और संबंधित नोडल अधिकारियों की खोज करते हैं।'))
  );

  const tabsWrap = el('div', 'tinyfish-tabs');
  const agentTabBtn = button(t('🤖 Autonomous Web Agent', '🤖 स्वायत्त वेब एजेंट'), () => switchTab('agent'), 'tinyfish-tab-btn active');
  const monitorTabBtn = button(t('📡 Continuous Monitors', '📡 विनियामक मॉनिटर'), () => switchTab('monitors'), 'tinyfish-tab-btn');
  const contactTabBtn = button(t('🏢 Authority Discovery', '🏢 प्राधिकारी खोज'), () => switchTab('contact'), 'tinyfish-tab-btn');
  tabsWrap.append(agentTabBtn, monitorTabBtn, contactTabBtn);

  if (identity?.role === 'REVIEWER' || identity?.role === 'ADMIN') {
    const reviewTabBtn = button(t('⚖️ Review Queue', '⚖️ समीक्षा कतार'), () => switchTab('review'), 'tinyfish-tab-btn');
    tabsWrap.append(reviewTabBtn);
  }

  const tabContent = el('div', 'tinyfish-tab-content');
  fishHub.append(hubHeader, tabsWrap, tabContent);
  parent.append(fishHub);

  function switchTab(tab: 'agent' | 'monitors' | 'contact' | 'review') {
    tabsWrap.querySelectorAll('.tinyfish-tab-btn').forEach(b => b.classList.remove('active'));
    tabContent.replaceChildren();

    if (tab === 'agent') {
      agentTabBtn.classList.add('active');
      renderAgentTab();
    } else if (tab === 'monitors') {
      monitorTabBtn.classList.add('active');
      renderMonitorsTab();
    } else if (tab === 'contact') {
      contactTabBtn.classList.add('active');
      renderContactTab();
    } else if (tab === 'review') {
      tabsWrap.querySelectorAll('.tinyfish-tab-btn')[3]?.classList.add('active');
      renderReviewTab();
    }
  }

  function renderAgentTab() {
    tabContent.replaceChildren();
    const subhead = el('h3', '', t('Run TinyFish Agent on Government Portals', 'सरकारी पोर्टलों पर टाइनीफिश एजेंट चलाएँ'));
    const pillsWrap = el('div', 'tinyfish-pills');
    const presets = [
      'Haryana Auto Appeal System (AAS) guidelines',
      'Sonipat Municipal Council grievance escalation',
      'Streetlight repair statutory SLA under RTSC',
      'Road pothole repair standard specification Sonipat'
    ];
    for (const p of presets) {
      const pill = button(p, () => {
        agentQuery.input.value = p;
      }, 'tinyfish-pill');
      pillsWrap.append(pill);
    }

    const agentForm = el('form', 'library-form');
    const agentQuery = field(t('Autonomous Search Goal or Policy Topic', 'स्वायत्त खोज लक्ष्य या नीति विषय'), 'agentQuery');
    agentQuery.input.value = 'Haryana Right to Service Commission municipal deadlines';
    const agentUrl = field(t('Official Target Portal URL (Optional Seed URL)', 'आधिकारिक पोर्टल URL (वैकल्पिक)'), 'agentUrl');
    agentUrl.input.placeholder = 'https://haryana-rtsc.gov.in';
    const agentBtn = el('button', 'button', t('🚀 Launch TinyFish Autonomous Agent', '🚀 टाइनीफिश स्वायत्त एजेंट प्रारंभ करें'));
    agentBtn.type = 'submit';
    agentForm.append(agentQuery.wrapper, agentUrl.wrapper, agentBtn);

    const agentResults = el('div', 'legal-results');
    agentForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      agentBtn.disabled = true;
      notice(agentResults, t('⚡ TinyFish Agent scanning target government domains in real-time…', '⚡ टाइनीफिश एजेंट सरकारी डोमेन स्कैन कर रहा है…'));
      try {
        const res = await api<LegalSource[]>('/legal/discover', {
          method: 'POST',
          body: JSON.stringify({
            query: agentQuery.input.value,
            url: agentUrl.input.value,
            jurisdiction: jurisdiction.value,
            language: 'en'
          })
        });
        agentResults.replaceChildren();
        if (!res.length) {
          notice(agentResults, t('TinyFish agent completed scan. Verified excerpts queued for municipal review.', 'स्कैन पूर्ण। उद्धरण समीक्षा कतार में जोड़े गए हैं।'));
        } else {
          for (const s of res) {
            const card = el('article', 'panel source-card');
            card.append(
              el('span', 'tinyfish-badge', `DISCOVERED: ${s.jurisdiction}`),
              link(s.title, s.url, 'text-link'),
              el('p', '', s.content.slice(0, 500) + '…'),
              el('small', 'microcopy', t('Source: ', 'स्रोत: ') + s.url)
            );
            agentResults.append(card);
          }
        }
      } catch (err) {
        notice(agentResults, errorMessage(err), true);
      } finally {
        agentBtn.disabled = false;
      }
    });

    tabContent.append(subhead, pillsWrap, agentForm, agentResults);
  }

  function renderMonitorsTab() {
    tabContent.replaceChildren();
    const headWrap = el('div');
    headWrap.append(
      el('h3', '', t('Continuous Statutory Monitors', 'सक्रिय विनियामक मॉनिटर')),
      el('p', 'microcopy', t('TinyFish schedules background checks across gazettes and municipal circulars, flagging SLA updates.', 'टाइनीफिश गजट और नगर निगम परिपत्रों की स्वतः जाँच करता है।'))
    );
    const monGrid = el('div', 'tinyfish-monitor-grid');
    const statusNote = el('div');
    tabContent.append(headWrap, statusNote, monGrid);

    void api<LegalMonitor[]>('/legal/monitors').then(monitors => {
      monGrid.replaceChildren();
      for (const m of monitors) {
        const card = el('div', 'tinyfish-monitor-card');
        const cardHeader = el('div', 'tinyfish-card-header');
        cardHeader.append(
          el('span', 'label', `${m.monitorType.toUpperCase()} · ${m.jurisdiction}`),
          el('span', 'status status-closed', m.lastStatus ?? m.status)
        );
        card.append(
          cardHeader,
          el('h4', 'tinyfish-card-title', m.name),
          el('p', 'tinyfish-card-target', m.target),
          button(t('⚡ Run Check Now', '⚡ अभी जाँचें'), async () => {
            notice(statusNote, t('Triggering TinyFish monitor check on ', 'टाइनीफिश मॉनिटर चला रहे हैं: ') + m.target);
            try {
              const res = await api<MonitorRunResult>(`/legal/monitors/${m.id}/run`, { method: 'POST' });
              notice(statusNote, `✓ Monitor executed (${res.providerMode}). Queued ${res.queuedSources.length} new statutory updates.`);
              renderMonitorsTab();
            } catch (e) {
              notice(statusNote, errorMessage(e), true);
            }
          }, 'button secondary small')
        );
        monGrid.append(card);
      }
    }).catch(err => notice(statusNote, errorMessage(err), true));
  }

  function renderContactTab() {
    tabContent.replaceChildren();
    tabContent.append(
      el('h3', '', t('Authority Contact Discovery Simulator', 'प्राधिकारी संपर्क खोज सिमुलेटर')),
      el('p', 'microcopy', t('Preview how TinyFish dynamically navigates official portals to discover municipal contacts:', 'देखें कि कैसे टाइनीफिश आधिकारिक पोर्टलों से संपर्क खोजता है:'))
    );
    const testCard = el('div', 'panel');
    testCard.append(
      el('span', 'tinyfish-badge', 'TINYFISH NODAL DISCOVERY'),
      el('p', '', '🏢 Department: Public Works Department (B&R), Sonipat Division'),
      el('p', '', '📍 Target Jurisdiction: Ward 4, Murthal / Sonipat, Haryana'),
      el('p', '', '📧 Discovered Official Email: eepwd-sonipat@hry.nic.in'),
      el('p', '', '🌐 Verified Government Source: https://haryanapwd.gov.in/contacts'),
      el('p', 'microcopy', t('Grievance emails dispatched via verified sender arshitgupta34@gmail.com.', 'शिकायतें सत्यापित प्रेषक arshitgupta34@gmail.com से भेजी जाती हैं।'))
    );
    tabContent.append(testCard);
  }

  function renderReviewTab() {
    tabContent.replaceChildren();
    tabContent.append(el('h3', '', t('Pending Regulatory Review Queue', 'लंबित विनियामक समीक्षा कतार')));
    const reviewList = el('div', 'case-list');
    const reviewAlert = el('div');
    tabContent.append(reviewAlert, reviewList);

    void api<LegalSource[]>('/legal/review').then(pending => {
      reviewList.replaceChildren();
      if (!pending.length) {
        notice(reviewList, t('No pending sources awaiting review. All current guidance is approved.', 'कोई लंबित स्रोत नहीं है। सभी मार्गदर्शन स्वीकृत हैं।'));
        return;
      }
      for (const p of pending) {
        const row = el('div', 'task-row');
        row.append(
          el('span', 'label', p.jurisdiction),
          el('span', '', p.title),
          button(t('Approve', 'स्वीकार करें'), () => {
            void api(`/legal/sources/${p.id}/review`, { method: 'POST', body: JSON.stringify({ approve: true }) })
              .then(() => renderReviewTab());
          }, 'quiet'),
          button(t('Reject', 'अस्वीकार करें'), () => {
            void api(`/legal/sources/${p.id}/review`, { method: 'POST', body: JSON.stringify({ approve: false }) })
              .then(() => renderReviewTab());
          }, 'quiet')
        );
        reviewList.append(row);
      }
    }).catch(err => notice(reviewAlert, errorMessage(err), true));
  }

  // Initialize with Agent tab
  switchTab('agent');
}

export function information(parent:HTMLElement,path:string){const pages:Record<string,[string,string]>={
  '/privacy':[t('Your information, handled with care.','आपकी जानकारी, सावधानी के साथ।'),t('This pilot sends reports to the common backend and limits access by account and department. User profiles and bookmarked sources are stored in Supabase PostgreSQL under Row-Level Security (`(select auth.uid()) = user_id`). Drafts are stored on this browser only when you choose Save draft. Location and microphone access are optional and requested when used. Legal reference queries use approved sources with TinyFish Agent Mode, Tavily official-domain fallback, and a local approved-source backup fallback (SOURCES_ONLY); private complaint details are excluded from source discovery.','यह पायलट रिपोर्ट साझा बैकएंड पर भेजता है और खाते तथा विभाग के अनुसार पहुँच सीमित करता है। उपयोगकर्ता प्रोफ़ाइल और सहेजे गए स्रोत सुपाबेस PostgreSQL में रो-लेवल सुरक्षा (RLS) के अंतर्गत सुरक्षित रहते हैं। ड्राफ्ट केवल आपके चुनने पर इस ब्राउज़र में सहेजा जाता है। स्थान और माइक्रोफोन वैकल्पिक हैं। कानूनी संदर्भ प्रश्न स्वीकृत स्रोतों के साथ टाइनीफिश एजेंट मोड, टैविली फ़ॉलबैक और स्थानीय स्वीकृत स्रोत बैकअप (SOURCES_ONLY) का उपयोग करते हैं।')],
  '/terms':[t('Pilot terms','पायलट शर्तें'),t('This is a development pilot, not an official government filing channel or emergency service. Demonstration service targets are not legal deadlines. Reference excerpts do not establish eligibility or guarantee a legal outcome. Use synthetic information during testing. Public-launch terms remain subject to operator review.','यह विकास पायलट है, आधिकारिक सरकारी आवेदन माध्यम या आपातकालीन सेवा नहीं। प्रदर्शन सेवा लक्ष्य कानूनी समय-सीमा नहीं हैं। संदर्भ अंश पात्रता या कानूनी परिणाम सुनिश्चित नहीं करते। परीक्षण में काल्पनिक जानकारी उपयोग करें। सार्वजनिक लॉन्च की शर्तों की संचालक समीक्षा बाकी है।')],
  '/help':[t('A little guidance.','थोड़ा मार्गदर्शन।'),t('Start with a clear description and an address. Location and recording are optional: deny permission and continue with text. You can revoke permissions in your browser settings. Save a draft explicitly before leaving. A report is submitted only when a case ID is returned. Staff request closure; an independent reviewer decides it.','स्पष्ट विवरण और पते से शुरू करें। स्थान और रिकॉर्डिंग वैकल्पिक हैं: अनुमति न देकर लिखित रूप में आगे बढ़ें। ब्राउज़र सेटिंग्स से अनुमति वापस ले सकते हैं। जाने से पहले ड्राफ्ट सहेजें। मामला ID मिलने पर ही रिपोर्ट जमा मानी जाती है। कर्मचारी बंद करने का अनुरोध करते हैं; स्वतंत्र समीक्षक फैसला करता है।')]
};const page=pages[path];heading(parent,page?t('THE SMALL PRINT, CLEARLY','स्पष्ट जानकारी'):'404',page?.[0]??t('This street leads elsewhere.','यह रास्ता कहीं और जाता है।'),page?.[1]??t('We could not find that page. Let’s get you back on track.','यह पृष्ठ नहीं मिला। वापस चलते हैं।'));parent.append(link(t('Back to overview','परिचय पर लौटें'),'/','button'));}

interface MapPinItem {
  id: string;
  title: string;
  category: string;
  status: string;
  address: string;
  department: string;
  latitude: number;
  longitude: number;
  x: number;
  y: number;
}

export async function civicMap(parent: HTMLElement) {
  heading(
    parent,
    t('CIVIC GEOSPATIAL RADAR · WARD 4', 'नागरिक भू-स्थानिक रडार · वार्ड 4'),
    t('Your neighbourhood map.', 'आपके पड़ोस का मानचित्र।'),
    t('Explore active civic reports across Ward 4, filter by issue category, or click anywhere on the map to report an issue with exact GPS coordinates.', 'वार्ड 4 में सक्रिय नागरिक रिपोर्ट देखें, श्रेणी के अनुसार फ़िल्टर करें, या सटीक जीपीएस निर्देशांक के साथ रिपोर्ट करने के लिए मानचित्र पर कहीं भी क्लिक करें।'),
  );

  const minLon = 77.06180, maxLon = 77.08139, minLat = 29.02447, maxLat = 29.03465;
  function projX(lon: number) { return Math.max(40, Math.min(860, Math.round(((lon - minLon) / (maxLon - minLon)) * 900))); }
  function projY(lat: number) { return Math.max(40, Math.min(420, Math.round(((maxLat - lat) / (maxLat - minLat)) * 460))); }

  const defaultPins: MapPinItem[] = [
    { id: 'case-pilot-001', title: 'Waterlogged pothole on GT Road service lane near Murthal Chowk', category: 'ROADS', status: 'IN_PROGRESS', address: 'Murthal Chowk, GT Road, Ward 4', department: 'Public works', latitude: 29.0315, longitude: 77.0725, x: projX(77.0725), y: projY(29.0315) },
    { id: 'case-pilot-002', title: 'Three LED streetlights dark and flickering near Murthal Police Station', category: 'LIGHTING', status: 'ASSIGNED', address: 'Murthal Police Station Road, Ward 4', department: 'Electrical services', latitude: 29.0266, longitude: 77.0732, x: projX(77.0732), y: projY(29.0266) },
    { id: 'case-pilot-003', title: 'Stormwater drain grate dislodged near State Bank of India', category: 'ROADS', status: 'PENDING_VERIFICATION', address: 'State Bank of India Lane, Murthal, Ward 4', department: 'Public works', latitude: 29.0265, longitude: 77.0740, x: projX(77.0740), y: projY(29.0265) },
    { id: 'ward-004', title: 'Sewage drain overflow on commercial market stretch', category: 'WATER', status: 'ASSIGNED', address: 'Main Market Road near Gulshan Dhaba, Ward 4', department: 'Water services', latitude: 29.0337, longitude: 77.0711, x: projX(77.0711), y: projY(29.0337) },
    { id: 'ward-005', title: 'Uncollected solid waste pile near Murthal Bus Stop', category: 'WASTE', status: 'SUBMITTED', address: 'Murthal Chowk Bus Stop, Ward 4', department: 'Sanitation', latitude: 29.0312, longitude: 77.0729, x: projX(77.0729), y: projY(29.0312) },
  ];

  let allPins: MapPinItem[] = [...defaultPins];
  if (identity) {
    try {
      const live = await api<{ items: CaseRecord[]; total: number }>('/complaints');
      if (live.items.length) {
        const mapped: MapPinItem[] = live.items.map((c, idx) => {
          const lat = c.latitude ?? (29.0295 - (idx % 5) * 0.0018);
          const lon = c.longitude ?? (77.0715 + (idx % 5) * 0.0020);
          const relX = projX(lon);
          const relY = projY(lat);
          return {
            id: c.id,
            title: c.title,
            category: c.category,
            status: c.status,
            address: c.address,
            department: c.department,
            latitude: lat,
            longitude: lon,
            x: relX,
            y: relY,
          };
        });
        const existingIds = new Set(mapped.map(m => m.id));
        allPins = [...mapped, ...defaultPins.filter(d => !existingIds.has(d.id))];
      }
    } catch {
      // Fallback to default Ward 4 pins when offline
    }
  }

  let activeCat = 'ALL';
  let selectedPin: MapPinItem = allPins[0];

  const filterBar = el('div', 'map-filter-bar');
  const categories: [string, string][] = [
    ['ALL', t('All issues', 'सभी समस्याएँ')],
    ['ROADS', t('Roads & footpaths', 'सड़क और फुटपाथ')],
    ['WATER', t('Water & drains', 'पानी और नालियाँ')],
    ['WASTE', t('Waste & sanitation', 'कचरा और सफाई')],
    ['LIGHTING', t('Street lighting', 'स्ट्रीट लाइट')],
  ];

  const shell = el('div', 'civic-map-shell');
  const canvasCard = el('div', 'panel civic-map-card');
  const detailCard = el('aside', 'panel civic-map-detail');

  function renderDetail() {
    detailCard.replaceChildren();
    detailCard.append(
      el('p', 'eyebrow', `${selectedPin.category} · ${selectedPin.id.slice(0, 14)}`),
      el('span', `status status-${selectedPin.status.toLowerCase()}`, statusText(selectedPin.status)),
      el('h2', '', selectedPin.title),
      el('p', '', `📍 ${selectedPin.address}`),
      el('p', 'microcopy', `${t('Coordinates:', 'निर्देशांक:')} ${selectedPin.latitude.toFixed(5)}° N, ${selectedPin.longitude.toFixed(5)}° E`),
      el('p', 'microcopy', `${t('Lead Department:', 'मुख्य विभाग:')} ${selectedPin.department}`),
    );
    const btnGroup = el('div', 'actions');
    const reportHereUrl = `/report?lat=${selectedPin.latitude.toFixed(5)}&lng=${selectedPin.longitude.toFixed(5)}&address=${encodeURIComponent(selectedPin.address)}&category=${encodeURIComponent(selectedPin.category)}`;
    btnGroup.append(
      link(t('Report at this location  ↗', 'इस स्थान पर रिपोर्ट करें  ↗'), reportHereUrl, 'button'),
    );
    if (!selectedPin.id.startsWith('ward-')) {
      btnGroup.append(link(t('Open case file  →', 'केस फ़ाइल खोलें  →'), `/cases/${selectedPin.id}`, 'button secondary'));
    }
    detailCard.append(btnGroup);
  }

  function renderMapCanvas() {
    canvasCard.replaceChildren();
    const visiblePins = allPins.filter(p => activeCat === 'ALL' || p.category === activeCat);
    const svgNS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('viewBox', '0 0 900 460');
    svg.setAttribute('class', 'civic-map-svg');
    svg.innerHTML = `
      <rect width="900" height="460" fill="#f4f6f0"/>
      <!-- NH 44 / GT Road Arterial Corridor from map.osm -->
      <path d="M 410 0 L 460 210 L 520 460" stroke="#b8c5b2" stroke-width="36" fill="none"/>
      <path d="M 410 0 L 460 210 L 520 460" stroke="#fbbf24" stroke-width="2.5" stroke-dasharray="14 10" fill="none"/>
      <!-- Secondary and Service Road Network from map.osm -->
      <path d="M 370 0 L 420 210 L 480 460" stroke="#cad6c4" stroke-width="16" fill="none"/>
      <path d="M 450 0 L 500 210 L 560 460" stroke="#cad6c4" stroke-width="16" fill="none"/>
      <path d="M 0 160 L 450 170 L 900 190" stroke="#cbd7c5" stroke-width="18" fill="none"/>
      <path d="M 0 320 L 480 310 L 900 330" stroke="#cbd7c5" stroke-width="16" fill="none"/>
      <path d="M 120 60 L 380 90 L 430 200" stroke="#d5dfd0" stroke-width="10" fill="none"/>
      <path d="M 520 220 L 760 260 L 890 280" stroke="#d5dfd0" stroke-width="10" fill="none"/>
      <path d="M 140 260 L 390 290 L 440 330" stroke="#d5dfd0" stroke-width="10" fill="none"/>
      <!-- Civic Zones & Amenities from map.osm -->
      <rect x="540" y="270" width="160" height="90" rx="10" fill="#e8eee3" stroke="#cad6c3"/>
      <text x="550" y="300" font-size="11" font-weight="700" fill="#2b4c3f">State Bank of India</text>
      <text x="550" y="318" font-size="10" fill="#4a6154">Murthal Police Station</text>
      <rect x="180" y="70" width="180" height="90" rx="10" fill="#ede8da" stroke="#d5ccb8"/>
      <text x="190" y="100" font-size="11" font-weight="700" fill="#5c4b32">Murthal Chowk Market</text>
      <text x="190" y="118" font-size="10" fill="#756247">Bus Stop & Transit Hub</text>
      <rect x="110" y="270" width="180" height="110" rx="10" fill="#d4e6cd" stroke="#b5cfa8"/>
      <text x="120" y="305" font-size="11" font-weight="700" fill="#2b4c3f">Community Green Zone</text>
      <text x="120" y="323" font-size="10" fill="#4a6154">Murthal Primary School</text>
      <circle cx="460" cy="210" r="28" fill="#b8c5b2"/>
      <circle cx="460" cy="210" r="14" fill="#9fb996"/>
      <text x="475" y="200" font-size="11" font-weight="700" fill="#123e39">Murthal Rotary Crossing</text>
    `;

    for (const pin of visiblePins) {
      const isSel = selectedPin?.id === pin.id;
      const g = document.createElementNS(svgNS, 'g');
      g.setAttribute('transform', `translate(${pin.x}, ${pin.y})`);
      g.style.cursor = 'pointer';
      const color = pin.status === 'CLOSED' ? '#10b981' : pin.status === 'PENDING_VERIFICATION' ? '#f59e0b' : '#123e39';
      g.innerHTML = `
        ${isSel ? `<circle r="24" fill="${color}" opacity="0.22"/>` : ''}
        <circle r="${isSel ? 15 : 12}" fill="${color}" stroke="#ffffff" stroke-width="2.5"/>
        <text y="4" text-anchor="middle" font-size="10" fill="#ffffff" font-weight="700">${pin.category.slice(0, 1)}</text>
      `;
      g.addEventListener('click', ev => {
        ev.stopPropagation();
        selectedPin = pin;
        renderMapCanvas();
        renderDetail();
      });
      svg.appendChild(g);
    }

    svg.addEventListener('click', ev => {
      const rect = svg.getBoundingClientRect();
      const relX = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width));
      const relY = Math.max(0, Math.min(1, (ev.clientY - rect.top) / rect.height));
      const lat = maxLat - relY * (maxLat - minLat);
      const lon = minLon + relX * (maxLon - minLon);
      selectedPin = {
        id: 'ward-custom',
        title: t('Custom pinned location in Ward 4', 'वार्ड 4 में चुना गया स्थान'),
        category: activeCat === 'ALL' ? 'ROADS' : activeCat,
        status: 'SUBMITTED',
        address: `Pinned Location (${lat.toFixed(4)}° N, ${lon.toFixed(4)}° E), Ward 4`,
        department: 'Civic triage',
        latitude: lat,
        longitude: lon,
        x: Math.round(relX * 900),
        y: Math.round(relY * 460),
      };
      void reverseGeocode(lat, lon, document.documentElement.lang || 'en').then(rev => {
        if (selectedPin.id === 'ward-custom' && rev.address) {
          selectedPin.address = rev.address;
          renderDetail();
        }
      }).catch(() => {});
      renderMapCanvas();
      renderDetail();
    });

    const caption = el('p', 'microcopy', t('Click any pin to inspect the case, or click any street/park on the map to drop a geotag pin and report an issue there.', 'मामले की जाँच करने के लिए किसी भी पिन पर क्लिक करें, या वहाँ समस्या दर्ज करने के लिए मानचित्र पर किसी भी सड़क/पार्क पर क्लिक करें।'));
    canvasCard.append(svg, caption);
  }

  function renderFilters() {
    filterBar.replaceChildren();
    for (const [code, label] of categories) {
      const btn = button(
        label,
        () => {
          activeCat = code;
          renderFilters();
          renderMapCanvas();
        },
        activeCat === code ? 'button small' : 'button secondary small',
      );
      filterBar.append(btn);
    }
  }

  renderFilters();
  renderMapCanvas();
  renderDetail();
  shell.append(canvasCard, detailCard);
  parent.append(filterBar, shell);
}


export async function leaderboard(parent: HTMLElement) {
  heading(
    parent,
    t('CIVIC ACCOUNTABILITY LEADERBOARD', 'नागरिक जवाबदेही लीडरबोर्ड'),
    t('Recognizing genuine reports & decisive action.', 'सच्ची शिकायतों और त्वरित कार्रवाई का सम्मान।'),
    t('Regular citizens earn points when their complaints are verified genuine and approved by authorities (+35 to +85 pts). Authorities earn points for reviewing, resolving, and approving civic issues (+15 to +40 pts).', 'जब नागरिकों की शिकायतें सच्ची पाई जाती हैं और स्वीकृत होती हैं तो उनके अंक बढ़ते हैं। अधिकारियों को समस्याओं के समाधान और सत्यापन पर अंक मिलते हैं।'),
  );

  const filterBar = el('div', 'map-filter-bar');
  const contentWrap = el('div');
  parent.append(filterBar, contentWrap);

  let activeFilter = 'ALL';

  async function loadBoard() {
    filterBar.replaceChildren();
    for (const [code, label] of [
      ['ALL', t('All Ranked (10)', 'सभी प्रतिभागी (10)')],
      ['CITIZEN', t('Regular Citizens', 'नागरिक उपयोगकर्ता')],
      ['AUTHORITY', t('Authorities & Reviewers', 'अधिकारी और समीक्षक')],
    ]) {
      filterBar.append(
        button(
          label,
          () => {
            activeFilter = code;
            void loadBoard();
          },
          activeFilter === code ? 'button small' : 'button secondary small',
        ),
      );
    }

    notice(contentWrap, t('Loading live leaderboard rankings…', 'लीडरबोर्ड रैंकिंग लोड हो रही है…'));
    try {
      const data = await getLeaderboard(activeFilter);
      contentWrap.replaceChildren();

      const stats = el('div', 'stats stats-4');
      const totalApproved = data.entries.reduce((acc, x) => acc + x.complaintsApprovedCount, 0);
      const totalActions = data.entries.reduce((acc, x) => acc + x.actionsTakenCount, 0);
      for (const [label, value] of [
        [t('Ranked Participants', 'कुल प्रतिभागी'), data.totalParticipants],
        [t('Total Civic Points', 'कुल नागरिक अंक'), data.totalPointsAwarded],
        [t('Authority Actions', 'अधिकारी कार्रवाइयाँ'), totalActions],
        [t('Approved Complaints', 'स्वीकृत शिकायतें'), totalApproved],
      ]) {
        const item = el('div');
        item.append(el('span', 'label', String(label)), el('strong', 'stat-value', String(value)));
        stats.append(item);
      }
      contentWrap.append(stats);

      if (data.entries.length >= 3) {
        const podium = el('div', 'leaderboard-podium');
        data.entries.slice(0, 3).forEach((entry, i) => {
          const card = el('div', `panel podium-card podium-rank-${i + 1}`);
          const medal = i === 0 ? '🥇 #1' : i === 1 ? '🥈 #2' : '🥉 #3';
          card.append(
            el('span', 'podium-medal', medal),
            el('h3', '', entry.fullName),
            el('span', `status ${entry.userType === 'AUTHORITY' ? 'status-closed' : 'status-assigned'}`, `${entry.role}${entry.department ? ` · ${entry.department}` : ` · ${entry.ward}`}`),
            el('strong', 'podium-score', `${entry.score} pts`),
            el('p', 'microcopy', entry.badge),
          );
          podium.append(card);
        });
        contentWrap.append(podium);
      }

      const grid = el('div', 'detail-grid');
      const tablePanel = el('section', 'panel');
      tablePanel.append(
        el('h2', '', t('Official Standings', 'आधिकारिक रैंकिंग तालिका')),
        el('p', 'microcopy', t('Sorted by total verified civic score in descending order.', 'कुल सत्यापित नागरिक अंकों के आधार पर अवरोही क्रम में।')),
      );

      const tableWrap = el('div', 'leaderboard-table-wrap');
      const table = el('table', 'leaderboard-table');
      const thead = el('thead');
      const headRow = el('tr');
      for (const h of [
        t('Rank', 'रैंक'),
        t('Participant', 'प्रतिभागी'),
        t('Role & Ward', 'भूमिका और वार्ड'),
        t('Verified Activity', 'सत्यापित गतिविधि'),
        t('Score', 'अंक'),
        t('Quick Test', 'त्वरित परीक्षण'),
      ]) {
        headRow.append(el('th', '', h));
      }
      thead.append(headRow);
      const tbody = el('tbody');

      for (const item of data.entries) {
        const tr = el('tr', identity?.email === item.email ? 'leaderboard-row-self' : '');
        const rankCell = el('td', 'leaderboard-rank-cell', `#${item.rank}`);
        const nameCell = el('td');
        nameCell.append(
          el('strong', '', item.fullName),
          el('small', 'leaderboard-badge-sub', item.badge),
        );
        const roleCell = el('td');
        roleCell.append(
          el('span', `status ${item.userType === 'AUTHORITY' ? 'status-closed' : 'status-assigned'}`, item.role),
          el('small', 'leaderboard-ward-sub', item.department || item.ward),
        );
        const activityText =
          item.userType === 'AUTHORITY'
            ? t(`${item.actionsTakenCount} authority actions`, `${item.actionsTakenCount} आधिकारिक कार्रवाइयाँ`)
            : t(`${item.complaintsApprovedCount}/${item.complaintsSubmittedCount} complaints approved`, `${item.complaintsApprovedCount}/${item.complaintsSubmittedCount} शिकायतें स्वीकृत`);
        const actCell = el('td', 'microcopy', activityText);
        const scoreCell = el('td', 'leaderboard-score-cell', `${item.score} pts`);
        const actionCell = el('td');
        if (identity?.email === item.email) {
          actionCell.append(el('span', 'status status-closed', t('Active user', 'सक्रिय')));
        } else {
          const switchBtn = button(
            t('Instant Sign-In →', 'तुरंत साइन-इन →'),
            () => {
              switchBtn.disabled = true;
              void instantSignIn(item.email)
                .then(() => render())
                .catch(() => { switchBtn.disabled = false; });
            },
            'quiet',
          );
          actionCell.append(switchBtn);
        }
        tr.append(rankCell, nameCell, roleCell, actCell, scoreCell, actionCell);
        tbody.append(tr);
      }

      table.append(thead, tbody);
      tableWrap.append(table);
      tablePanel.append(tableWrap);

      const sidePanel = el('aside', 'panel');
      sidePanel.append(
        el('p', 'eyebrow', t('HOW SCORING WORKS', 'अंक प्रणाली कैसे काम करती है')),
        el('h2', '', t('Transparent Rules', 'पारदर्शी नियम')),
      );
      const rulesList = el('div', 'trust-pillars');
      for (const [rTitle, rBody] of [
        [t('Regular Citizens (+35 to +85 pts)', 'सामान्य नागरिक (+35 से +85 अंक)'), t('+35 pts when a submitted complaint is triaged as genuine; +50 pts bonus when an authority approves closure.', 'सच्ची शिकायत दर्ज करने पर +35 अंक; अधिकारी द्वारा समाधान स्वीकृत होने पर +50 बोनस अंक।')],
        [t('Department Authorities (+15 to +30 pts)', 'विभागीय अधिकारी (+15 से +30 अंक)'), t('+15 pts for starting work, +25 pts for completing tasks, and +30 pts for submitting resolution proof.', 'कार्य शुरू करने पर +15 अंक, कार्य पूरा करने पर +25 अंक, और समाधान प्रस्तुत करने पर +30 अंक।')],
        [t('Independent Reviewers (+40 pts)', 'स्वतंत्र समीक्षक (+40 अंक)'), t('+40 pts for independent verification and closure approval or upholding citizen challenges.', 'स्वतंत्र सत्यापन और समापन अनुमोदन पर +40 अंक।')],
      ]) {
        const rp = el('div', 'trust-pillar');
        rp.append(el('strong', '', rTitle), el('p', 'microcopy', rBody));
        rulesList.append(rp);
      }
      sidePanel.append(rulesList);

      if (data.recentActivity.length) {
        sidePanel.append(el('h3', 'leaderboard-feed-heading', t('Recent Point Awards', 'हाल के अंक पुरस्कार')));
        for (const ev of data.recentActivity.slice(0, 8)) {
          const item = el('div', 'timeline-event');
          item.append(
            el('span', 'label', `+${ev.pointsDelta} PTS · ${ev.userName}`),
            el('p', '', ev.description),
            el('time', 'microcopy', date(ev.createdAt)),
          );
          sidePanel.append(item);
        }
      }

      grid.append(tablePanel, sidePanel);
      contentWrap.append(grid);
      refreshScroll();
    } catch (err) {
      notice(contentWrap, errorMessage(err), true);
    }
  }

  await loadBoard();
}
