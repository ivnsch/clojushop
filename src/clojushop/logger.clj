(ns clojushop.logger)

(def log-level :info) ;:info :debug :warn


(defn- debug? []
  (= log-level :debug))

(defn- info? []
  (or (debug?) (= log-level :info)))

(defn- warning? []
  (or (info?) (= log-level :warn)))


(defn debug [msg]
  (when (debug?)
    (println (str "debug: ") msg)))

(defn info [msg]
  (when (info?)
    (println (str "info: " msg))))

(defn warning [msg]
  (when (warning?)
    (println (str "\nWARNING: " msg "\n"))))

(defn test-name [msg]
  (info (str "\n>>>> " (.toUpperCase msg) "\n")))

(defn response [response]
  (info (str "Response: " response)))

