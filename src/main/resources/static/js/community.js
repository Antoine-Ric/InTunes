/* Community board.
   Live behavior POSTs to /api/communities/{key}/...; when the API is not wired
   yet the buttons fall back to the local demo behavior so the board stays usable. */
(function () {
  var toast = window.InTunes.toast;
  var board = document.getElementById('board');
  if (!board) return;
  var key = board.dataset.communityKey || 'hiphop_deep_90s';

  function api(path, body) {
    return fetch('/api/communities/' + encodeURIComponent(key) + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body || {})
    }).then(function (res) {
      if (!res.ok) throw new Error('api: ' + res.status);
      return res;
    });
  }

  function toggleLike(btn) {
    var count = btn.querySelector('.like-count');
    var n = parseInt(count.textContent, 10) || 0;
    if (btn.classList.contains('liked')) {
      btn.classList.remove('liked');
      count.textContent = n - 1;
    } else {
      btn.classList.add('liked');
      count.textContent = n + 1;
    }
  }

  function wirePost(scope) {
    scope.querySelectorAll('.like-btn').forEach(function (btn) {
      if (btn.dataset.wired) return;
      btn.dataset.wired = '1';
      btn.addEventListener('click', function () {
        var postId = btn.closest('.post').getAttribute('data-post-id');
        if (postId) {
          api('/posts/' + postId + '/like').then(function () { toggleLike(btn); })
            .catch(function () { toggleLike(btn); }); // not wired yet: local toggle
        } else {
          toggleLike(btn);
        }
      });
    });

    scope.querySelectorAll('.reply-toggle').forEach(function (btn) {
      if (btn.dataset.wired) return;
      btn.dataset.wired = '1';
      btn.addEventListener('click', function () {
        var replies = btn.closest('.post').querySelector('.replies');
        if (replies) replies.classList.toggle('on');
      });
    });

    scope.querySelectorAll('.pin-btn').forEach(function (btn) {
      if (btn.dataset.wired) return;
      btn.dataset.wired = '1';
      btn.addEventListener('click', function () {
        var replyId = btn.getAttribute('data-reply-id');
        var label = btn.querySelector('.pin-label') || btn;
        function flip() {
          var pinned = btn.classList.toggle('pinned');
          if (label !== btn) label.textContent = pinned ? 'Pinned ✓' : 'Pin to my profile';
          toast(pinned ? 'Pinned. It now shows on your profile.' : 'Unpinned from your profile.');
        }
        if (replyId) {
          api('/replies/' + replyId + '/pin').then(flip)
            .catch(function () { flip(); }); // not wired yet: local state
        } else {
          flip();
        }
      });
    });
  }
  wirePost(board);

  /* Composer */
  var postBtn = document.getElementById('post-btn');
  var input = document.getElementById('composer-input');
  if (postBtn && input) {
    postBtn.addEventListener('click', function () {
      var text = input.value.trim();
      if (!text) { toast('Write something first. Your people are waiting.'); input.focus(); return; }
      api('/posts', { body: text }).then(function () {
        toast('Posted.');
        location.reload();
      }).catch(function () {
        prependPost(text); // not wired yet: local demo post
      });
    });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') postBtn.click();
    });
  }

  function prependPost(text) {
    var article = document.createElement('article');
    article.className = 'post';
    article.innerHTML =
      '<div class="post-head"><span class="avatar-dot">R</span>' +
      '<div><b>@ricardi (you)</b><span>Just now</span></div></div><p></p>' +
      '<div class="post-actions">' +
      '<button class="act like-btn"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21.2l7.8-7.7 1-1a5.5 5.5 0 0 0 0-7.9z"/></svg><span class="like-count">0</span></button>' +
      '<button class="act reply-toggle"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.4 8.6 8.6 0 0 1-3.8-.9L3 20l1.1-5.4a8.3 8.3 0 0 1-1-4.1A8.4 8.4 0 0 1 11.5 2h.9A8.4 8.4 0 0 1 21 11.5z"/></svg>0 replies</button>' +
      '<button class="act pin-btn"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 3a1 1 0 0 1 .7 1.7L14 7.4V11h3a1 1 0 0 1 .7 1.7l-5 6A1 1 0 0 1 11 18V6.4L8.3 3.7A1 1 0 0 1 9 2h6a1 1 0 0 1 1 1Z" transform="rotate(35 12 12)"/></svg><span class="pin-label">Pin to my profile</span></button>' +
      '</div><div class="replies"></div>';
    article.querySelector('p').textContent = text;
    var list = document.getElementById('post-list');
    list.insertBefore(article, list.firstChild);
    wirePost(article);
    input.value = '';
    toast('Posted to the board.');
  }
})();
