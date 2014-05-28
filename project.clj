(defproject clojushop "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [jayq "0.1.0-alpha3"]
                 [com.cemerick/friend "0.2.0"]
                 [ring/ring-json "0.3.0"]
                 [ring-json-params "0.1.0"]
                 [cheshire "5.3.1"]
                 [org.clojure/data.json "0.2.4"]
                 [com.novemberain/monger "1.7.0"]
                 [ring-middleware-format "0.3.2"]
                 [org.clojure/core.cache "0.6.3"]
                 [com.novemberain/validateur "2.1.0"]
                 [ring-basic-authentication "1.0.2"]
                 [abengoa/clj-stripe "1.0.4"]
                 [digest "1.4.4"]
                 ]
  :plugins [[lein-ring "0.8.10"]
            ]
  :ring {:handler clojushop.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
