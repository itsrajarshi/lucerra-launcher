document.querySelectorAll('pre').forEach(function(pre){
  var b=document.createElement('button');b.type='button';b.textContent='Copy';
  b.addEventListener('click',function(){
    var t=pre.querySelector('code').innerText;
    (navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(function(){b.textContent='Copied';setTimeout(function(){b.textContent='Copy'},1500)},function(){b.textContent='Press Ctrl+C'});
  });
  pre.appendChild(b);
});
(function(){
  if(!('IntersectionObserver' in window))return;
  document.documentElement.classList.add('js');
  var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target)}})},{threshold:.12,rootMargin:'0px 0px -6% 0px'});
  document.querySelectorAll('section .split>*,section .facts>div,section>.wrap>.eyebrow,section>.wrap>h2').forEach(function(el){el.classList.add('rv');io.observe(el)});
})();

(function(){var b=document.getElementById('upicopy');if(!b)return;b.addEventListener('click',function(){var t=document.getElementById('upi').textContent;(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(function(){b.textContent='Copied';setTimeout(function(){b.textContent='Copy ID'},1500)},function(){b.textContent='Select and copy'})})})();

(function(){var h=document.querySelector('header.top'),b=h&&h.querySelector('.menu');if(!b)return;
function set(o){h.classList.toggle('open',o);b.setAttribute('aria-expanded',o)}
b.addEventListener('click',function(){set(!h.classList.contains('open'))});
document.addEventListener('keydown',function(e){if(e.key==='Escape')set(false)});
h.querySelectorAll('nav a').forEach(function(a){a.addEventListener('click',function(){set(false)})})})();

(function(){var f=document.getElementById('fb');if(!f)return;
f.addEventListener('submit',function(e){e.preventDefault();var t=f.elements.title,b=f.elements.body,bad=null;[[t,5],[b,15]].forEach(function(p){var n=p[0].value.trim().length;if(!bad&&n<p[1]){bad=p[0];bad.setCustomValidity(n?'Too short: write at least '+p[1]+' characters (you have '+n+').':'Please fill this in.')}});if(bad){bad.reportValidity();return}var d=new FormData(f),dev=d.get('device');
var body=(d.get('body')||'')+(dev?'\n\nDevice: '+dev:'');
var u='https://github.com/itsrajarshi/lucerra-launcher/issues/new?title='+encodeURIComponent('['+d.get('type')+'] '+d.get('title'))+'&body='+encodeURIComponent(body);
window.open(u,'_blank','noopener')})})();

document.querySelectorAll('#fb input,#fb textarea').forEach(function(i){i.addEventListener('input',function(){i.setCustomValidity('')})});
