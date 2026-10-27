# Web design quality gate

The browser-native website is implemented in `web/`; native Android and Desktop interfaces use Compose in `app/shared/`. Design for citizens reporting local problems and staff resolving them. Keep the civic direction: warm neutral light surfaces, deep teal, restrained amber, readable editorial typography, and clear status semantics. Web dark mode persists the user's choice and follows the system until overridden. Native dark mode follows the system and provides an in-app override for the current app session. Both modes must preserve readable contrast and semantic colors; shared web colors belong in CSS tokens where practical.

- Make the real case workflow the product demonstration: reporting, duplicate suggestions, task ownership, due dates, evidence, review, and timeline. Do not use fake testimonials, fabricated counters, or decorative dashboard statistics.
- Avoid generic purple/black templates, glowing orbs, aggressive gradients, emoji icons, repetitive feature cards, heavy glass effects, and stock SaaS copy. Use animation only when it clarifies action or feedback; honor reduced-motion preferences.
- Design citizen, department, and reviewer pages for mobile and desktop. Use semantic HTML, visible focus, accessible contrast, understandable form errors, and usable loading, empty, failure, and offline states.
- Include a custom 404 page. Publish real privacy and terms pages before public launch. Show a cookie choice only if non-essential cookies are used; do not claim a consent banner proves compliance.
- Review the working interface at common phone and desktop widths and inspect actual interactive states before calling the UI complete.

These are the applicable quality checks from the supplied design reference, not a mandate to import a specific UI library or copy its examples.
