(ns clojushop.test.handler
  (:use clojure.test
        ring.mock.request  
        clojushop.handler)
  (:require [cheshire.core :require :all]
            [monger.collection :as mc]
            [clojushop.status-codes :as status]
            [clojushop.mongo-data-provider :as mdp]
            [clojushop.logger :as log]
            [clojushop.paths :as paths]
            [clojushop.http-constants :as chttp]
            ))

(import java.lang.reflect.Modifier)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;general utils TODO move

(defn match-keys? [map keys]
  (apply = (map count [keys (select-keys map keys)])))

(defn has-keys? [map keys]
  ;check if map contains every key
  ;the partial is passed to "every" to be evaluated on each key
  (every? (partial contains? map) keys))


(defn static? [field]
  (java.lang.reflect.Modifier/isStatic (.getModifiers field)))

(defn get-record-field-names [record]
  (->> record
       .getDeclaredFields
       (remove static?)
       (map #(.getName %))
       (remove #{"__meta" "__extmap"})))

(defn get-record-field-keys [record]
  (map #(keyword %) (get-record-field-names record)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;helpers


(defn test-valid-product [product]
  ;TODO review this
  (let [id (:id product)
        name (:name product)
        description (:description product)
        image (:picture product)
        price (:price product)
        seller (:seller product)]

    (->> id
        is
        (re-matches #"\d+")
        )

    ;todo avoid 2 different section for ->> and -> (not necessary
    ;anymore though)
    
    (is (< 0 (count id) 10))

    (is (< 0 (count name) 30))
    (is (and (>= (count description) 0) (< (count description) 100)))

    true ;this return value does not affect the test
))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;request helpers


;request builders
;TODO rename wrap-req-json etc.
(defn wrap-req-json [request]
  (-> request
      (content-type "application/json")))

(defn wrap-req-auth [request token]
  (-> request
      (header "Cookie" (str chttp/session-cookie-prefix "=" token))))

(defn wrap-req-json-auth [request token]
  (-> request
      wrap-req-json
      (wrap-req-auth token)))



(defn gen-get-req
  ([path] (gen-get-req nil))
  ([path params]
      (->
       (request :get path (when (not (nil? params)) params))
       (wrap-req-json))))

(defn gen-get-req-auth [path params token]
  (->
   (gen-get-req path params)
   (wrap-req-json-auth token)))

(defn gen-post-req [path params]
  (->
   (request :post path (when (not (nil? params)) (clojure.data.json/write-str params)))
   (wrap-req-json)))

(defn gen-post-req-auth [path params token]
  (->
   (gen-post-req path params)
   (wrap-req-json-auth token)))



;convenience methods to execute request, return response
(defn req-get
  ([path] (req-get path nil))
  ([path params]
     (app (gen-get-req path params))))

(defn req-get-auth
  ([path token] (req-get-auth path nil token))
  ([path params token]
     (app (gen-get-req-auth path params token))))

(defn req-post
  ([path] (req-post path nil))
  ([path params]
      (app (gen-post-req path params))))

(defn req-post-auth
  ([path token] (req-post-auth))
  ([path params token]
     (app (gen-post-req-auth path params token))))




; common tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn test-valid-response
  "General response checks"
  [response]
  (is (= (:status response) 200))
  (is (not (empty? (:body response))))
  (is (not=  (.indexOf (str (:headers response)) "application/json") -1 ))
  )


(defn test-not-found
  [response]
  (is (= (:status response) 200)))

(defn test-valid-response-with-body
  [response]
  (test-valid-response response)
  (is (contains? response :body))
  )

(defn test-valid-body
  [body-json]
  (is (contains? body-json :status)))

(defn test-response-with-valid-body [response]
  (test-valid-response-with-body)
  (test-valid-body (:body response))
  ;TODO test class of body is map not e.g. string
  )


(defn test-body-status [body-json status]
  (is (= (:status body-json) status)))

(defn test-success-body [body-json]
  (test-body-status body-json 1))


(defn test-unauthorized [response]
  (let [body-json (cheshire.core/parse-string (:body response) true)]
  ;; (is (= (:status json) 401))    
    (test-body-status body-json status/not-auth)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;request utils

(defn keyvals-str-to-map [keyvals keyval-delim pairs-delim]
  (clojure.walk/keywordize-keys
   (apply hash-map
          (flatten
           (map #(clojure.string/split % keyval-delim)
                (clojure.string/split keyvals pairs-delim))))))


(defn get-set-cookie-header [response]
  (:Set-Cookie (clojure.walk/keywordize-keys (:headers response))))

(defn has-set-cookie-header [response]
  (is (not (empty? (get-set-cookie-header response)))))

(defn get-auth-token [response]
  (let [auth-cookie (first (filter #(.contains % chttp/session-cookie-prefix) (get-set-cookie-header response)))
        cookie-map (keyvals-str-to-map auth-cookie #"=" #";")
        auth-token ((keyword chttp/session-cookie-prefix) cookie-map)
        ]
    auth-token))
        

;test data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dummy-products
  [
   {:na "Cookies" :des "Good tasting!" :img "http://ecx.images-amazon.com/images/I/81im-ztcK8L._SY606_.jpg" :pr "2" :se "ischuetz"}
   {:na "Blueberries" :des "Healthy!" :img "http://1.bp.blogspot.com/-sumate-5zQE/Tc6B5LSkqzI/AAAAAAAAABQ/Uq0NBhxB0aQ/s1600/AA026339.png" :pr "3" :se "betty123"}
   {:na "Meat" :des "The best!" :img "http://0.static.wix.com/media/01c68a_730785e499ab4ce8c43e26ab335a876b.jpg_1024" :pr "7" :se "a-fisher"}
   {:na "Juice" :des "100% fruit!" :img "http://ecx.images-amazon.com/images/I/71gBbObPBxL._SY606_.jpg" :pr "4" :se "ischuetz"}
   ])

(defn index-to-db-id [index]
  (apply str (repeat 24 (str index)))  
  )

(defn generate-test-id [documents element]
  (index-to-db-id (.indexOf documents element)))

(defn add-test-mongo-ids [documents]
  (map
   #(assoc % :_id
           (generate-test-id documents %))
   documents))

(defn clear-db-products []
  (mc/remove mdp/coll-products)
  )

(defn clear-db-users []
  (mc/remove mdp/coll-users)
  )

(defn clear-db []
  (clear-db-products)
  (clear-db-users))

(defn add-test-products []
  (clear-db-products) ;TODO remove from here
  (clear-db-users) ;TODO remove from here

  (doseq [product (add-test-mongo-ids dummy-products)]
        (mc/insert mdp/coll-products product)))





;TODO remove
(defn register-user1 []
  (app (request :post paths/user-register {:na "user1" :em "user2@foo.com" :pw "test123"})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn test-products-logged-in [token]
  
  (log/test-name "product-add, authorized, products empty")

  (let [response (req-post-auth paths/product-add (dummy-products 0) token)]
;  (let [response (app (-> (request-json-auth2 (request :post "/product/add" (clojure.data.json/write-str (dummy-products 0))) token)))]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-success-body body)))

  (log/test-name "product-get, authorized, after add one product")
  (let [response (req-get paths/products-get {:st 0 :sz 2})]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-success-body body)

      (is (= (count (:products body)) 1))

      (for [product (body :products)]
          (is (test-valid-product (product))))))

  (log/test-name "product-add, authorized, add second product")
  (let [response (req-post-auth paths/product-add (dummy-products 1) token)]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-success-body body)))

  (log/test-name "product-get, after add second product")
  (let [response (req-get paths/products-get {:st 0 :sz 2})]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-success-body body)

      (is (= (count (:products body)) 2))

      (for [product (:products body)]
          (is (test-valid-product (product))))
      ))

  (log/test-name "product-get, authorized, after add second product, test pagination")
  (let [response (req-get paths/products-get {:st 0 :sz 1})]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-success-body body)

      (is (= (count (:products body)) 1))
      (is (= (:na ((:products body) 0)) (:name (dummy-products 0)))) ;check sorting is correct
      
      (for [product (body :products)]
          (is (test-valid-product (product))))

      (let [product-id (:id ((:products body) 0))]

        (log/test-name "product-remove, authorized, remove the product we retrieved")
        (let [response (req-post-auth paths/product-remove {:id product-id} token)]
          (log/response response)

          (test-valid-response response)

          (let [body (cheshire.core/parse-string (:body response) true)]
            (test-valid-body body)
            (test-success-body body))

          )

        (log/test-name "products-get, after remove")
        (let [response (req-get paths/products-get {:st 0 :sz 2})]
          (log/response response)

          (test-valid-response response)

          (let [body (cheshire.core/parse-string (:body response) true)]
            (test-valid-body body)
            (test-success-body body)

            (is (= (count (:products body)) 1))

            (for [product (:products body)]
              (is (test-valid-product (product))))

              (is (not (= (:id ((:products body) 0)) product-id))) ;check product left is not the one we removed

              
              (log/test-name "products-edit, authorized")
              (let [product-id (:id ((:products body) 0))
                    new-name "new-name"
                    new-price "999.9"
                    new-desc "new-description"
                    response (req-post-auth paths/product-edit {:id product-id :na new-name :pr new-price :des new-desc} token)]
                (log/response response)

                (test-valid-response response)

                (let [body (cheshire.core/parse-string (:body response) true)]
                  (test-valid-body body)
                  (test-success-body body))


                (log/test-name "product-get, authorized, after edit")
                (let [response (req-get paths/products-get {:st 0 :sz 2})]
                  (log/response response)

                  (test-valid-response response)

                  (let [body (cheshire.core/parse-string (:body response) true)]
                    (test-valid-body body)
                    (test-success-body body)

                    (is (= (count (:products body)) 1))

                    (for [product (body :products)]
                      (is (test-valid-product (product))))

                    (let [product ((:products body) 0)]
                      (is (= (:id product) product-id))
                      (is (= (:name product) new-name))
                      (is (= (:description product) new-desc))
                      (is (= (:price product) new-price))
                      )))
                )
              
;TODO edit - test properties not editable like seller
              ))
        ))
    )
  )


(deftest test-products
  
  ;preconditions for tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (clear-db)
  
  (testing "products"

    (log/test-name "product-get, no params")
    (let [response (req-get paths/products-get)]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]
      (test-valid-body body)
      (test-body-status body status/validation-error)))
  
    (log/test-name "get-products, empty")
    (let [response (req-get paths/products-get {:st 0 :sz 2})]
      (log/response response)

      (test-valid-response-with-body response)

      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body)

        (is (contains? body :products))
        (is (empty? (:products body)))))


    (log/test-name "add product without authorization...")    
    (let [response (req-post paths/product-add (dummy-products 0))]
      
       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-body-status body status/not-auth))
       
       ;tests with logged in user
       ;register and login a user
       (register-user1) ;TODO add the user in fill-db-with-test-data
       (log/test-name "loggin in the user...")
       (let [response (req-post paths/user-login {:username "user1" :password "test123"})
             auth-token (get-auth-token response)]

         (test-products-logged-in auth-token))
       )
    ))



; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;TODO ? test user names are unique

(defn test-users-logged-in [token]

  (log/test-name "getting user we registered, authenticated")
  (let [response (req-get-auth paths/user-get token)]
    (log/response response)

    (test-valid-response-with-body response)
    
    (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body)

        (is (contains? body :user))
        (is (not (empty? (:user body))))

        (let [user (:user body)]

          (is (contains? user :na))
          (is (contains? user :em))
          (is (not (contains? user :pw))))))

  (log/test-name "editing user email, authenticated")
  (let [
        new-email "new-email@bla.com"
        response (req-post-auth paths/user-edit {:em new-email} token)]
    (log/response response)

    (test-valid-response-with-body response)
    
    (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body)

        (log/test-name "get user after edit, authenticated")
        (let [response (req-get-auth paths/user-get token)]
          (log/response response)

          (test-valid-response-with-body response)
          
          (let [body (cheshire.core/parse-string (:body response) true)]

            (test-valid-body body)
            (test-success-body body)

            (is (contains? body :user))
            (is (not (empty? (:user body))))

            (let [user (:user body)]

              (is (contains? user :na))
              (is (contains? user :em))
              (is (not (contains? user :pw)))
              
              (is (= (:em user) new-email)))))))

  ;TODO test edit password
  
  (log/test-name "logout, authenticated")
  (let [response (req-get-auth paths/user-logout token)]
    (log/response response)

    (test-valid-response-with-body response)

    ;logout clears the auth cookie by setting a new not auth cookie
    (has-set-cookie-header response)
    
    (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body))

    (let [logout-auth-token (get-auth-token response)]

      (log/test-name "get user after logout, with logout session token")
      (let [response (req-get-auth paths/user-get logout-auth-token)]
        (log/response response)

        (test-valid-response-with-body response)
        
        (let [body (cheshire.core/parse-string (:body response) true)]

          (test-valid-body body)
          (test-body-status body status/not-auth)))
      )
    )

  (log/test-name "get user after logout, with old session token")
  (let [response (req-get-auth paths/user-get token)]
    (log/response response)

    (test-valid-response-with-body response)
    
    (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        
        ;note: the old session token is still valid!
        ;in browser/app this would not be used
        ;since Set-Cookie from logout response replaces this token with a new (invalid) one
        (test-success-body body)))

  
  (log/info "logging in again...")
  (let [response (req-post paths/user-login {:username "user1" :password "test123"})
        auth-token (get-auth-token response)]

    (log/debug (str "response of login: " response))

    
    (log/test-name "removing user, authenticated")
    (let [response (req-get-auth paths/user-remove auth-token)]
      (log/response response)

      (test-valid-response-with-body response)
      
      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body)

        ;remove user clears the auth cookie by setting a new not auth
        ;cookie (like logout)
        (has-set-cookie-header response)

        ;check that the user is gone using a still valid auth token
        (log/test-name "get user after remove, with old session token")
        (let [response (req-get-auth paths/user-get auth-token)]
          (log/response response)

          (test-valid-response-with-body response)
          
          (let [body (cheshire.core/parse-string (:body response) true)]

            (test-valid-body body)
            
            (test-body-status body status/not-found)

            (is (not (contains? body :user)))
            ))
        ))   
    ))

