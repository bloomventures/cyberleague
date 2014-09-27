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
badd +15 project.clj
badd +3 src/clj/pog/core.clj
badd +43 src/cljs/pog/games.cljs
badd +6 test/cljs/pog/games_test.clj
badd +7 test/clj/pog/core_test.clj
badd +1 src/clj/pog/precompile.clj
argglobal
silent! argdel *
argadd project.clj
edit test/cljs/pog/games_test.clj
set splitbelow splitright
wincmd _ | wincmd |
vsplit
1wincmd h
wincmd w
set nosplitbelow
wincmd t
set winheight=1 winwidth=1
exe 'vert 1resize ' . ((&columns * 133 + 133) / 267)
exe 'vert 2resize ' . ((&columns * 133 + 133) / 267)
argglobal
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
5
normal! zo
6
normal! zo
7
normal! zo
7
normal! zo
let s:l = 1 - ((0 * winheight(0) + 43) / 86)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
1
normal! 0
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
17
normal! zo
29
normal! zo
31
normal! zo
35
normal! zo
37
normal! zo
38
normal! zo
29
normal! zo
31
normal! zo
35
normal! zo
37
normal! zo
38
normal! zo
40
normal! zo
41
normal! zo
41
normal! zo
41
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
42
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
42
normal! zo
44
normal! zo
let s:l = 2 - ((1 * winheight(0) + 43) / 86)
if s:l < 1 | let s:l = 1 | endif
exe s:l
normal! zt
2
normal! 0
wincmd w
exe 'vert 1resize ' . ((&columns * 133 + 133) / 267)
exe 'vert 2resize ' . ((&columns * 133 + 133) / 267)
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
