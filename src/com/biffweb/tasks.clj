(ns com.biffweb.tasks
  "A collection of tasks used by Biff projects.")

(def tasks
  {"css"          'com.biffweb.tasks.css/css
   "deploy"       'com.biffweb.tasks.deploy/deploy
   "dev"          'com.biffweb.tasks.dev/dev
   "format"       'com.biffweb.tasks.format/format
   "nrepl"        'com.biffweb.tasks.nrepl/nrepl
   "prod-install" 'com.biffweb.tasks.prod/install
   "prod-logs"    'com.biffweb.tasks.prod/logs
   "prod-nrepl"   'com.biffweb.tasks.prod/nrepl
   "prod-restart" 'com.biffweb.tasks.prod/restart
   "setup"        'com.biffweb.tasks.setup/setup
   "test"         'com.biffweb.tasks.test/test
   "uberjar"      'com.biffweb.tasks.uberjar/uberjar})