(deftest test-users

  ;preconditions for tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (clear-db)
  (add-test-products)
  
  (testing "users"

    (log/test-name "user-register no params")
    (let [response (req-post paths/user-register)]
      (log/response response)

      (test-valid-response response)
      ;TODO wrong parameters response (status 2)/ test
    )
    
    (log/test-name "user-register wrong params")
    (let [response (req-post paths/user-register (clojure.data.json/write-str {:na "aaaaaaa"}))]
      (log/response response)

      (test-valid-response response)

      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-body-status body status/validation-error)
        )
     )

    (log/test-name "registering user...")
    (let [response (req-post paths/user-register {:na "user1" :em "user1@foo.com" :pw "test123"})]
      (log/response response)

      (test-valid-response-with-body response)
      
      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-success-body body)
        ))

    (log/test-name "registering a new user with same name")
    (let [response (req-post paths/user-register {:na "user1" :em "user2@foo.com" :pw "test123"})]
      
      (log/response response)

      (test-valid-response-with-body response)
      
      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-body-status body status/user-already-exists)))

    (log/test-name "getting user we registered, not authenticated")
    (let [response (req-get paths/user-get {:na "user1"})]

      (log/response response)
      
      (test-valid-response-with-body response)
      
      (let [body (cheshire.core/parse-string (:body response) true)]

        (test-valid-body body)
        (test-body-status body status/not-auth)))

    (log/test-name "login, with user we just registered")
    (let [response (req-post paths/user-login {:username "user1" :password "test123"})]

            (log/response response)
      
            (test-valid-response-with-body response)

            (let [set-cookie-header (:Set-Cookie (clojure.walk/keywordize-keys (:headers response)))]

              (is (not (nil? set-cookie-header)))
              (is (not (empty? set-cookie-header)))
              (is (not (empty? (nth set-cookie-header 0))))
              (is (.contains (nth set-cookie-header 0) chttp/session-cookie-prefix))

              (let [auth-token (get-auth-token response)]

                (is (not (empty? auth-token)))

                (test-users-logged-in auth-token))))))


