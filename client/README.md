## Set up datomic for local development

  $ VERSION=0.9.4899
  $ wget --http-user=james@leanpixel.com \
         --http-password=***REMOVED*** \
         https://my.datomic.com/repo/com/datomic/datomic-pro/$VERSION/datomic-pro-$VERSION.zip \
         -O datomic-pro-$VERSION.zip

Extract the database, copy the sql transactor template from
flexrm/datomic-config & run `bin/transactor <path to copied tempalte>`.


## Using datomic free



  $ (binding [db/*uri* "datomic:free://localhost:4334/cldev"] (wrap-reload e))

## Running server

  $ lein quickie
  $ lein cljsbuild auto
  $ lein lesscss auto

  $ lein repl
  => (run-server app {:port 8080})



  lein with-profile production cljsbuild once


  lein with-profile production repl




