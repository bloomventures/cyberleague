## Set up datomic for local development

  $ VERSION=0.9.4899
  $ wget --http-user=james@leanpixel.com \
         --http-password=eb6ce9b2-92f7-4164-86c3-49e60732bba9 \
         https://my.datomic.com/repo/com/datomic/datomic-pro/$VERSION/datomic-pro-$VERSION.zip \
         -O datomic-pro-$VERSION.zip

Extract the database, copy the sql transactor template from
flexrm/datomic-config & run `bin/transactor <path to copied tempalte>`.


## Using datomic free



  (binding [db/*uri* "datomic:free://localhost:4334/cldev"] ... )

## Port forward

  $ ./pf.sh 8080

to clear:

  $ ./pf.sh


## Running server

  $ lein quickie
  $ lein cljsbuild auto
  $ lein lesscss auto

  $ lein repl
  => (run-server app {:port 8080})



  lein with-profile production cljsbuild once


  lein with-profile production repl




