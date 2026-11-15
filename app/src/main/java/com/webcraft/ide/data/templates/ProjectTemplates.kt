package com.webcraft.ide.data.templates

object ProjectTemplates {

    val blankHtml = ProjectTemplate(
        id = "blank-html",
        name = "Blank HTML",
        description = "Empty HTML5 boilerplate",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Document</title>
</head>
<body>

</body>
</html>
"""),
            TemplateFile("style.css", "/* Custom styles */\n"),
            TemplateFile("script.js", "// Custom scripts\n")
        )
    )

    val portfolio = ProjectTemplate(
        id = "portfolio",
        name = "Portfolio",
        description = "Personal portfolio with hero, projects and contact",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>My Portfolio</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header>
    <h1>John Doe</h1>
    <p>Frontend Developer & Designer</p>
  </header>

  <section class="hero">
    <p>Hi, I build accessible and performant web experiences.</p>
    <a href="#projects" class="btn">View Projects</a>
  </section>

  <section id="projects" class="projects">
    <h2>Featured Projects</h2>
    <div class="card">
      <h3>Project One</h3>
      <p>A modern web app built with vanilla JS.</p>
    </div>
    <div class="card">
      <h3>Project Two</h3>
      <p>A responsive dashboard using CSS Grid.</p>
    </div>
  </section>

  <footer>
    <p>&copy; 2026 John Doe</p>
  </footer>
  <script src="script.js"></script>
</body>
</html>
"""),
            TemplateFile("style.css", """* { box-sizing: border-box; }
body { font-family: system-ui, sans-serif; margin: 0; background:#0f1114; color:#e8e8e8; }
header { padding: 3rem 1rem; text-align:center; background:#1a1e24; }
header h1 { margin:0 0 .5rem; font-size:2.5rem; }
.hero { padding: 4rem 1rem; text-align:center; }
.btn { display:inline-block; margin-top:1rem; padding:.8rem 1.6rem;
       background:#7aa2ff; color:#0f1114; text-decoration:none; border-radius:8px; font-weight:600; }
.projects { padding: 2rem 1rem; }
.card { background:#1a1e24; padding:1.5rem; margin:1rem 0; border-radius:12px; }
footer { padding:2rem 1rem; text-align:center; color:#8b93a1; }
"""),
            TemplateFile("script.js", """// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(a => {
  a.addEventListener('click', e => {
    e.preventDefault();
    document.querySelector(a.getAttribute('href'))?.scrollIntoView({behavior:'smooth'});
  });
});
""")
        )
    )

    val landingPage = ProjectTemplate(
        id = "landing",
        name = "Landing Page",
        description = "Product landing page with CTA, features and pricing",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Product</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <nav>
    <span class="logo">Product</span>
    <a href="#pricing" class="cta">Get Started</a>
  </nav>

  <section class="hero">
    <h1>Ship faster with Product</h1>
    <p>The modern toolkit for teams who build for the web.</p>
    <a href="#pricing" class="cta">Start Free</a>
  </section>

  <section class="features">
    <div class="feature"><h3>Fast</h3><p>Sub-100ms interactions.</p></div>
    <div class="feature"><h3>Secure</h3><p>SOC2 compliant infrastructure.</p></div>
    <div class="feature"><h3>Scalable</h3><p>From prototype to production.</p></div>
  </section>

  <section id="pricing" class="pricing">
    <h2>Simple pricing</h2>
    <div class="plan"><h3>Free</h3><p>$0 / mo</p><ul><li>1 project</li><li>Community support</li></ul></div>
    <div class="plan"><h3>Pro</h3><p>$12 / mo</p><ul><li>Unlimited projects</li><li>Priority support</li></ul></div>
  </section>

  <footer><p>&copy; 2026 Product Inc.</p></footer>
</body>
</html>
"""),
            TemplateFile("style.css", """* { box-sizing:border-box; }
body { margin:0; font-family:system-ui,sans-serif; background:#0b0d10; color:#e8e8e8; }
nav { display:flex; justify-content:space-between; align-items:center;
      padding:1rem 1.5rem; border-bottom:1px solid #22272f; }
.logo { font-weight:700; font-size:1.2rem; }
.cta { background:#7aa2ff; color:#0b0d10; padding:.6rem 1.2rem; border-radius:8px;
       text-decoration:none; font-weight:600; }
.hero { padding:5rem 1.5rem; text-align:center; }
.hero h1 { font-size:clamp(2rem,5vw,3.5rem); margin:0 0 1rem; }
.features { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr));
            gap:1rem; padding:2rem 1.5rem; }
.feature { background:#141920; padding:1.5rem; border-radius:12px; }
.pricing { padding:3rem 1.5rem; text-align:center; }
.plan { display:inline-block; background:#141920; padding:2rem; margin:1rem;
        border-radius:12px; width:min(300px,90%); vertical-align:top; }
.plan ul { list-style:none; padding:0; color:#9aa4b0; }
footer { padding:2rem; text-align:center; color:#8b93a1; }
""")
        )
    )

    val personalBlog = ProjectTemplate(
        id = "blog",
        name = "Personal Blog",
        description = "Minimal blog with article list and posts",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>My Blog</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header>
    <h1>My Blog</h1>
    <p>Notes on design and code</p>
  </header>
  <main>
    <article>
      <h2><a href="post1.html">Getting Started with CSS Grid</a></h2>
      <time datetime="2026-11-10">Nov 10, 2026</time>
      <p>CSS Grid changed layout forever. Here's how to get started with the basics...</p>
    </article>
    <article>
      <h2><a href="post2.html">Why I Still Write Vanilla JS</a></h2>
      <time datetime="2026-10-02">Oct 2, 2026</time>
      <p>Frameworks come and go, but the platform remains. Here's why plain JavaScript still wins...</p>
    </article>
  </main>
  <footer><p>&copy; 2026 My Blog</p></footer>
</body>
</html>
"""),
            TemplateFile("post1.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Getting Started with CSS Grid</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header><h1>Getting Started with CSS Grid</h1></header>
  <main>
    <p>CSS Grid lets you build two-dimensional layouts with a fraction of the code of flexbox or floats.</p>
    <pre><code>.container {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 1rem;
}</code></pre>
  </main>
  <footer><a href="index.html">&larr; Back</a></footer>
</body>
</html>
"""),
            TemplateFile("post2.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Why I Still Write Vanilla JS</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <header><h1>Why I Still Write Vanilla JS</h1></header>
  <main>
    <p>The web platform has matured. Custom elements, shadow DOM, and modules cover most use cases.</p>
  </main>
  <footer><a href="index.html">&larr; Back</a></footer>
</body>
</html>
"""),
            TemplateFile("style.css", """body { font-family:Georgia,serif; max-width:680px; margin:0 auto; padding:2rem 1rem;
        background:#fafafa; color:#222; line-height:1.7; }
header h1 { margin:0 0 .25rem; }
a { color:#1a5fd0; text-decoration:none; }
article { margin:2rem 0; padding-bottom:2rem; border-bottom:1px solid #e0e0e0; }
time { display:block; color:#777; font-size:.85rem; margin-bottom:.5rem; }
pre { background:#f0f0f0; padding:1rem; overflow-x:auto; border-radius:8px; }
code { font-family:monospace; font-size:.9em; }
""")
        )
    )

    val dashboard = ProjectTemplate(
        id = "dashboard",
        name = "Dashboard",
        description = "Analytics dashboard with cards, charts and stats",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Dashboard</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <aside class="sidebar">
    <h2>Dashboard</h2>
    <nav>
      <a href="#" class="active">Overview</a>
      <a href="#">Analytics</a>
      <a href="#">Reports</a>
      <a href="#">Settings</a>
    </nav>
  </aside>

  <main class="main">
    <h1>Overview</h1>
    <div class="stats">
      <div class="stat"><span class="label">Visitors</span><span class="value">12,483</span><span class="delta up">+8%</span></div>
      <div class="stat"><span class="label">Revenue</span><span class="value">$48,220</span><span class="delta up">+14%</span></div>
      <div class="stat"><span class="label">Bounce</span><span class="value">32%</span><span class="delta down">-2%</span></div>
    </div>
    <div class="chart">
      <h3>Traffic — last 30 days</h3>
      <svg viewBox="0 0 600 200" preserveAspectRatio="none">
        <polyline fill="none" stroke="#7aa2ff" stroke-width="3"
          points="0,180 40,160 80,165 120,140 160,150 200,120 240,130 280,100 320,110 360,80 400,90 440,60 480,70 520,40 560,50 600,20"/>
      </svg>
    </div>
  </main>
  <script src="script.js"></script>
</body>
</html>
"""),
            TemplateFile("style.css", """* { box-sizing:border-box; }
body { margin:0; font-family:system-ui,sans-serif; background:#0b0d10; color:#e8e8e8;
        display:grid; grid-template-columns:220px 1fr; min-height:100vh; }
.sidebar { background:#101318; padding:1.5rem 1rem; border-right:1px solid #22272f; }
.sidebar nav { display:flex; flex-direction:column; gap:.25rem; }
.sidebar a { padding:.6rem .8rem; color:#9aa4b0; text-decoration:none; border-radius:6px; }
.sidebar a.active, .sidebar a:hover { background:#1a1e24; color:#e8e8e8; }
.main { padding:2rem; }
.stats { display:grid; grid-template-columns:repeat(auto-fit,minmax(200px,1fr)); gap:1rem; margin-bottom:2rem; }
.stat { background:#141920; padding:1.25rem; border-radius:12px; display:flex; flex-direction:column; gap:.25rem; }
.stat .label { color:#8b93a1; font-size:.85rem; }
.stat .value { font-size:1.75rem; font-weight:700; }
.delta.up { color:#06d6a0; font-size:.85rem; }
.delta.down { color:#ff7b54; font-size:.85rem; }
.chart { background:#141920; padding:1.5rem; border-radius:12px; }
.chart svg { width:100%; height:200px; }
@media (max-width:720px) { body { grid-template-columns:1fr; } .sidebar { border-right:none; border-bottom:1px solid #22272f; } }
"""),
            TemplateFile("script.js", """// Animate stat deltas on load
document.querySelectorAll('.stat').forEach(stat => {
  stat.style.opacity = '0';
  stat.style.transform = 'translateY(10px)';
  requestAnimationFrame(() => {
    stat.style.transition = 'opacity .4s, transform .4s';
    stat.style.opacity = '1';
    stat.style.transform = 'none';
  });
});
""")
        )
    )

    val productShowcase = ProjectTemplate(
        id = "showcase",
        name = "Product Showcase",
        description = "E-commerce product page with gallery and specs",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Aurora Headphones</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <nav><span class="brand">Aurora</span></nav>

  <main class="product">
    <div class="gallery">
      <div class="img" style="background:linear-gradient(135deg,#7aa2ff,#4ecdc4)"></div>
    </div>

    <div class="info">
      <h1>Aurora Pro</h1>
      <p class="price">$249</p>
      <p class="desc">Studio-grade wireless headphones with active noise cancellation and 40-hour battery life.</p>

      <ul class="specs">
        <li>Active noise cancellation</li>
        <li>40h battery life</li>
        <li>USB-C fast charge</li>
        <li>Bluetooth 5.3</li>
      </ul>

      <button class="buy">Add to Cart</button>
    </div>
  </main>

  <script src="script.js"></script>
</body>
</html>
"""),
            TemplateFile("style.css", """* { box-sizing:border-box; }
body { margin:0; font-family:system-ui,sans-serif; background:#fff; color:#111; }
nav { padding:1rem 1.5rem; border-bottom:1px solid #eee; }
.brand { font-weight:700; font-size:1.2rem; letter-spacing:.05em; }
.product { display:grid; grid-template-columns:1fr 1fr; gap:2rem; padding:3rem; max-width:1000px; margin:0 auto; }
@media (max-width:720px) { .product { grid-template-columns:1fr; padding:1.5rem; } }
.gallery .img { aspect-ratio:1; border-radius:16px; }
h1 { font-size:2.25rem; margin:0 0 .5rem; }
.price { font-size:1.5rem; font-weight:600; color:#1a5fd0; margin:.5rem 0; }
.desc { color:#555; line-height:1.6; }
.specs { list-style:none; padding:0; margin:1.5rem 0; display:grid; gap:.5rem; }
.specs li::before { content:"✓"; color:#06d6a0; margin-right:.5rem; }
.buy { background:#111; color:#fff; border:none; padding:.9rem 2rem; border-radius:10px;
       font-size:1rem; font-weight:600; cursor:pointer; width:100%; }
.buy:active { transform:scale(.98); }
"""),
            TemplateFile("script.js", """document.querySelector('.buy').addEventListener('click', () => {
  const btn = document.querySelector('.buy');
  btn.textContent = 'Added ✓';
  setTimeout(() => btn.textContent = 'Add to Cart', 1500);
});
""")
        )
    )

    val documentation = ProjectTemplate(
        id = "docs",
        name = "Documentation Website",
        description = "Technical docs with sidebar navigation and code samples",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Project Docs</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <aside class="sidebar">
    <h2>Docs</h2>
    <nav>
      <a href="#intro" class="active">Introduction</a>
      <a href="#install">Installation</a>
      <a href="#usage">Usage</a>
      <a href="#api">API Reference</a>
    </nav>
  </aside>

  <main>
    <section id="intro">
      <h1>Introduction</h1>
      <p>Welcome to the documentation. This site covers installation, usage, and the full API.</p>
    </section>

    <section id="install">
      <h2>Installation</h2>
      <pre><code>npm install my-package</code></pre>
    </section>

    <section id="usage">
      <h2>Usage</h2>
      <pre><code>import { init } from 'my-package';
init({ debug: true });</code></pre>
    </section>

    <section id="api">
      <h2>API Reference</h2>
      <table>
        <thead><tr><th>Method</th><th>Returns</th><th>Description</th></tr></thead>
        <tbody>
          <tr><td><code>init(opts)</code></td><td>void</td><td>Initialize the library</td></tr>
          <tr><td><code>destroy()</code></td><td>void</td><td>Clean up resources</td></tr>
        </tbody>
      </table>
    </section>
  </main>
  <script src="script.js"></script>
</body>
</html>
"""),
            TemplateFile("style.css", """* { box-sizing:border-box; }
body { margin:0; font-family:system-ui,sans-serif; display:grid;
        grid-template-columns:240px 1fr; min-height:100vh; }
.sidebar { background:#0f1114; color:#e8e8e8; padding:1.5rem 1rem; }
.sidebar h2 { margin:0 0 1rem; }
.sidebar a { display:block; padding:.5rem .75rem; color:#9aa4b0; text-decoration:none; border-radius:6px; }
.sidebar a.active, .sidebar a:hover { background:#1a1e24; color:#fff; }
main { padding:3rem; max-width:820px; }
pre { background:#f0f2f5; padding:1rem; border-radius:8px; overflow-x:auto; }
code { font-family:monospace; }
table { width:100%; border-collapse:collapse; }
th, td { text-align:left; padding:.6rem .8rem; border-bottom:1px solid #e5e5e5; }
th { background:#f7f7f7; }
@media (max-width:720px) { body { grid-template-columns:1fr; } .sidebar { padding:1rem; } }
"""),
            TemplateFile("script.js", """// Highlight active section in sidebar
const links = document.querySelectorAll('.sidebar a');
const sections = [...document.querySelectorAll('main section')];

const observer = new IntersectionObserver(entries => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      links.forEach(l => l.classList.toggle('active', l.getAttribute('href') === '#' + e.target.id));
    }
  });
}, { rootMargin: '-20% 0px -70% 0px' });
sections.forEach(s => observer.observe(s));
""")
        )
    )

    val cssPlayground = ProjectTemplate(
        id = "css-playground",
        name = "CSS Animation Playground",
        description = "Interactive CSS animation demos with live controls",
        files = listOf(
            TemplateFile("index.html", """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CSS Animation Playground</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <h1>CSS Animation Playground</h1>

  <div class="controls">
    <label>Duration: <output id="durOut">2s</output>
      <input type="range" id="dur" min="0.5" max="5" step="0.1" value="2">
    </label>
    <label>Easing:
      <select id="easing">
        <option value="ease">ease</option>
        <option value="linear">linear</option>
        <option value="ease-in-out">ease-in-out</option>
        <option value="cubic-bezier(0.68,-0.55,0.27,1.55)">bounce</option>
      </select>
    </label>
  </div>

  <div class="stage">
    <div class="box" id="box"></div>
  </div>

  <script src="script.js"></script>
</body>
</html>
"""),
            TemplateFile("style.css", """body { font-family:system-ui,sans-serif; background:#0f1114; color:#e8e8e8;
        text-align:center; padding:2rem 1rem; }
h1 { font-size:1.75rem; }
.controls { display:flex; gap:1.5rem; justify-content:center; flex-wrap:wrap;
            margin:1.5rem 0; align-items:center; }
label { display:flex; align-items:center; gap:.5rem; }
input[type=range], select { accent-color:#7aa2ff; }
.stage { height:240px; display:flex; align-items:center; justify-content:center;
          border:1px dashed #2b3138; border-radius:12px; margin:1rem auto; max-width:600px; }
.box { width:80px; height:80px; border-radius:16px;
        background:linear-gradient(135deg,#7aa2ff,#4ecdc4);
        animation: slide 2s ease infinite alternate; }
@keyframes slide {
  from { transform: translateX(-140px) rotate(0deg); }
  to   { transform: translateX(140px) rotate(360deg); }
}
"""),
            TemplateFile("script.js", """const box = document.getElementById('box');
const dur = document.getElementById('dur');
const durOut = document.getElementById('durOut');
const easing = document.getElementById('easing');

function apply() {
  box.style.animationDuration = dur.value + 's';
  box.style.animationTimingFunction = easing.value;
  durOut.textContent = dur.value + 's';
}
dur.addEventListener('input', apply);
easing.addEventListener('change', apply);
apply();
""")
        )
    )

    val all: List<ProjectTemplate> = listOf(
        blankHtml, portfolio, landingPage, personalBlog,
        dashboard, productShowcase, documentation, cssPlayground
    )

    fun byName(id: String): ProjectTemplate? = all.find { it.id == id }
}
