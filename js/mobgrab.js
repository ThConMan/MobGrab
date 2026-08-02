/* ===========================================================================
 * MobGrab site behaviour.
 *
 * Replaces the shared framework's motion.js: this site no longer uses that
 * framework, so the reveal targets and the mobile nav are wired to MobGrab's
 * own markup instead of being auto-detected.
 * ======================================================================== */
(function () {
    'use strict';

    var reduce = window.matchMedia &&
                 window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function each(sel, root, fn) {
        var n = (root || document).querySelectorAll(sel);
        for (var i = 0; i < n.length; i++) fn(n[i], i);
    }

    /* ── mobile nav ──────────────────────────────────────────────────────── */
    function nav() {
        var burger = document.querySelector('.nav-burger');
        var links = document.getElementById('nav-links');
        if (!burger || !links) return;
        burger.addEventListener('click', function () {
            var open = links.classList.toggle('open');
            burger.classList.toggle('active', open);
            burger.setAttribute('aria-expanded', open ? 'true' : 'false');
        });
        // Tapping a link should close the sheet, not leave it hanging open.
        each('a', links, function (a) {
            a.addEventListener('click', function () {
                links.classList.remove('open');
                burger.classList.remove('active');
                burger.setAttribute('aria-expanded', 'false');
            });
        });
    }

    /* ── scroll progress + nav elevation ─────────────────────────────────── */
    function chrome() {
        var bar = null;
        var navEl = document.querySelector('.nav');

        if (!reduce) {
            bar = document.createElement('div');
            bar.className = 'progress';
            bar.setAttribute('aria-hidden', 'true');
            document.body.appendChild(bar);
        }
        if (!bar && !navEl) return;

        var ticking = false;
        function update() {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(function () {
                if (bar) {
                    var max = document.documentElement.scrollHeight - window.innerHeight;
                    bar.style.setProperty('--p', max > 0 ? window.scrollY / max : 0);
                }
                if (navEl) navEl.classList.toggle('is-stuck', window.scrollY > 12);
                ticking = false;
            });
        }
        window.addEventListener('scroll', update, { passive: true });
        update();
    }

    /* ── hero motes ──────────────────────────────────────────────────────── */
    function motes() {
        var host = document.querySelector('.motes');
        if (!host || reduce) return;
        var n = 16;
        var html = '';
        for (var i = 0; i < n; i++) {
            var left = Math.round((i / n) * 100 + (Math.random() * 5 - 2.5));
            html += '<i style="left:' + Math.max(0, Math.min(100, left)) + '%;' +
                    'top:' + (62 + Math.random() * 34).toFixed(0) + '%;' +
                    '--s:' + (3 + Math.random() * 5).toFixed(1) + 'px;' +
                    '--dur:' + (12 + Math.random() * 12).toFixed(1) + 's;' +
                    '--delay:' + (Math.random() * 14).toFixed(1) + 's;' +
                    '--peak:' + (0.28 + Math.random() * 0.34).toFixed(2) + '"></i>';
        }
        host.innerHTML = html;
    }

    /* ── count-ups ───────────────────────────────────────────────────────── */
    function countUp(el) {
        var target = parseFloat(el.getAttribute('data-count'));
        if (isNaN(target)) return;
        var suffix = el.getAttribute('data-suffix') || '';
        if (reduce || !target) { el.textContent = target + suffix; return; }

        var start = performance.now(), dur = 1000;
        requestAnimationFrame(function step(now) {
            var t = Math.min((now - start) / dur, 1);
            el.textContent = Math.round(target * (1 - Math.pow(1 - t, 3))) + suffix;
            if (t < 1) requestAnimationFrame(step);
        });
    }

    function show(el) {
        el.classList.add('visible');
        each('[data-count]', el, countUp);
    }

    /* ── reveal on scroll ────────────────────────────────────────────────── */
    var TARGETS = ['.section-head', '.statband', '.cta-card', '.docs-section', '.fine-print'];
    var GROUPS = ['.exchange', '.bento', '.steps', '.compat', '.hero-buttons', '.plugin-hero-meta'];

    function reveal() {
        TARGETS.forEach(function (sel) {
            each(sel, document, function (el) { el.classList.add('reveal'); });
        });
        GROUPS.forEach(function (sel) {
            each(sel, document, function (group) {
                each(':scope > *', group, function (child, i) {
                    child.classList.add('reveal');
                    child.setAttribute('data-d', Math.min(i + 1, 5));
                });
            });
        });

        if (reduce || !('IntersectionObserver' in window)) {
            each('.reveal', document, function (el) { el.classList.add('reveal-now'); show(el); });
            return;
        }

        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (e) {
                if (!e.isIntersecting) return;
                show(e.target);
                io.unobserve(e.target);
            });
        }, { rootMargin: '0px 0px -10% 0px', threshold: 0.12 });
        each('.reveal', document, function (el) { io.observe(el); });

        /* Failsafe. Content must never stay hidden because the animation did
         * not run -- which is what happens if the observer never fires, in a
         * context that never paints. .reveal-now drops the transition too,
         * since a transition that never advances leaves opacity at 0. */
        setTimeout(function () {
            each('.reveal:not(.visible)', document, function (el) {
                el.classList.add('reveal-now');
                show(el);
            });
        }, 1600);
    }

    /* ── docs sidebar scroll-spy ─────────────────────────────────────────── */
    function spy() {
        var links = document.querySelectorAll('.docs-toc a[href^="#"]');
        if (!links.length || !('IntersectionObserver' in window)) return;

        var map = {}, targets = [];
        for (var i = 0; i < links.length; i++) {
            var id = links[i].getAttribute('href').slice(1);
            var sec = document.getElementById(id);
            if (!sec) continue;
            map[id] = links[i];
            targets.push(sec);
        }
        if (!targets.length) return;

        var visible = {};
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (e) { visible[e.target.id] = e.isIntersecting; });
            var current = null;
            for (var j = 0; j < targets.length; j++) {
                if (visible[targets[j].id]) { current = targets[j].id; break; }
            }
            for (var id in map) {
                if (Object.prototype.hasOwnProperty.call(map, id)) {
                    map[id].classList.toggle('active', id === current);
                }
            }
        }, { rootMargin: '-88px 0px -55% 0px', threshold: 0 });
        targets.forEach(function (t) { io.observe(t); });
    }

    /* ── latest release from GitHub ──────────────────────────────────────
     * Progressive enhancement only: the badge and the buttons already carry
     * correct values, so a failed or rate-limited request changes nothing. */
    function release() {
        var badge = document.getElementById('version-badge');
        var docsVersion = document.getElementById('docs-version');
        if (!badge && !docsVersion && !document.getElementById('download-btn')) return;

        fetch('https://api.github.com/repos/ThConMan/MobGrab/releases/latest')
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (data) {
                if (!data) return;
                if (data.tag_name && badge) {
                    badge.textContent = 'Paper 26.1.2 – 26.2 • ' + data.tag_name;
                }
                // Keeps the docs reference version in step with the release
                // instead of drifting the next time one ships.
                if (data.tag_name && docsVersion) {
                    docsVersion.textContent = data.tag_name;
                }
                var jar = data.assets && data.assets.filter(function (a) {
                    return a.name && a.name.slice(-4) === '.jar';
                })[0];
                if (jar) {
                    each('#download-btn, #download-btn-2', document, function (a) {
                        a.href = jar.browser_download_url;
                    });
                }
            })
            .catch(function () {});
    }

    function init() {
        nav();
        chrome();
        motes();
        reveal();
        spy();
        release();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
