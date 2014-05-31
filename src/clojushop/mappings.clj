(ns clojushop.mappings
  #^{:doc "Functions to map parameters/results between handler(webservice) and data provider"}
  (:require [clojushop.utils :as utils])
  (:import
   [org.bson.types ObjectId]))

;; TODO decouple from Mongo (ObjectId.)

;; TODO rename db-ws-mapping-filtering

;; TODO mapping for nested values

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Util

;; Adds a Mongo id to obj - parses :id if present, otherwise creates a new one
(defn assoc-db-id [obj]
  (assoc obj
    :_id (if (contains? obj :id) (ObjectId. (:id obj)) (ObjectId.))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Mapping dp -> ws (data provider to webservice)

(def cart-item-keys-dp->ws
  {:id :id
   :na :na
   :des :des
   :img :pic
   :pr :pr
   :se :se
   :qt :qt})

(def product-keys-dp->ws
  [:na :des :img :pr :se])


;; TODO cart item contains product - and pass here only product
;; instead of item-with-imgs
(defn- filter-resolutions [item-with-imgs res-cat]
  "Filters resolutions map with resolutions corresponsing to res-cat. This returns a product with the :img value replaced by the fitered value."
  (update-in item-with-imgs [:img]
             (fn [val] (into {} (map (fn [[k v]] [k (res-cat v)]) val)))))



(defn cart-item-dp->ws [res-cat item]
  (utils/filter-map-keys cart-item-keys-dp->ws
                         (filter-resolutions item res-cat)))



(defn product-dp->ws
  "Maps product delivered by dataprovider to product we sent to client.
   First the image map is filtered by res-cat (resolution-category).
   Then the product keys are mapped."
  [res-cat product]
  (assoc
      (utils/filter-map-keys product-keys-dp->ws
                             (filter-resolutions product res-cat))
    :id (str (:_id product))))


;; TODO
(defn user-dp->ws [user]
  {:id (str (:_id user))
   :una (:una user)
   :uem (:uem user)
   })


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Mapping dp -> ws (webservice to data provider)

(def product-keys-ws->dp
  [:na :des :img :pr :se])

;; TODO merge the username from params in prod and pass only prod?
(defn product-ws->dp [product]
  (utils/filter-map-keys product-keys-ws->dp product))

(defn product-catalog-db-with-id [product]
  (assoc-db-id (product-ws->dp product)))

(def user-keys-ws->dp [:una :uem :upw])

(defn user-ws->dp [user-ws]
  (utils/filter-map-keys user-keys-ws->dp user-ws))


;; TODO remove?
(defn user-db-with-id [user-ws]
  (assoc-db-id (user-ws->dp user-ws)))
