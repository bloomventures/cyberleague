## Set up datomic for local development

  $ VERSION=0.9.4899
  $ wget --http-user=james@leanpixel.com \
         --http-password=***REMOVED*** \
         https://my.datomic.com/repo/com/datomic/datomic-pro/$VERSION/datomic-pro-$VERSION.zip \
         -O datomic-pro-$VERSION.zip

Extract the database, copy the sql transactor template from
flexrm/datomic-config & run `bin/transactor <path to copied tempalte>`.
