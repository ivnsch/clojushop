(ns clojushop.status-codes)

;webservice status codes

;general
(def error-unspecified 0)
(def success 1)
(def wrong-params 2) ;TODO rename bad-params
(def not-found 4)
(def validation-error 5)

;user
(def user-already-exists 3)
(def login-failed 6)
(def not-auth 7)
