(ns clojushop.handler
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [clojure.data.json :as json]
            [ring.middleware.nested-params :as nested-params]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [cheshire.core :as checore]
            [clojure.set :as cs]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :refer :all]
            [ring.util.response :as resp]
            [clojushop.status-codes :as status]
            [clojushop.dp-status-codes :as dp-status]
            [clojushop.validation :as val]
            [clojushop.mappings :as mp]
            [clojushop.utils :as utils]
            [clojushop.logger :as log]
            [clojushop.paths :as paths]
            [clojushop.http-constants :as chttp]
            [clojushop.data-provider :as dp]
            [clojushop.mongo-data-provider :as mdp]
            [digest :refer :all]
            [clj-stripe.common :as str-common]
            [clj-stripe.charges :as str-charges]
            [clj-stripe.plans :as str-plans]))

(import clojushop.mongo_data_provider.MongoDataProvider)

(def dp (dp/init (mdp/MongoDataProvider. "127.0.0.1" 27017)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

(defn resp-status-not-found []
  {:status status/not-found})

(defn resp-status-success []
  {:status status/success})


(def dp-to-ws-status-code-map
  "Maps the status codes delivered by DataProvider results to webservice status codes."
  {dp-status/error-unspecified status/error-unspecified
   dp-status/success status/success
   dp-status/bad-id status/wrong-params
   dp-status/user-already-exists status/user-already-exists
   dp-status/not-found status/not-found})


(defn replace-data-key
  "Helper function to replace a key in a map called 'data' with a new name 'new-key'.
  This is used when processing the results of DataProvider read operations, where the requested data
  is delivered as value of 'data' key, typically to map them to webservice response."
  ([new-key] (partial replace-data-key new-key))
  ([new-key data-map] (utils/replace-key :data new-key data-map)))


(defn wrap-with-key [key object]
  {key object})

(defn map-data-provider-result
  "Maps data provider read result to body of webservice response.
  In order to do this, first the result status code of the DataProvider operation is mapped to a webservice status code.
  Then, if the operation was successful, the requested data (which is provided as value of 'data' key), if present (read operation can also return nothing),
  is mapped to a map, using 'mapper' function, which will be sent to the client, together with status keyval."
  [res mapper]
  
  (merge {:status (dp-to-ws-status-code-map (:status res))}
      (when (= (:status res) dp-status/success)
        (when (not (nil? (:data res)))
          (mapper (:data res))))))


(defn mapper-partial-with-key
  "Helper function to construct mapper function which will be passed to map-data-provider-result.
  It creates a partial and wraps the mapping result with a key 'wrapper-key'
  The idea of the partial, is that we can pass parameters to the mapper function before it's executed in map-data-provider-result
  The wrapper key: E.g. we requested products, we get an array. Wrapper-key will be the json key for the array in the response."
  [ws-obj-mapping-fn wrapper-key]
  (comp
   (partial wrap-with-key wrapper-key)
   (partial utils/map-var #(ws-obj-mapping-fn %))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn add-md5 [response-body]
  (assoc response-body :md5 (digest/md5 (str response-body))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Authentication

(defn login [request]
  (let [params (:params request)]
    (if (empty? params)
      (resp/response {:status status/wrong-params})
      
      (let [user (:data (dp/user-get dp params))]

        (if (and (not (nil? user)) (= (:upw user) (:upw params))) ;validate password
          {:body {:status status/success} :session {:una (:una params)}}
          (resp/response {:status status/login-failed}))))))

(def logout-response
  {:body {:status status/success} :session nil})

(defn logout [request]
  logout-response)

(defn wrap-authentication [handler]
  (fn [request]
    (let [username (:una (:session request))]
      (if (nil? username)
        (resp/response {:status status/not-auth})
        (handler (merge request
                        {:params (merge (:params request) {:una username})}))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Image

(defn get-res-cat [screen-size]
  "Get resolution category identifier for screen size"
  (let [dims (clojure.string/split screen-size #"x")
        width (dims 0)
        height (dims 1)]

    ;placeholder implementation... we use 2 categories and a very simple algorithm - if
    ;screen width is less than 300 px we return category 1
    ;otherwise 2. The low res pictures turned to be a bit too low,
    ;this is why we the limit is currently only 300 px
    (if (< (Integer. width) 300) :1 :2)))

(defn replace-screen-with-res-cat
  "Replace screen size in passed params map with resolution category"
  [params]
  (assoc
      (into {} (remove (fn [[k v]] (= k :scsz)) params)) ;remove screen size
    :res (get-res-cat (:scsz params)))) ;add resolution category


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adapted from ring https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/keyword_params.clj
;; to allow keyword syntax starting with numbers
;; TODO put middleware in other namespace, or add letter to keys and
;; use rings middleware

(defn- keyword-syntax? [s]
  (re-matches #"[A-Za-z0-9*+!_?-]*" s))

(defn- keyify-params [target]
  (cond
    (map? target)
      (into {}
        (for [[k v] target]
          [(if (and (string? k) (keyword-syntax? k))
             (keyword k)
             k)
           (keyify-params v)]))
    (vector? target)
      (vec (map keyify-params target))
    :else
      target))

(defn wrap-keyword-params [handler]
  (fn [req] (handler (update-in req [:params] keyify-params))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Webservice handlers


(defmacro ws-handler
  "Wraps return value in body map: {:body return-value}, logs function call and parameters"
  [fn-name args & body]
  `(defn ~fn-name ~args
     (let [now# (System/currentTimeMillis)]

       (log/debug  (str "Calling: " (var ~fn-name) ", params: " ~@args))

       {:body (do ~@body)} ;wrap handler result with body element - Ring sends
                           ;this as the body of the response
       )))


(ws-handler products-get [params]

  (val/validate-products-get params
                             (fn []
                               (add-md5
                                (map-data-provider-result 
                                 (dp/products-get dp params)
                                 (mapper-partial-with-key
                                  (partial mp/product-dp->ws (get-res-cat (:scsz params)))
                                  :products))))))

(ws-handler product-add [params]
            (val/validate-product-add params #(dp/product-add dp params)))

(ws-handler product-remove [params]
  (val/validate-product-remove params #(dp/product-remove dp params)))

(ws-handler product-edit [params]
  (val/validate-product-edit params #(dp/product-edit dp params)))



(ws-handler cart-add [params]
  (val/validate-cart-add params #(let [result (dp/cart-add dp params)]
                                        ;TODO examine results
                                        (resp-status-success))))

(ws-handler cart-remove [params]
  (val/validate-cart-remove params #(dp/cart-remove dp params)))

;NOTE when user id doesn't exist we just return empty cart
(ws-handler cart-get [params]
            (val/validate-cart-get params
                                   (fn []
                                     (add-md5
                                      (map-data-provider-result 
                                        (dp/cart-get dp params)
                                         (mapper-partial-with-key
                                         (partial mp/cart-item-dp->ws (get-res-cat (:scsz params)))
                                         :cart))))))

(ws-handler cart-quantity [params]
            (val/validate-cart-quantity params #(dp/cart-quantity dp params)))



(ws-handler user-get [params]
            (val/validate-user-get params
                                   (fn []
                                     (add-md5
                                      (map-data-provider-result
                                       (dp/user-get dp params)
                                       (mapper-partial-with-key mp/user-dp->ws :user))))))

(defn user-remove [params]
  (val/validate-user-remove params #(do
                                       (dp/user-remove dp params)
                                       logout-response)))

(ws-handler user-register [params]
  (val/validate-user-register params #(dp/user-register dp params)))

(ws-handler user-edit [params]
  (val/validate-user-edit params #(dp/user-edit dp params)))


(ws-handler pay [params]
  (val/validate-pay params #(let [value-to-send (int (* (read-string (:v params)) 100)) ; Stripe wants cents
                                  stripe-result
                                  (str-common/with-token "your_stripe_secret_key" ; Insert secret key here (and ensure client uses matching public key)
                                    (str-common/execute (str-charges/create-charge (str-common/money-quantity value-to-send (:c params))
                                                                                   (str-common/card  (:to params)) ; Credit card token
                                                                                   (str-common/description (str "Payment test, from user: " (:una params))))))]

                              (if (and (nil? (:error stripe-result)) (nil? (:failure_message stripe-result)) (true? (:paid stripe-result)))
                                (do
                                  (dp/cart-clear dp params)
                                  (resp-status-success))
                                (do
                                  (log/info (str "Payment error! result: " stripe-result))
                                  {:status status/error-unspecified})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes

(defroutes public-routes
  (GET paths/products {params :params} (products-get params))
  (POST paths/user-register {params :params} (user-register params))
  (POST paths/user-login request (login request)))

(defroutes protected-routes
  ;; Users
  (GET paths/user-get {params :params} (user-get params))
  (GET paths/user-remove {params :params} (user-remove params))
  (POST paths/user-edit {params :params} (user-edit params))
  (GET paths/user-logout request (logout request))

  ;; Products
  (POST paths/product-add {params :params} (product-add params))
  (POST paths/product-remove {params :params} (product-remove params))
  (POST paths/product-edit {params :params} (product-edit params))
  
  ;; Cart
  (GET paths/cart-get {params :params} (cart-get params))
  (POST paths/cart-remove {params :params} (cart-remove params))
  (POST paths/cart-add {params :params} (cart-add params))
  (POST paths/cart-quantity {params :params} (cart-quantity params))

  ;; Payment
  (POST paths/pay {params :params} (pay params)))


(defroutes app-routes
  public-routes
  (wrap-authentication protected-routes)
  (route/not-found "Not found"))


(defn logging-middleware-req
  [handler]
  (fn [request]
    (log/debug ">>> middleware request: " request)
    (handler request)))

(defn logging-middleware-resp
  [handler]
  (fn [request]
    (let [response (handler request)]

      (log/debug ">>> middleware  response: " response)
      response)))

(def app
  (->
   (handler/api app-routes)

   (session/wrap-session
    {:cookie-name chttp/session-cookie-prefix :store (cookie-store)}
    ;{:cookie-attrs {:max-age 3600}}
    )
      
    ;(logging-middleware-req)

   (wrap-keyword-params)
      
   (middleware/wrap-json-body)
   (middleware/wrap-json-params)
   (middleware/wrap-json-response)

   ;(logging-middleware-resp)
   ))
