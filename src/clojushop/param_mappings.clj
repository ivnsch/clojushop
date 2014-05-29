(ns clojushop.param-mappings
  (:require [clojushop.utils :as utils])
  (:import
   [org.bson.types ObjectId]))

;TODO decouple from Mongo (ObjectId.)

;TODO rename db-ws-mapping-filtering

;TODO mapping for nested values

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;util

;Adds a Mongo id to obj - parses :id if present
;otherwise creates a new one
(defn assoc-db-id [obj]
  (assoc
      obj
    :_id (if (contains? obj :id) (ObjectId. (:id obj)) (ObjectId.))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;db -> ws

(def cart-item-ws-keys
  {:id :id
   :na :na
   :des :des
   :img :pic
   :pr :pr
   :se :se
   :qt :qt})

(def product-ws-keys
  [:na :des :img :pr :se])


;TODO cart item contains product - and pass here only product
;instead of item-with-imgs
(defn- filter-resolutions [item-with-imgs res-cat]
  "Filters resolutions map with resolutions corresponsing to res-cat. This returns a product with the :img value replaced by   the fitered value."
  (update-in item-with-imgs [:img]
             (fn [val] (into {} (map (fn [[k v]] [k (res-cat v)]) val)))))



(defn cart-item-db-to-ws [res-cat item]
  (utils/filter-map-keys cart-item-ws-keys
                         (filter-resolutions item res-cat)))



(defn product-ws
  "Maps product delivered by dataprovider to product we sent to client.
   First the image map is filtered by res-cat (resolution-category).
   Then the product keys are mapped."
  [res-cat product]
  (assoc
      (utils/filter-map-keys product-ws-keys
                             (filter-resolutions product res-cat))
    :id (str (:_id product))
    ))


;TODO
(defn user-db-to-ws [user]
  {:id (str (:_id user))
   :una (:una user)
   :uem (:uem user)
   }
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;ws -> db

(def product-db-keys
  [:na :des :img :pr :se]
  )

;TODO merge the username from params in prod and pass only prod?
(defn product-catalog-db [product]
  (utils/filter-map-keys product-db-keys product))

(defn product-catalog-db-with-id [product]
  (assoc-db-id (product-catalog-db product)))

(def user-db-keys [:una :uem :upw])

(defn user-db [user-ws]
  (utils/filter-map-keys user-db-keys user-ws))

(defn user-db-with-id [user-ws]
  (assoc-db-id (user-db user-ws)))
