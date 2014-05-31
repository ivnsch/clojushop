(ns clojushop.utils)


(defn map-var
  "Utility function, serves 2 purposes:
- If val is a sequence, applies function to elements using (map), otherwise directly to val
- If val is a sequence ensures that the type of the returned sequence is the same as of val
supported sequences: list, map, vector"
  ; TODO better implementation?
  [function val]
  (if (or (seq? val) (vector? val))
    (let [res (map function val)]
      (when (seq? val) (into '() res))
      (when (vector? val) (into [] res)))
    (let [res (function val)]
      (when (map? val) (into {} res)))))

(defn replace-key
  "Replaces a key in a map"
  [old-key new-key mmap]
  (map (fn [[k  v]] [(if (= k old-key) new-key k) v]
         ) mmap))

(defn map-keys
  "Maps the keys of a map mmap to new keys, using the map keys-map"
  [keys-map mmap]
  (into {} (map (fn [[k v]] [(k keys-map) v]) mmap)))



(defmulti filter-map-keys
  "Filters and maps keys of map mmap, using keys. The behaviour of mapping depends of the type of keys."
  (fn [keys mmap] (class keys)))

(defmethod filter-map-keys
  clojure.lang.PersistentHashMap
  [keys-map mmap]
  "Filters and maps keys of map mmap, using the map keys-map.
  Example: 
  (filter-and-map-keys {:a :newa :b :newb} {:a 1 :b 2 :c 3}) ==> {:newa 1 :newb 2}"
  (map-keys keys-map (select-keys mmap (keys keys-map))))

(defmethod filter-map-keys
  clojure.lang.PersistentVector
  [keys-vector mmap]
  "Filters the keys of map mmap using the vector keys-vector. No mapping is done / the keys are mapped to themselves"  
  (select-keys mmap keys-vector))