(defn test-cart-logged-in [token]

  (log/test-name "cart-get, authorized, cart empty")
  (let [response (req-get-auth paths/cart-get token)]
    (log/response response)

    (test-valid-response response)

    (let [body (cheshire.core/parse-string (:body response) true)]

      (test-valid-body body)
         
      (test-success-body body)      ;for now we just return empty cart
                                        ;for unknown ids
      (is (contains? body :cart))
                                        ;(is (not (empty? (:cart body))))
      (is (= (count (:cart body)) 0))))

  
     (log/test-name "cart-add, authorized, cart empty")
     (let [response (req-post-auth paths/cart-add {:pid (index-to-db-id 0)} token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         (test-success-body body)
         )
       )

     (log/test-name "cart-get, authorized, one item")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         ;; (test-body-status body status/not-found))
         (test-success-body body) ;for now we just return empty cart
                                        ;for unknown ids
         (is (contains? body :cart))
                                        ;(is (not (empty? (:cart body))))
         (is (= (count (:cart body)) 1))

         (let [item (nth (:cart body) 0)]

           (log/debug (str "item:" item))
           
           (is (not (empty? item)))
           (is (= (:id item) (index-to-db-id 0)))
           (is (= (:qt item) 1))

           (is (not (empty? (:na item))))
           (is (not (empty? (:des item))))
           (is (not (empty? (:pic item))))
           (is (not (empty? (:pr item))))
           (is (not (empty? (:se item))))
           )
         )
       )

     (log/test-name "cart-add, authorized, one item with qt 1")
     (let [response (req-post-auth paths/cart-add {:pid (index-to-db-id 0)} token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         (test-success-body body)
         )
       )     
     
     (log/test-name "cart-get, authorized, one item with qt 2")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         ;; (test-body-status body 4)) ;not found ;TODO use same constants
         (test-success-body body) ;for now we just return empty cart
                                        ;for unknown ids
         (is (contains? body :cart))
                                        ;(is (not (empty? (:cart body))))
         (is (= (count (:cart body)) 1))

         (let [item (nth (:cart body) 0)]

           (log/debug (str "item:" item))
           
           (is (not (empty? item)))
           (is (= (:id item) (index-to-db-id 0)))
           (is (= (:qt item) 2))

           (is (not (empty? (:na item))))
           (is (not (empty? (:des item))))
           (is (not (empty? (:pic item))))
           (is (not (empty? (:pr item))))
           (is (not (empty? (:se item))))
           )
         )
       )

     (log/test-name "cart-add, authorized, item with different id")
     (let [response (req-post-auth paths/cart-add {:pid (index-to-db-id 1)} token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         (test-success-body body)
         )
       )

     (log/test-name "cart-get, authorized, 2 items - one with qt 2 other qt 1")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         ;; (test-body-status body status/not-found))
         (test-success-body body) ;for now we just return empty cart
                                        ;for unknown ids
         (is (contains? body :cart))
                                        ;(is (not (empty? (:cart body))))
         (is (= (count (:cart body)) 2))

         (let [item (nth (:cart body) 0)]

           (log/debug (str "item:" item))
           
           (is (not (empty? item)))
           (is (= (:id item) (index-to-db-id 0)))
           (is (= (:qt item) 2))

           (is (not (empty? (:na item))))
           (is (not (empty? (:des item))))
           (is (not (empty? (:pic item))))
           (is (not (empty? (:pr item))))
           (is (not (empty? (:se item))))
           )

         (let [item (nth (:cart body) 1)]

           (log/debug (str "item:" item))
           
           (is (not (empty? item)))
           (is (= (:id item) (index-to-db-id 1)))
           (is (= (:qt item) 1))

           (is (not (empty? (:na item))))
           (is (not (empty? (:des item))))
           (is (not (empty? (:pic item))))
           (is (not (empty? (:pr item))))
           (is (not (empty? (:se item))))
           )
         )
       )     
     
     (log/test-name "cart-quantity, authorized, set quantity")
     (let [response (req-post-auth paths/cart-quantity {:pid (index-to-db-id 0) :qt 3} token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         (test-success-body body)))

     (log/test-name "cart-get, authorized, checking quantity")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         ;; (test-body-status body 4)) ;not found ;TODO use same constants
         (test-success-body body) ;for now we just return empty cart
                                        ;for unknown ids
         (is (contains? body :cart))
         (is (= (count (:cart body)) 2))
         
         (let [item (nth (:cart body) 0)]
           (is (= (:qt item) 3)))))

     (log/test-name "cart-remove, authorized")
     (let [response (req-post-auth paths/cart-remove {:pid (index-to-db-id 0)} token)]
       (log/response response)

       (test-valid-response response)
       
       (let [body (cheshire.core/parse-string (:body response) true)]
         (test-valid-body body)
         (test-success-body body))
      )
     
     (log/test-name "cart-get, authorized, checking remove")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         (test-success-body body)
         
         (is (contains? body :cart))
         (is (= (count (:cart body)) 1))
         ))
     
     
     (log/test-name "cart-quantity, authorized, when product is not in the cart")
     (let [response (req-post-auth paths/cart-quantity {:pid (index-to-db-id 0) :qt 5} token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         (test-body-status body 1)))

     (log/test-name "cart-get, authorized, after set quantity of product that's not in the cart")
     (let [response (req-get-auth paths/cart-get token)]
       (log/response response)

       (test-valid-response response)

       (let [body (cheshire.core/parse-string (:body response) true)]

         (test-valid-body body)
         
         ;; (test-body-status body 4)) ;not found ;TODO use same constants
         (test-success-body body) ;for now we just return empty cart
                                        ;for unknown ids
         (is (contains? body :cart))
         (is (= (count (:cart body)) 2)) ;we have again 2 products
         
         (let [item (nth (:cart body) 1)]
           (is (= (:qt item) 5))))
       )     
     )




(deftest test-cart

  ;preconditions for tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (clear-db)
  (add-test-products)

  ;TODO remove? is this working? this should return not auth
  (let [response (req-get paths/user-get {:na "user1"})]
  
     (let [body (cheshire.core/parse-string (:body response) true)]
       (def test-user (:user body))))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   
   (testing "cart"

     (log/test-name "cart-get, not authenticated...")     
     (let [response (req-get paths/cart-get)]
       (log/response response)

       (test-valid-response-with-body response)
       (test-unauthorized response))

     (log/test-name "cart-add, not authenticated...")
     (let [response (req-post paths/cart-add {:pid (index-to-db-id 0)})]
       
       (log/response response)

       (test-valid-response-with-body response)
       (test-unauthorized response))



       ;tests with logged in user
       ;register and login a user
       (register-user1) ;TODO add the user in fill-db-with-test-data
       (log/test-name "loggin in the user...")
       (let [response (req-post paths/user-login {:username "user1" :password "test123"})
             auth-token (get-auth-token response)]

         (test-cart-logged-in auth-token))

     ))
