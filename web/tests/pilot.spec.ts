import {test,expect} from '@playwright/test';
test.beforeEach(async({page})=>{await page.route('**/api/v1/auth/me',route=>route.fulfill({status:401,json:{message:'Sign in to continue.'}}));});
test('citizen navigation, Hindi and mobile layout',async({page})=>{
  await page.goto('/');await expect(page.getByRole('heading',{level:1})).toContainText('A better city');
  await page.getByRole('button',{name:'हिन्दी',exact:true}).click();await expect(page.locator('html')).toHaveAttribute('lang','hi');
  await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  await page.getByRole('button',{name:'English',exact:true}).click();await page.goto('/report');
  await expect(page.getByLabel('Give the issue a short title')).toBeVisible();await expect(page.locator('input[type=checkbox]')).not.toBeChecked();
});
test('theme toggle persists and exposes its selected state',async({page})=>{
  await page.goto('/');const root=page.locator('html');const before=await root.getAttribute('data-theme');
  await page.getByRole('button',{name:/Switch to (dark|light) mode/}).click();
  const selected=await root.getAttribute('data-theme');expect(selected).not.toBe(before);
  await expect(page.locator('.theme-toggle')).toHaveAttribute('aria-pressed',String(selected==='dark'));
  await page.reload();await expect(root).toHaveAttribute('data-theme',selected!);
});
test('failed submission preserves report and never displays success',async({page})=>{
  await page.route('**/api/v1/auth/me',route=>route.fulfill({json:{id:'test-user',role:'CITIZEN',department:null,csrf:'test-csrf'}}));
  await page.route('**/api/v1/complaints',route=>route.fulfill({status:503,json:{message:'Service unavailable. Please retry.'}}));
  await page.goto('/report');await page.getByLabel('Give the issue a short title').fill('Broken street light');await page.getByLabel('What is happening?').fill('The light has been out for several evenings.');await page.getByLabel('Address or landmark').fill('Synthetic Ward A');await page.locator('input[type=checkbox]').check();await page.getByRole('button',{name:'Review & submit report'}).click();
  await expect(page.getByRole('alert')).toContainText('Service unavailable');await expect(page.getByLabel('Give the issue a short title')).toHaveValue('Broken street light');await expect(page).toHaveURL(/report/);
});
test('case text is rendered as text and task actions are scoped',async({page})=>{
  await page.route('**/api/v1/auth/me',route=>route.fulfill({json:{id:'test-user',role:'CITIZEN',department:null,csrf:'test-csrf'}}));
  await page.route('**/api/v1/complaints/test',route=>route.fulfill({json:{case:{id:'test',title:'<img src=x onerror=alert(1)>',description:'Test description',category:'ROADS',address:'Ward A',citizenId:'test-user',department:'Public works',status:'ASSIGNED',dueAt:1800000000000,version:1},tasks:[],events:[]}}));
  await page.goto('/cases/test');await expect(page.getByRole('heading',{level:1})).toContainText('<img');await expect(page.locator('main img')).toHaveCount(0);await expect(page.getByRole('button',{name:'Approve closure'})).toHaveCount(0);
});
