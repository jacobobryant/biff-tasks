(ns com.biffweb.tasks
  "A collection of tasks used by Biff projects.")

(def tasks
  {"clean"             'com.biffweb.tasks.clean/clean
   "css"               'com.biffweb.tasks.css/css
   "deploy"            'com.biffweb.tasks.deploy/deploy
   "dev"               'com.biffweb.tasks.dev/dev
   "nrepl"             'com.biffweb.tasks.nrepl/nrepl
   "prod-dev"          'com.biffweb.tasks.prod-dev/prod-dev
   "logs"              'com.biffweb.tasks.ssh/logs
   "prod-repl"         'com.biffweb.tasks.ssh/prod-repl
   "restart"           'com.biffweb.tasks.ssh/restart
   "setup"             'com.biffweb.tasks.setup/setup
   "soft-deploy"       'com.biffweb.tasks.soft-deploy/soft-deploy
   "test"              'com.biffweb.tasks.test/test
   "uberjar"           'com.biffweb.tasks.uberjar/uberjar})
