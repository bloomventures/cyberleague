## Running server

for tests:
```
lein quickie
```

for cljs:
```
lein cljsbuild auto
```

for css:
```
lein lesscss auto
```

for app:
```
lein repl

then:
```
(require 'cyberleague.server.seed)
(cyberleague.server.seed/seed!)
(start-server! 8080)
```
