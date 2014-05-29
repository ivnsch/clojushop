(ns clojushop.validation
  (:require [validateur.validation :as val]
            [clojushop.status-codes :as status]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;helpers
;TODO replace text message with number

(defn- val-min-max-length [keyword min max]
  (val/format-of keyword :format (re-pattern (str "^.{" min "," max "}$")) :message (str "length:{" min "," max "}")))

(defn- val-white-sp [keyword]
  (val/format-of keyword :format #"^[^\s]*$" :message "whitesp"))

(defn- val-is-float [keyword]
 (val/format-of keyword :format #"^\d*\.?\d*$" :message "float"))

(defn- val-is-int [keyword]
 (val/format-of keyword :format #"^\d*$" :message "int"))

(defn- val-email [keyword]
  (val/format-of keyword :format #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$" :message "email"))

(defn- val-db-id [keyword]
  (val/format-of keyword :format #"^[0-9a-fA-F]{24}$" :message "id"))


(defmulti val-empty class)

(defmethod val-empty clojure.lang.Keyword [keyword]
  (val/presence-of keyword :message "empty"))

;TODO superclass of persistent vector?
(defmethod val-empty clojure.lang.PersistentVector [keyword-list]
  (map #(val-empty %) keyword-list))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;webservice parameters validation


(def validations-get-products
  (val/validation-set
   (val-empty :st)
   (val-empty :sz)
   (val-empty :scsz)
   
   (val-min-max-length :st 1 20)
   (val-min-max-length :sz 1 20)
   (val-min-max-length :scsz 1 20)

   (val-is-int :st)
   (val-is-int :sz)
   ))

(def validations-product-add
  (val/validation-set
            (val-empty :na)
            (val-empty :des)
            (val-empty :img)            
            (val-empty :pr)
            (val-empty :se)

            ;TODO
            ;; (val-empty [:img :pd])
            ;; (val-empty [:img :pl])
            
            ;; (val-empty [:img :pl :1])
            ;; (val-empty [:img :pl :1])            
            ;; (val-empty [:img :pd :2])            
            ;; (val-empty [:img :pd :2])
            
            (val-min-max-length :na 1 20)
            (val-min-max-length :des 1 200)

            ;; (val-min-max-length [:img :pl :1] 1 100)
            ;; (val-min-max-length [:img :pl :2] 1 100)
            ;; (val-min-max-length [:img :pd :1] 1 100)
            ;; (val-min-max-length [:img :pd :2] 1 100)            

            ;(val-min-max-length :pr 1 20)
            (val-min-max-length :se 1 100)

            (val-is-float [:pr :v])
            (val-is-int [:pr :c])
            )
)

;TODO
(def validations-product-edit
  (val/validation-set
  ))

;TODO
(def validations-user-edit
  (val/validation-set
   ))


(def validations-remove-product
  (val/validation-set
            (val-empty :id)
            
            (val-db-id :id)))

(def validations-user-register

  (val/validation-set
            (val-empty :una)
            (val-empty :uem)
            (val-empty :upw)

            (val-white-sp :una)
            (val-white-sp :uem)
            (val-white-sp :upw)            
            
            (val-min-max-length :una 1 20)
            (val-min-max-length :uem 1 30)
            (val-min-max-length :upw 1 15)

            ;; (val-email :em) TODO not working with unit test emails
            ))

(def validations-user-remove
  (val/validation-set
            (val-empty :una)

            (val-white-sp :una)
            
            (val-min-max-length :una 1 20)
            ))

(def validations-cart-get
  (val/validation-set
            (val-empty :una)

            (val-min-max-length :una 1 20)            
            ;(val-db-id :una)

            ;TODO reuse validations from scsz from product
            ))

(def validations-cart-add
  (val/validation-set
            (val-empty :una)
            (val-empty :pid)

            (val-min-max-length :una 1 20)            
            (val-db-id :pid)            
            ))

;TODO
(def validations-cart-remove
  (val/validation-set
            ))

;TODO
(def validations-cart-quantity
  (val/validation-set
            ))

(def validations-user-get
  (val/validation-set
            ;(val-empty :una)

            ;(val-white-sp :una)
            
            ;(val-min-max-length :una 1 20)
            ))

(def validations-user-add
  (val/validation-set
            (val-empty :una)
            (val-empty :pid)

            (val-min-max-length :una 1 20)            
            (val-db-id :pid)            
            ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;TODO validation should not send webservice response
;create validation result handler in handler
;TODO validation error messages use numers instead of text
(defn- validate-on-result [on-validated val-result]
  (if (val/valid? val-result)
    (on-validated)
    {:status status/validation-error :err val-result}))


(defn- validate [validation-set params on-validated]
  (let [v validation-set
        val-result (v params)]
    (validate-on-result on-validated val-result)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn validate-products-get [params on-validated]
  (validate validations-get-products params on-validated))

(defn validate-product-add [params on-validated]
  (validate validations-product-add params on-validated))

(defn validate-product-remove [params on-validated]
  (validate validations-remove-product params on-validated))

(defn validate-product-edit [params on-validated]
  (validate validations-product-edit params on-validated))



(defn validate-cart-add [params on-validated]
  (validate validations-cart-add params on-validated))

(defn validate-cart-remove [params on-validated]
  (validate validations-cart-remove params on-validated))

(defn validate-cart-get [params on-validated]
  (validate validations-cart-get params on-validated))

(defn validate-cart-quantity [params on-validated]
  (validate validations-cart-quantity params on-validated))



(defn validate-user-get [params on-validated]
  (validate validations-user-get params on-validated))

(defn validate-user-remove [params on-validated]
  (validate validations-user-remove params on-validated))

(defn validate-user-register [params on-validated]
  (validate validations-user-register params on-validated))

(defn validate-user-edit [params on-validated]
  (validate validations-user-edit params on-validated))
