(ns clojushop.data-provider
  (:require [clojushop.dp-status-codes :as dp-status]))


(defprotocol DataProvider

  (init [this])
  
  (products-get [this params])
  (product-add [this params])
  (product-remove [this params])
  (product-edit [this params])

  (cart-add [this params])
  (cart-remove [this params])
  (cart-get [this params])
  (cart-quantity [this params])

  (user-get [this params])
  (user-register [this params])
  (user-remove [this params])
  (user-edit [this params]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;helpers

(defn status-result
  "helper to create generic status code result with optional message"
  ([status] (status-result nil))
  ([status msg]
     (let [status {:status status}]
       (if msg (assoc status {:msg msg}) status)
       )))

(defn success-result
  "returns a generic sucess json result, with an optional message"
  ([] (success-result nil))
  ([msg]
     (status-result dp-status/success msg)))

(defn error-result
  "returns a generic error json result, with error code an optional message. If error code is not provided, 0 is used"
  ([] (error-result dp-status/error-unspecified))
  ([error-code] (error-result error-code nil))
  ([error-code msg]
     (status-result error-code msg)))


(defn wrap-read-result
  ([status] (wrap-read-result status nil))
  ([status data] ;TODO also error msg like in wrap-write-result
      {:status status :data data}))

(defn wrap-write-result
  ([status] (wrap-write-result status nil))
  ([status err] 
     {:status status :err err}))
