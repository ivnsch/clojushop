(ns clojushop.dp-status-codes)

;dataprovider status codes

;general
(def error-unspecified 0)
(def success 1)
(def bad-id 2)
(def database-error 5)

;user
(def user-already-exists 3)
(def not-found 4) ;TODO is this status necessary for dataprovider or use success?

