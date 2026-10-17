/* Matchup arena.
   Live mode: votes POST to /api/matchups/vote, then the page reloads for the next pair.
   Demo mode (no pair in the model, or the API is not wired yet): runs the local
   6-round demo so the page stays playable. */
(function () {
  var toast = window.InTunes.toast;
  var versus = document.getElementById('versus');
  if (!versus) return;

  var mode = versus.dataset.mode || 'demo'; // 'demo' | 'live'
  var genre = versus.dataset.genre || 'hiphop';

  var PLAY_SVG = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
  var PAUSE_SVG = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';

  function stopAllPlayback() {
    document.querySelectorAll('.song').forEach(function (s) {
      s.classList.remove('playing');
      var a = s.querySelector('audio');
      if (a) a.pause();
    });
    document.querySelectorAll('[data-play]').forEach(function (b) { b.innerHTML = PLAY_SVG; });
  }

  /* 30s preview: real <audio> when the model has a previewUrl, visual demo otherwise. */
  document.querySelectorAll('[data-play]').forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      var card = document.getElementById('song-' + btn.getAttribute('data-play'));
      var audio = card.querySelector('audio');
      var wasPlaying = card.classList.contains('playing');
      stopAllPlayback();
      if (audio && audio.getAttribute('src')) {
        if (!wasPlaying) {
          audio.play().catch(function () { toast('Preview could not play.'); });
          card.classList.add('playing');
          btn.innerHTML = PAUSE_SVG;
          audio.onended = stopAllPlayback;
        }
      } else if (!wasPlaying) {
        card.classList.add('playing');
        btn.innerHTML = PAUSE_SVG;
        toast('Playing 30-second preview (iTunes preview in the real build).');
      }
    });
  });

  /* ---------- Pick handling ---------- */
  function pick(side) {
    if (mode === 'live') postVote(side);
    else demoPick(side);
  }

  function postVote(side) {
    var winnerEl = document.getElementById('song-' + side);
    var loserEl = document.getElementById('song-' + (side === 'a' ? 'b' : 'a'));
    var winnerId = winnerEl.getAttribute('data-song-id');
    var loserId = loserEl.getAttribute('data-song-id');
    winnerEl.classList.add('picked');
    loserEl.classList.add('faded');
    fetch('/api/matchups/vote', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ winnerSongId: Number(winnerId), loserSongId: Number(loserId), genre: genre })
    }).then(function (res) {
      if (!res.ok) throw new Error('vote failed: ' + res.status);
      location.reload(); // server renders the next pair
    }).catch(function () {
      toast('Live arena is not wired yet. Running demo rounds instead.');
      mode = 'demo';
      resetArena();
    });
  }

  document.querySelectorAll('[data-pick]').forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      pick(btn.getAttribute('data-pick'));
    });
  });
  ['a', 'b'].forEach(function (side) {
    var card = document.getElementById('song-' + side);
    if (card) card.addEventListener('click', function () { pick(side); });
  });

  /* ---------- Demo mode (prototype behavior) ---------- */
  var pairs = [
    { a: { t: 'Electric Relaxation', ar: 'A Tribe Called Quest', y: 1993, pop: 'Deep cut', c: 'c2', lean: '90s · underground' },
      b: { t: "God's Plan", ar: 'Drake', y: 2018, pop: 'Chart hit', c: 'c1', lean: '2010s · mainstream' } },
    { a: { t: 'Doo Wop (That Thing)', ar: 'Lauryn Hill', y: 1998, pop: 'Chart hit', c: 'c3', lean: '90s · mainstream' },
      b: { t: 'Sicko Mode', ar: 'Travis Scott', y: 2018, pop: 'Chart hit', c: 'c4', lean: '2010s · mainstream' } },
    { a: { t: 'N.Y. State of Mind', ar: 'Nas', y: 1994, pop: 'Deep cut', c: 'c5', lean: '90s · underground' },
      b: { t: 'No Role Modelz', ar: 'J. Cole', y: 2014, pop: 'Chart hit', c: 'c6', lean: '2010s · mainstream' } },
    { a: { t: 'Ms. Jackson', ar: 'Outkast', y: 2000, pop: 'Chart hit', c: 'c1', lean: '2000s · mainstream' },
      b: { t: 'Get Ur Freak On', ar: 'Missy Elliott', y: 2001, pop: 'Chart hit', c: 'c2', lean: '2000s · mainstream' } },
    { a: { t: 'Alright', ar: 'Kendrick Lamar', y: 2015, pop: 'Chart hit', c: 'c4', lean: '2010s · mainstream' },
      b: { t: 'Hard Knock Life', ar: 'Jay-Z', y: 1998, pop: 'Chart hit', c: 'c3', lean: '90s · mainstream' } },
    { a: { t: 'Scenario', ar: 'A Tribe Called Quest', y: 1991, pop: 'Deep cut', c: 'c2', lean: '90s · underground' },
      b: { t: 'HUMBLE.', ar: 'Kendrick Lamar', y: 2017, pop: 'Chart hit', c: 'c1', lean: '2010s · mainstream' } }
  ];
  var round = 0, picks = [], busy = false;

  function fillSong(side, s) {
    document.getElementById('title-' + side).textContent = s.t;
    document.getElementById('artist-' + side).textContent = s.ar;
    document.getElementById('yr-' + side).textContent = s.y;
    document.getElementById('pop-' + side).textContent = s.pop.toUpperCase();
    document.getElementById('cover-' + side).className = 'cover ' + s.c;
  }
  function loadPair() {
    var p = pairs[round];
    fillSong('a', p.a); fillSong('b', p.b);
    document.getElementById('round-num').textContent = round + 1;
    document.getElementById('round-bar').style.width = (round / pairs.length * 100 + 8) + '%';
    ['a', 'b'].forEach(function (s) {
      document.getElementById('song-' + s).classList.remove('picked', 'faded', 'playing');
    });
  }
  function updateLean() {
    var el = document.getElementById('lean-text');
    if (!el) return;
    if (!picks.length) { el.textContent = 'too early to call'; return; }
    var counts = {};
    picks.forEach(function (l) { counts[l] = (counts[l] || 0) + 1; });
    el.textContent = Object.keys(counts).sort(function (x, y) { return counts[y] - counts[x]; })[0];
  }
  function finishDemo() {
    document.getElementById('round-num').textContent = pairs.length;
    document.getElementById('round-bar').style.width = '100%';
    document.getElementById('reveal-box').classList.add('on');
    document.getElementById('reveal-text').textContent =
      'Your picks leaned ' + document.getElementById('lean-text').textContent +
      ', the Elo engine has ranked all 25 songs and picked your archetype.';
    toast('Run complete. Your music profile is ready.');
  }
  function demoPick(side) {
    if (busy || round >= pairs.length) return;
    busy = true;
    var other = side === 'a' ? 'b' : 'a';
    document.getElementById('song-' + side).classList.add('picked');
    document.getElementById('song-' + other).classList.add('faded');
    picks.push(pairs[round][side].lean);
    updateLean();
    setTimeout(function () {
      busy = false;
      round++;
      if (round >= pairs.length) finishDemo();
      else loadPair();
    }, 480);
  }
  function resetArena() {
    round = 0; picks = []; busy = false;
    var box = document.getElementById('reveal-box');
    if (box) box.classList.remove('on');
    updateLean();
    loadPair();
  }

  var skipBtn = document.getElementById('skip-btn');
  if (skipBtn) skipBtn.addEventListener('click', function () {
    if (mode !== 'demo' || busy || round >= pairs.length) return;
    round++;
    if (round >= pairs.length) finishDemo();
    else loadPair();
  });

  if (mode === 'demo') resetArena();
})();
