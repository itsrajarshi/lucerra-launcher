/* Lucerra site motion: Lenis (smooth scroll), GSAP + ScrollTrigger (scroll story),
   PixiJS + pixi-filters (hero background), Anime.js (entrance, micro motion).
   Everything is skipped under prefers-reduced-motion. */
(function () {
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (reduce) return;
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  /* Lenis: smooth scroll, shared with ScrollTrigger when it is present */
  var lenis = null;
  if (window.Lenis) {
    $$('pre').forEach(function (p) { p.setAttribute('data-lenis-prevent', ''); });
    lenis = new Lenis({ lerp: 0.1, anchors: true });
    if (window.gsap && window.ScrollTrigger) {
      lenis.on('scroll', ScrollTrigger.update);
      gsap.ticker.add(function (t) { lenis.raf(t * 1000); });
      gsap.ticker.lagSmoothing(0);
    } else {
      (function raf(t) { lenis.raf(t); requestAnimationFrame(raf); })(0);
    }
  }

  /* Anime.js: hero entrance */
  var A = window.anime;
  var heroEls = $$('.hero > div:first-child > *');
  var film = $('.hero .film');
  if (A && A.animate && heroEls.length) {
    try {
      heroEls.concat(film ? [film] : []).forEach(function (e) { e.style.opacity = 0; });
      A.animate(heroEls, { opacity: [0, 1], translateY: [18, 0], delay: A.stagger(90), duration: 900, ease: 'outExpo' });
      if (film) A.animate(film, { opacity: [0, 1], scale: [0.97, 1], delay: 250, duration: 1100, ease: 'outExpo' });
    } catch (e) { heroEls.concat(film ? [film] : []).forEach(function (el) { el.style.opacity = 1; }); }
  }

  /* GSAP: pinned story. Stock home dissolves into Lucerra, favourites focus one by one, then All Apps. */
  var sec = $('.story');
  if (sec && window.gsap && window.ScrollTrigger) {
    gsap.registerPlugin(ScrollTrigger);
    sec.classList.add('story-on');
    var imgs = $$('.tv img', sec), lines = $$('.line', sec);
    gsap.set(imgs, { opacity: 0 });
    gsap.set(imgs[0], { opacity: 1 });
    gsap.set(lines, { opacity: 0, y: 24 });
    gsap.set(lines[0], { opacity: 1, y: 0 });

    var tl = gsap.timeline({
      defaults: { ease: 'none' },
      scrollTrigger: { trigger: sec, start: 'top top', end: window.innerWidth < 860 ? '+=240%' : '+=320%', scrub: 0.6, pin: $('.story-pin', sec), anticipatePin: 1 }
    });
    // incoming image fades in on top, then the outgoing one is cleared (no ghosting)
    function swap(from, to, at, d) {
      tl.to(imgs[to], { opacity: 1, duration: d }, at).set(imgs[from], { opacity: 0 }, at + d);
    }
    function say(from, to, at) {
      tl.to(lines[from], { opacity: 0, y: -20, duration: 0.5 }, at)
        .fromTo(lines[to], { opacity: 0, y: 24 }, { opacity: 1, y: 0, duration: 0.5 }, at + 0.4);
    }
    tl.to({}, { duration: 1 });                // hold: too much noise
    swap(0, 1, 1, 1); say(0, 1, 1);            // stock -> Lucerra home
    tl.to({}, { duration: 1 });
    say(1, 2, 3.2);                            // five favourites
    swap(1, 2, 3.6, 0.8); swap(2, 3, 4.6, 0.8); swap(3, 4, 5.6, 0.8); swap(4, 5, 6.6, 0.8);
    say(2, 3, 7.8);                            // one press to all apps
    swap(5, 6, 8, 1);
    tl.to({}, { duration: 1 });
  }

  /* PixiJS + pixi-filters: soft drifting glass behind the hero (loaded late, paused offscreen) */
  var host = $('.hero-bg');
  if (host) {
    var load = function (src, cb) { var s = document.createElement('script'); s.src = src; s.onload = cb; document.head.appendChild(s); };
    var start = function () {
      load('vendor/pixi.min.js', function () {
        load('vendor/pixi-filters.js', function () { try { hero(); } catch (e) { /* background is decorative */ } });
      });
    };
    ('requestIdleCallback' in window) ? requestIdleCallback(start, { timeout: 1500 }) : setTimeout(start, 600);
  }

  function hero() {
    var app = new PIXI.Application();
    app.init({ resizeTo: host, backgroundAlpha: 0, antialias: true, autoDensity: true, resolution: Math.min(window.devicePixelRatio || 1, 1.5), powerPreference: 'low-power' }).then(function () {
      host.appendChild(app.canvas);
      var layer = new PIXI.Container();
      layer.filters = [new PIXI.filters.KawaseBlurFilter({ strength: 14, quality: 5 })];
      app.stage.addChild(layer);
      var orbs = [
        { c: 0x6c8cff, a: 0.55, r: 0.24, x: 0.78, y: 0.30, sx: 0.16, sy: 0.10, p: 0 },
        { c: 0x9db4ff, a: 0.50, r: 0.20, x: 0.55, y: 0.75, sx: 0.12, sy: 0.14, p: 1.7 },
        { c: 0x1b2340, a: 0.14, r: 0.18, x: 0.90, y: 0.80, sx: 0.10, sy: 0.12, p: 3.1 },
        { c: 0xc7d3ff, a: 0.65, r: 0.26, x: 0.25, y: 0.20, sx: 0.14, sy: 0.12, p: 4.4 }
      ].map(function (o) {
        var g = new PIXI.Graphics().circle(0, 0, 100).fill({ color: o.c, alpha: o.a });
        layer.addChild(g); o.g = g; return o;
      });
      var mx = 0, my = 0, tx = 0, ty = 0;
      window.addEventListener('pointermove', function (e) { tx = e.clientX / innerWidth - 0.5; ty = e.clientY / innerHeight - 0.5; }, { passive: true });
      app.ticker.maxFPS = 30;
      var t = 0;
      app.ticker.add(function (tk) {
        t += tk.deltaMS / 1000; mx += (tx - mx) * 0.04; my += (ty - my) * 0.04;
        var w = app.screen.width, h = app.screen.height, m = Math.min(w, h * 1.6);
        orbs.forEach(function (o, i) {
          o.g.scale.set(o.r * m / 100);
          o.g.x = w * o.x + Math.sin(t * 0.25 + o.p) * w * o.sx + mx * (30 + i * 14);
          o.g.y = h * o.y + Math.cos(t * 0.21 + o.p) * h * o.sy + my * (30 + i * 14);
        });
      });
      if ('IntersectionObserver' in window) {
        new IntersectionObserver(function (es) { es[0].isIntersecting ? app.ticker.start() : app.ticker.stop(); }).observe(host);
      }
    });
  }
})();
