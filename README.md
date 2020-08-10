## Running server

for the app:
```
lein repl
(start!)
```



## Tests

`lein test`

or better...

add to `~/.lein/profiles.clj`:
```
{:user {:plugins [[com.jakemccrary/lein-test-refresh "0.24.1"]
                  [venantius/ultra "0.6.0"]]}}
```
then run:
`lein test-refresh`

