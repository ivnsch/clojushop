(ns clojushop.data-provider
  (:require [clojushop.dp-status-codes :as dp-status]))


(defprotocol DataProvider
  "Protocol for data source, typically a database"
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
;Helpers

(defn status-result
  "Helper to create a map with data provider result status code and an optional message.
  The possible status codes are in dp_status_codes.clj"
  ([status] (status-result nil))
  ([status msg]
     (let [status {:status status}]
       (if msg (assoc status {:msg msg}) status))))

(defn success-result
  "Returns a map with data provider success status code, and an optional message."
  ([] (success-result nil))
  ([msg]
     (status-result dp-status/success msg)))

(defn error-result
  "Returns a map with data provider error status code, and an optional message.
  The error status code can be passed as parameter error-code. If it's not passed, dp-status/error-unspecified will be used."
  ([] (error-result dp-status/error-unspecified))
  ([error-code] (error-result error-code nil))
  ([error-code msg]
     (status-result error-code msg)))


(defn wrap-read-result
  "Wraps a data provider read operation result in a map, which contains the requested data as value of a key called 'data'
  and a status flag, with key 'status' and a numeric value indicating the status of the read operation.
  The possible codes for the status are in dp_status_codes.clj"
  ([status] (wrap-read-result status nil))
  ([status data] ;TODO also error msg like in wrap-write-result
      {:status status :data data}))

(defn wrap-write-result
  "Wraps a data provider write operation result in a map, which contains a status flag, with key 'status' and a numeric value indicating the
  status of the write operation. The possible codes for the status are in dp_status_codes.clj"
  ([status] (wrap-write-result status nil))
  ([status err] 
     {:status status :err err}))
