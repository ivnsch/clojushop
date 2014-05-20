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
   
   (val-min-max-length :st 1 20)
   (val-min-max-length :sz 1 20)

   (val-is-int :st)
   (val-is-int :sz)
   ))

(def validations-create-product
(val/validation-set
            (val-empty :na)
            (val-empty :des)
            (val-empty :img)            
            (val-empty :pr)
            (val-empty :se)            
            
            (val-min-max-length :na 1 20)
            (val-min-max-length :des 1 200)
            (val-min-max-length :img 1 100)
            (val-min-max-length :pr 1 20)
            (val-min-max-length :se 1 100)

            (val-is-float :pr)))

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

(def validations-user-create

  (val/validation-set
            (val-empty :na)
            (val-empty :em)
            (val-empty :pw)

            (val-white-sp :na)
            (val-white-sp :em)
            (val-white-sp :pw)            
            
            (val-min-max-length :na 1 20)
            (val-min-max-length :em 1 30)
            (val-min-max-length :pw 1 15)

            ;; (val-email :em) TODO not working with unit test emails
            ))

(def validations-user-remove
  (val/validation-set
            (val-empty :username)

            (val-white-sp :username)
            
            (val-min-max-length :username 1 20)
            ))

(def validations-cart-get
  (val/validation-set
            (val-empty :username)

            (val-min-max-length :username 1 20)            
            ;(val-db-id :username)
            ))

(def validations-cart-add
  (val/validation-set
            (val-empty :username)
            (val-empty :pid)

            (val-min-max-length :username 1 20)            
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
            (val-empty :username)

            (val-white-sp :username)
            
            (val-min-max-length :username 1 20)
            ))

(def validations-user-add
  (val/validation-set
            (val-empty :username)
            (val-empty :pid)

            (val-min-max-length :username 1 20)            
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

(defn validate-product-create [params on-validated]
  (validate validations-create-product params on-validated))

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
  (validate validations-user-create params on-validated))

(defn validate-user-edit [params on-validated]
  (validate validations-user-edit params on-validated))
