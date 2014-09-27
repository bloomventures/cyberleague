let SessionLoad = 1
if &cp | set nocp | endif
let s:so_save = &so | let s:siso_save = &siso | set so=0 siso=0
let v:this_session=expand("<sfile>:p")
silent only
cd ~/Work/LeanPixel/cyberleague/pog
if expand('%') == '' && !&modified && line('$') <= 1 && getline(1) == ''
  let s:wipebuf = bufnr('%')
endif
set shortmess=aoO
badd +27 project.clj
badd +3 src/clj/pog/core.clj
badd +43 src/cljs/pog/games.cljs
badd +1 test/cljs/pog/games_test.cljs
badd +7 test/clj/pog/core_test.clj
badd +1 src/clj/pog/precompile.clj
argglobal
silent! argdel *
argadd project.clj
edit project.clj
set splitbelow splitright
wincmd _ | wincmd |
vsplit
1wincmd h
wincmd _ | wincmd |
split
1wincmd k
wincmd w
wincmd w
set nosplitbelow
wincmd t
set winheight=1 winwidth=1
exe '1resize ' . ((&lines * 43 + 44) / 88)
exe 'vert 1resize ' . ((&columns * 133 + 133) / 267)
exe '2resize ' . ((&lines * 42 + 44) / 88)
exe 'vert 2resize ' . ((&columns * 133 + 133) / 267)
exe 'vert 3resize ' . ((&columns * 133 + 133) / 267)
argglobal
setlocal fdm=marker
setlocal fde=0
setlocal fmr={{{,}}}
setlocal fdi=#
setlocal fdl=99
setlocal fml=1
setlocal fdn=20
setlocal fen
let s:l = 1 - ((0 * winheight(0) + 21) / 43)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
1
normal! 0
wincmd w
argglobal
edit test/cljs/pog/games_test.cljs
setlocal fdm=marker
setlocal fde=0
setlocal fmr=(,)
setlocal fdi=#
setlocal fdl=99
setlocal fml=1
setlocal fdn=20
setlocal fen
1
normal! zo
2
normal! zo
6
normal! zo
7
normal! zo
8
normal! zo
8
normal! zo
let s:l = 1 - ((0 * winheight(0) + 21) / 42)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
1
normal! 018|
wincmd w
argglobal
edit src/cljs/pog/games.cljs
setlocal fdm=marker
setlocal fde=0
setlocal fmr=(,)
setlocal fdi=#
setlocal fdl=99
setlocal fml=1
setlocal fdn=20
setlocal fen
3
normal! zo
5
normal! zo
6
normal! zo
30
normal! zo
32
normal! zo
36
normal! zo
38
normal! zo
39
normal! zo
41
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
43
normal! zo
43
normal! zo
43
normal! zo
43
normal! zo
43
normal! zo
43
normal! zo
43
normal! zo
45
normal! zo
30
normal! zo
32
normal! zo
36
normal! zo
38
normal! zo
39
normal! zo
41
normal! zo
42
normal! zo
43
normal! zo
43
normal! zo
let s:l = 43 - ((42 * winheight(0) + 43) / 86)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
43
normal! 084|
wincmd w
exe '1resize ' . ((&lines * 43 + 44) / 88)
exe 'vert 1resize ' . ((&columns * 133 + 133) / 267)
exe '2resize ' . ((&lines * 42 + 44) / 88)
exe 'vert 2resize ' . ((&columns * 133 + 133) / 267)
exe 'vert 3resize ' . ((&columns * 133 + 133) / 267)
tabnext 1
if exists('s:wipebuf')
  silent exe 'bwipe ' . s:wipebuf
endif
unlet! s:wipebuf
set winheight=1 winwidth=20 shortmess=filnxtToO
let s:sx = expand("<sfile>:p:r")."x.vim"
if file_readable(s:sx)
  exe "source " . fnameescape(s:sx)
endif
let &so = s:so_save | let &siso = s:siso_save
doautoall SessionLoadPost
let g:this_obsession = v:this_session
unlet SessionLoad
" vim: set ft=vim :
