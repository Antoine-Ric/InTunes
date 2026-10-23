/* InTunes shared helpers. Loaded on every page. */
window.InTunes = (function () {
  var toastEl = null;
  var toastTimer = null;

  function toast(msg) {
    if (!toastEl) toastEl = document.getElementById('toast');
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.add('on');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove('on'); }, 2600);
  }

  return { toast: toast };
})();
