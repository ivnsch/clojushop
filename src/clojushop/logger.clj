(ns clojushop.logger)

(def log-level :info) ;:info :debug :warn


(defn- debug? []
  (= log-level :debug))

(defn- info? []
  (or (debug?) (= log-level :info)))

(defn- warning? []
  (or (info?) (= log-level :warn)))


(defn debug [& msg]
  (when (debug?)
    (apply println "debug: " msg)))

(defn info [& msg]
  (when (info?)
    (apply println "info: " msg)))

(defn warning [& msg]
  (when (warning?)
    (apply println "\nWARNING: " msg "\n")))

(defn test-name [msg]
  (println "\n>>>> " (.toUpperCase msg) "\n"))

(defn response [response]
  (println "Response: " response))
