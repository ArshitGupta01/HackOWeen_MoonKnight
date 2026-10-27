import ngrok from '@ngrok/ngrok';
import fs from 'node:fs';
import path from 'node:path';

function loadAuthToken() {
  if (process.env.NGROK_AUTHTOKEN) return process.env.NGROK_AUTHTOKEN.trim();
  const envPaths = [path.resolve(process.cwd(), '.env'), path.resolve(process.cwd(), '..', '.env')];
  for (const p of envPaths) {
    if (fs.existsSync(p)) {
      const lines = fs.readFileSync(p, 'utf8').split(/\r?\n/);
      for (const line of lines) {
        const match = line.match(/^\s*NGROK_AUTHTOKEN\s*=\s*(.+)\s*$/);
        if (match) {
          return match[1].trim().replace(/^['"]|['"]$/g, '');
        }
      }
    }
  }
  return '';
}

const port = Number(process.env.NGROK_PORT || 5173);
const authtoken = loadAuthToken();

if (!authtoken) {
  console.error('Error: Missing NGROK_AUTHTOKEN in environment or .env file.');
  process.exit(1);
}

const listener = await ngrok.forward({
  addr: port,
  authtoken,
});

const url = listener.url();
fs.writeFileSync(path.resolve(process.cwd(), 'ngrok-url.txt'), url, 'utf8');

console.log('==================================================================');
console.log('  TheekHai Live Ngrok Deployment Active (@ngrok/ngrok SDK)');
console.log(`  Public HTTPS URL : ${url}`);
console.log(`  Web App          : ${url}/`);
console.log(`  Backend REST API : ${url}/api/v1`);
console.log(`  Health Endpoint  : ${url}/health`);
console.log('==================================================================');

process.stdin.resume();
