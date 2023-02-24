# lupapiste-toj

Lupapisteen tiedonohjausjärjestelmä (TOJ) / sähköinen arkistonmuodostusssuunnitelma (eAMS)

## Installation

## Usage

Run it like this:

    $ java -jar lupapiste-toj.jar [args]

## Setup environment

### 1. MongoDB

Choose option 1 or 2 (whatever is easier for you).

#### Option 1: Use Mongo DB without authentication

Install MongoDB 5.x and create a database called `toj`. Your MongoDB should not enforce authentication.

See `config.edn` for the default configuration. Currently there are no facilities for overriding the settings in
`config.edn` in an another file, so if you change the config, make sure not to accidentally commit the changes.

#### Option 2: Use Lupapiste docker-compose setup for MongoDB (or any other setup with authentication)

Lupapiste MongoDB container requires authentication, while in this project MongoDB is accessed by default
without authentication.

As a workaround, modify connection strings in `lupapiste-toj.test-helpers` and in `config.edn` as follows:

```
mongodb://<mongo user name>:<mongo user pwd>@localhost/<default database>?authSource=<db where users are stored>
```

You can use existing lupapiste user for authentication: It has sufficient access rights by default. If you do so, check
username and password from `user.properties` file in lupapiste repository root and use `lupapiste` as authSource. 
In the end, connection string should be something like this: `mongodb://user:pwd@localhost/toj?authSource=lupapiste` 
in config.edn or `mongodb://user:pwd@localhost/test-db?authSource=lupapiste` for tests, where `user` and `pwd` are same 
as in `user.properties` file in Lupapiste repository.

Alternatively you can create own user(s) for TOJ development and tests using MongoDB's db.createUser function and setup
them accordingly in `lupapiste-toj.test-helpers` and in `config.edn`.

**NOTE**: DO NOT commit your configs into GitHub remote repository.

### 2. Link lupapiste-commons into checkouts

Link (or checkout) [lupapiste-commons][1] to
checkouts/lupapiste-commons. This is required for running Figwheel.

    mkdir checkouts
    cd checkouts

    # *nix:
    ln -s /path/to/lupapiste/commons lupapiste-commons

    # Windows, Command Prompt run as administrator:
    mklink /j lupapiste-commons \path\to\lupapiste\commons\

### 3. Install npm debs
Install Karma and dependencies (required only for JS tests)

    $ lein npm install

### 4. Setup sessionkey

Create a file named `sessionkey` that contains 16 bytes of random data.
Copy or link the file in lupapiste-toj and lupapiste projects' root directory
(working directory when running the servers).

Session key must be same as in Lupapiste and in TOJ!

## Delopment 

Start figwheel to compile ClojureScript on the background and to provide a CLJS REPL. Figwheel also takes care of reloading CSS.

    $ lein figwheel

If you are going to edit LESS style files, start the LESS compiler in watch mode with

    $ lein less auto

The latest compiled main.css should be committed to Git.

Run a repl

    $ lein repl

Start server in repl, following Stuart Sierra's [Reloaded Workflow][2]

    user=> (go)

Browse to http://localhost:8010

[1]: https://github.com/lupapiste/commons
[2]: http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

### Log in

To log in to TOJ you need to:

1) Start Lupapiste dev server
2) Start TOJ dev server (see above)
3) Log in as a user in Lupapiste app (http://localhost:8000)
4) After successful login navigate to TOJ dev site in http://localhost:8010 (This works because the session is shared.)

## Testing

### Clojure tests

Run (tests once) 

    $ lein test 

OR run Clojure tests on the background

    $ lein test-refresh

NOTE: Tests are in fact integration tests and you need to have working MondoDB instance (see above). There have also 
been capability to run tests automatically, but at the moment (2022-08-29) it does not work.

### ClojureScript tests

Run ClojureScripts tests ion Chrome and PhantomJS:

    $ lein cljsbuild test

Or in PhantomJS only:

    $ lein cljsbuild test ci

To run tests automatically after a change in Chrome and PhantomJS:

    $ lein cljsbuild auto test

### Adding new ClojureScript unit test

This is slightly involved now, here are steps for adding a new test:

 1. Add test namespace under `test/cljs/` folder using convention `lupapiste-toj.bar-test`
 2. Edit `karma.adapter` namespace (under `test/cljs/karma` folder):
 3. Require the new namespace in the `ns` form
    - This is needed to actually load the test namespace

## License

Copyright © 2023 Cloudpermit Oy

Distributed under the European Union Public Licence (EUPL) version 1.2.
