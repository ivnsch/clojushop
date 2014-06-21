(ns clojushop.logger)

(def log-level :info) ;:info :debug :warn

(defn- my-print [label & msg]
  (println (apply str label msg)))

(defn- warning? []
  (= log-level :warn))

(defn- info? []
  (or (warning?) (= log-level :info)))

(defn- debug? []
  (or (info?) = log-level :debug))

(defn debug-custom [label & msg]
  (when (debug?)
    (apply my-print label msg)))

(defn debug [& msg]
  (apply debug-custom "debug: " msg))

(defn info [& msg]
  (when (info?)
    (my-print "info: " msg)))

(defn warning [& msg]
  (when (warning?)
    (my-print "\nWARNING: " msg "\n")))

(defn test-name [msg]
  (println "\n>>>> " (.toUpperCase msg) "\n"))

(defn response [response]
  (println "Response: " response))
