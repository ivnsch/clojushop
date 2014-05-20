(ns clojushop.handler
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojushop.dataprovider :as dataprovider]
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
            [clojushop.param-mappings :as mp]
            [clojushop.utils :as utils]
            [clojushop.logger :as log]
            [clojushop.paths :as paths]
            [clojushop.http-constants :as chttp]
            ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;macros

(defmacro ws-handler
  "Wraps return value in body map: {:body return-value}, logs function call and parameters"
  [fn-name args & body]
  `(defn ~fn-name ~args
     (let [now# (System/currentTimeMillis)]

       (log/debug  (str "Calling: " (var ~fn-name) ", params: " ~@args))

       {:body (do ~@body)} ;we need :body for response to have json
                           ;content type see http://stackoverflow.com/a/14891584/930450
       ;(response ~@body)
       )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;helpers

(defn resp-status-not-found []
  {:status status/not-found})

(defn resp-status-success []
  {:status status/success})


(def dp-to-ws-status-code-map
  {dp-status/error-unspecified status/error-unspecified
   dp-status/success status/success
   dp-status/bad-id status/wrong-params
   dp-status/user-already-exists status/user-already-exists
   dp-status/not-found status/not-found
   })


(defn replace-data-key
  ([new-key] (partial replace-data-key new-key))
  ([new-key data-map] (utils/replace-key :data new-key data-map)))


(defn wrap-with-key [key object]
  {key object})

(defn wrap-data-provider-op
  "Generic function to execute a data provider operation and map the returned data provider result to webservice result"
  [res data-transformer]
  
  (merge {:status (dp-to-ws-status-code-map (:status res))}
                                        ;if data provider returns a :data object, apply
                                        ;data-transformer function to it and add to map
      (when (= (:status res) dp-status/success)
        (when (not (nil? (:data res)))
          (data-transformer (:data res))))))


(defn map-db-result-data-to-ws
  "Helper function to process :data value of database read operation, in order to send it in :body webservice response.
It will first map the database objects to webservice objects using the function ws-obj-mapping-fn
and then wrap this with a new key wrapper-key"
  [ws-obj-mapping-fn wrapper-key]
  (comp
   (partial wrap-with-key wrapper-key)
   (partial utils/map-var #(ws-obj-mapping-fn %))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;authentication

(defn login [request]
  (let [params (:params request)]
    (if (empty? params)
      (resp/response {:status status/wrong-params})
      
      (let [user (:data (dataprovider/user-get (:username params)))]
        (if (= (:pw user) (:password params)) ;validate password
          {:body {:status status/success} :session {:username (:username params)}}
          (resp/response {:status status/login-failed}))))))

(def logout-response
  {:body {:status status/success} :session nil})

(defn logout [request]
  logout-response)

(defn wrap-authentication [handler]
  (fn [request]
    (let [username (:username (:session request))]
      (if (nil? username)
        (resp/response {:status status/not-auth})
        (handler (merge request
                        {:params (merge (:params request) {:username username})}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;handlers

(ws-handler products-get [params]
  (val/validate-products-get params (fn [] (wrap-data-provider-op 
                                                     (dataprovider/products-get params)
                                                     (map-db-result-data-to-ws mp/product-ws :products)))))
(ws-handler product-add [params]
  (val/validate-product-add params #(dataprovider/product-add params)))

(ws-handler product-remove [params]
  (val/validate-product-remove params #(dataprovider/product-remove params)))

(ws-handler product-edit [params]
  (val/validate-product-edit params #(dataprovider/product-edit params)))



(ws-handler cart-add [params]
  (val/validate-cart-add params #(let [result (dataprovider/cart-add params)]
                                        ;TODO examine results
                                        (resp-status-success))))

(ws-handler cart-remove [params]
  (val/validate-cart-remove params #(dataprovider/cart-remove params)))

;NOTE when user id doesn't exist we just return empty cart
(ws-handler cart-get [params]
  (val/validate-cart-get params (fn [] (wrap-data-provider-op 
                                        (dataprovider/cart-get params)
                                        (map-db-result-data-to-ws mp/cart-item-db-to-ws :cart)))))

(ws-handler cart-quantity [params]
            ;TODO use deconstruction to pass parameters
            (val/validate-cart-quantity params #(dataprovider/cart-quantity params)))



(ws-handler user-get [params]
  (val/validate-user-get params (fn [] (wrap-data-provider-op 
                                        (dataprovider/user-get params)
                                        (map-db-result-data-to-ws mp/user-db-to-ws :user)))))

(defn user-remove [params]
  (val/validate-user-remove params #(do
                                       (dataprovider/user-remove params)
                                       logout-response)))

(ws-handler user-register [params]
  (val/validate-user-register params #(dataprovider/user-register params)))

(ws-handler user-edit [params]
  (val/validate-user-edit params #(dataprovider/user-edit params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;routes

(defroutes public-routes
  (GET paths/products {params :params} (products-get params))
  (POST paths/user-register {params :params} (user-register params))
  (POST paths/user-login request (login request)))

(defroutes protected-routes
  ;users
  (GET paths/user-get {params :params} (user-get params))
  (GET paths/user-remove {params :params} (user-remove params))
  (POST paths/user-edit {params :params} (user-edit params))
  (GET paths/user-logout request (logout request))

  ;products
  (POST paths/product-add {params :params} (product-add params))
  (POST paths/product-remove {params :params} (product-remove params))
  (POST paths/product-edit {params :params} (product-edit params))
  
  ;cart
  (GET paths/cart-get {params :params} (cart-get params))
  (POST paths/cart-remove {params :params} (cart-remove params))
  (POST paths/cart-add {params :params} (cart-add params))
  (POST paths/cart-quantity {params :params} (cart-quantity params)))

(defroutes app-routes
  public-routes
  (wrap-authentication protected-routes)
  (route/resources "/") ;TODO is this necessary?
  (route/not-found "Not found"))


(defn logging-middleware-req
  [handler]
  (fn [request]
    (log/debug ">>> middleware request: " request)
    (handler request)
    ))

(defn logging-middleware-resp
  [handler]
  (fn [request]
    (let [response (handler request)]

      (log/debug ">>> middleware  response: " response)
      response
      )
    ))


(def app
  (->
      (handler/api app-routes)

      (session/wrap-session
       {:cookie-name chttp/session-cookie-prefix :store (cookie-store)}
       ;{:cookie-attrs {:max-age 3600}}
       )
      
      ;(logging-middleware-req)

      (keyword-params/wrap-keyword-params)
      (middleware/wrap-json-body)
      (middleware/wrap-json-params)
      (middleware/wrap-json-response)

      ;(logging-middleware-resp)
      ))
