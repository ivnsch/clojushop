(ns clojushop.utils)


(defn map-var
  "Utility function, serves 2 purposes:
1. If val is a sequence, applies function to elements using (map), otherwise directly to val
2. If val is a sequence ensures that the type of the returned sequence is the same as of val
supported sequences: list, map, vector"
;TODO are there better solutions for this?
  [function val]
  (if (or (seq? val) (vector? val))
    (let [res (map function val)]
      (when (vector? val) (into [] res)))
    (let [res (function val)]
      (when (map? val) (into {} res)))))

;note had problems naming parameter "map" that's why "mmap"
(defn replace-key [old-key new-key mmap]
  (map (fn [[k  v]] [
                     (if (= k old-key) new-key k) ;key
                     v ;value
                     ]
         ) mmap))

(defn map-keys [keys-map mmap]
  (into {} (map (fn [[k v]] [(k keys-map) v]) mmap)))



(defmulti filter-map-keys (fn [keys mmap] (class keys)))

;;   "Filters a mmap with keys from keys-map, and then maps keys from mmap to values of keys-map
;; Example: 
;; (filter-and-map-keys {:a :newa :b :newb} {:a 1 :b 2 :c 3}) ==> {:newa 1 :newb 2}"
(defmethod filter-map-keys clojure.lang.PersistentHashMap
  [keys-map mmap]
  (map-keys keys-map (select-keys mmap (keys keys-map))))

(defmethod filter-map-keys clojure.lang.PersistentVector
  [keys-vector mmap]
  (select-keys mmap keys-vector))
