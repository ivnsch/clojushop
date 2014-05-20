(ns clojushop.dataprovider
  (:require
            [monger.core :as mg]
            [monger.query :as mq]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.result :refer [ok? has-error?]]
            [clojushop.dp-status-codes :as dp-status]
            [clojushop.param-mappings :as mp]
            [clojushop.logger :as log]            
            )
  (:import
   [org.bson.types ObjectId]
   [com.mongodb MongoOptions ServerAddress])
  )


(def coll-products "products")
(def coll-users "users")


(let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
      ^ServerAddress sa (mg/server-address "127.0.0.1" 27017)]
  (mg/connect! sa opts)
  (mg/set-db! (mg/get-db "clojushop")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;helpers

(defn- status-result
  "helper to create generic status code result with optional message"
  ([status] (status-result nil))
  ([status msg]
     (let [status {:status status}]
       (if msg (assoc status {:msg msg}) status)
       )))

(defn- success-result
  "returns a generic sucess json result, with an optional message"
  ([] (success-result nil))
  ([msg]
     (status-result dp-status/success msg)))

(defn- error-result
  "returns a generic error json result, with error code an optional message. If error code is not provided, 0 is used"
  ([] (error-result dp-status/error-unspecified))
  ([error-code] (error-result error-code nil))
  ([error-code msg]
     (status-result error-code msg)))


(defn- wrap-read-result
  ([status] (wrap-read-result status nil))
  ([status data] ;TODO also error msg like in wrap-write-result
      {:status status :data data}))

(defn- wrap-write-result
  ([status] (wrap-write-result status nil))
  ([status err] 
     {:status status :err err}))

(defn- db-write-handler [write-op]
  (let [result (write-op)
        success (ok? result)
        err (get result "err")
        ]
    (wrap-write-result
     (if success dp-status/success dp-status/database-error)
     (when (not (empty? err)) err))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;products

(defn products-get [params]
  (let [start (Integer. (:st params))
        size (Integer. (:sz params))]
    
    (wrap-read-result dp-status/success
                      (into [] (mq/with-collection coll-products
                                 (mq/find {})
                                 (mq/limit size)
                                 (mq/skip start))))))

(defn product-add [params]
  (db-write-handler #(mc/insert coll-products (mp/product-catalog-db params))))

(defn product-remove [params]
  (let [id (ObjectId. (:id params))]
    
    (db-write-handler #(mc/remove coll-products {:_id id}))))


(defn product-edit [params]
  (let [id (ObjectId. (:id params))]
    
    (log/debug
     (mc/find coll-products {:_id id}))
    
    ;todo extract all editable product properties
    (let [editable-props [:na :des :img :pr :se]
          edit-params (select-keys params editable-props)
          editmap (mp/product-catalog-db edit-params)
          to-update {$set editmap}
          ]

        (db-write-handler #(mc/update-by-id coll-products id to-update))
      )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;cart


(defn- cart-insert [user-name product-id qt]
  (let [result
        (db-write-handler #(mc/update coll-users {:na user-name}
                                            {$push {:cart {:id product-id :qt qt}}}))]

    result
    ))

(defn cart-add [params]
  ;TODO check that product with this id exists, before inserting in cart
  ;currently it will be inserted and get-cart will deliver these items with
  ;nil fields

  (let [user-name (:username params)
        product-id (:pid params)
        
        item (mq/with-collection coll-users
               (mq/find {:na user-name "cart.id" product-id})
               )]

    (if (empty? item)
      (do
        (cart-insert user-name product-id 1))
      
      (let [current-qt (:qt (nth (:cart (into {} item)) 0)) ;TODO
            result (db-write-handler #(mc/update coll-users {:na user-name "cart.id" product-id}
                                                 {$set {"cart.$.qt" (+ current-qt 1)}}))]
        result
        ))))


(defn cart-remove [params]
  (let [user-name (:username params)
        product-id (:pid params)]
    (db-write-handler #(mc/update coll-users {:na user-name}
                                  {$pull {:cart {:id product-id}}}))))


(defn cart-get [params]
  (let [user-name (:username params)]
    
    ;get cart items
    (let [result 
          (mq/with-collection coll-users
            (mq/find {:na user-name})
            ;TODO projection - get only cart and remove map access bellow
            )
          items (:cart (into {} result))
          items-ids (into [] (map #(:id %) items))]
      
      ;get product for cart items ids
      (let [result-products
            (mq/with-collection coll-products
              (mq/find {:_id {$in items-ids}}))
            products (into [] result-products)
            ]

        ;merge cart items with products
        ;TODO better way? also, avoid into{} ?
        (wrap-read-result dp-status/success 
                          (let [items-full 
                                (into [] (map
                                          (fn [item]
                                            (select-keys
                                             (merge item
                                                    (into {} (filter (fn [product] (= (:id product) (:pid item))) products))
                                                    ) [:id :na :des :img :pr :se :qt])) items))]

                            items-full))))))

(defn cart-quantity [params]
  (let [
        user-name (:username params)
        product-id (:pid params)
        quantity (:qt params)
        item (mq/with-collection coll-users
               (mq/find {:na user-name "cart.id" product-id})
               )]

    (if (empty? item)
      (do
        (cart-insert user-name product-id quantity))
      (let [result (db-write-handler #(mc/update coll-users {:na user-name "cart.id" product-id}
                                              {$set {"cart.$.qt" quantity}}))]
        result
        ))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;users

(defn user-get [params]
  (let [user-name (:na params)
        res (mq/with-collection coll-users
              (mq/find user-name)
              )]

    (wrap-read-result
     (if (empty? res)
       dp-status/not-found
       dp-status/success
       )
     (if (empty? res)
       nil ;TODO still necessary to convert empty list to nil?
       (nth res 0))
     )))

(defn user-register [params]
  (let [user-name (:na params)
        res
        (mq/with-collection coll-users
          (mq/find {:na user-name})
          )]

    (if (empty? res)
      (do 
        (db-write-handler #(mc/insert coll-users (mp/user-db params)))
        (success-result))
      (error-result dp-status/user-already-exists))))

(defn user-remove [params]
  (let [user-name (:username params)]
    (db-write-handler #(mc/remove coll-users {:na user-name}))))


(defn user-edit [params]
  (let [id (:id params)]
    (let [editable-props [:em :pw]
          edit-params (select-keys params editable-props)
          editmap (mp/user-db edit-params)
          to-update {$set editmap}
          ]
      (db-write-handler #(mc/update coll-users {:na (:username params)} to-update))
      )
  ))
