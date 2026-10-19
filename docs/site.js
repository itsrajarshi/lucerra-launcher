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
