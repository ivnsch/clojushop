Clojure shop
=========

Online store backend/api written in Clojure


JSON api, currently supports user authentication, product list and cart.

Database is easily interchangeable thanks to data provider layer. Currently MongoDB is used.

This api is intended to be used by mobile apps or ajax applications.


Note that this is a learning project - use at your own risk.


##### Start server:
```
lein ring server-headless
```



###### Example curl requests:

Get products:

```
curl --request GET 'http://localhost:3000/products?st=0&sz=2'
```

Login (note use of cookie store - this will allow us to send next requests authenticated):

```
curl -i -H "Content-Type: application/json" -X POST -d '{"username":"user1", "password":"test123"}' http://localhost:3000/login  --cookie "cookies.txt" --cookie-jar "cookies.txt" --location --verbose
```


Get cart (authenticated request):

```
curl -i -H --request GET 'http://localhost:3000/cart-get'  --cookie "cookies.txt" --cookie-jar "cookies.txt" --location --verbose
```



##### Unit tests: 
```
lein test clojushop.test.handler
```

The unit tests in https://github.com/i-schuetz/clojushop/blob/master/test/clojushop/test/handler.clj do request calls and response processing like a normal client, thus can also help with further understanding about how to use the api.


##### Example client app (iOS):

https://github.com/i-schuetz/clojushop_client_ios




##### Api

Path  | Request type  | Authenticated  | Description  | Params
------------- | ------------- | ------------- | ------------- | -------------
/products  |  GET  | No |  Gets products list  | st: Page start, sz: Page size
/products/add  | POST | Yes  | Adds a product (current user) |  na: name, des: description, img: image, pr: price, se: seller
/products/edit  | POST | Yes | Edits a product (current user) | na: name, des: description, img: image, pr: price, se: seller (all optional, but at least 1 required)
/products/remove  | POST | Yes | Removes a product (current user) | id: product id
/cart  | GET | Yes | Gets cart (current user) |  
/cart/add  | POST | Yes | Adds a product to cart | id: product id 
/cart/remove  | POST | Yes | Removes a product from cart | id: product id 
/cart/quantity | POST | Yes | Sets the quantity of cart item (upsert) | id: product id, qt: quantity 
/user | GET | Yes | Gets current user | 
/user/register | POST | No | Registers a user | na: name, em: email, pw: password 
/user/edit | POST | Yes | Edits current user | na: name, em: email, pw: password (all optional, but at least 1 required)
/user/login | POST | No | Logs in a user | username: name, password: password 
/user/logout | GET | Yes | Logs out current user | 
/user/remove | GET | Yes | Removes current user |
/pay | POST | Yes | Executes payment and empties cart | to: credit card token, v: amount c: currency ISO code 



##### Status codes

All api responses have a status flag. Possible codes:

Code  | Meaning
------------- | -------------
0  | Unspecified error
1  | Success
2  | Wrong params
3  | Not found
4  | Validation error
5  | User already exists
6  | Login failed
7  | Not authenticated


Also, the results delivered by the dataprovider have status. Possible codes:

Code  | Meaning
------------- | -------------
0  | Unspecified error
1  | Success
2  | Bad id
3  | User already exists
4  | Not found
5  | Database internal error

The api and dataprovider status codes are independent of each other. The api handler (https://github.com/i-schuetz/clojushop/blob/master/src/clojushop/handler.clj) decides how to handle database result codes. Usually a map will be used to determine api code.


##### Image resolutions

When products are added to the database, the images have to be grouped in resolution categories. A resolution category is a simple numeric identifier. The meaning of the identifier is not fixed, and it depends where and how it's used.

Best explained with an example: Products are used in a list and a detailed view. List uses small images. Detail view uses big images. For testing we will use only 2 resolution categories - one will stand for "low" and the other for "high".

The img field of a product in the database would look like this (this implementation will be improved to avoid repeating the base path):

```
:img {
          :pl {
            :1 "http://ivanschuetz.com/img/cs/product_list/r1/blueberries.png" 
            :2 "http://ivanschuetz.com/img/cs/product_list/r2/blueberries.png"}
          :pd {
            :1 "http://ivanschuetz.com/img/cs/product_details/r1/blueberries.png" 
            :2 "http://ivanschuetz.com/img/cs/product_details/r2/blueberries.png"}
}
```

Here :pl stands for product-list and :pd for product-details. :1 is our low res category and :2 is high res (in a serious application we would use much more categories, e.g. specific for iPhone retina, Android (ranges), tablets, etc).

In total, we have 4 images. 2 use cases (list and details), and 2 available resolutions for each use case.

When the client makes a request to get items that contain images, it must send the screen size as "scsz" parameter. Example:
"scsz":"640x960"

In the function get-res-cat [screen-size] in our handler  (https://github.com/i-schuetz/clojushop/blob/master/src/clojushop/handler.clj), we map this screen size to a resolution category. The algorithm to do this can be anything - for demonstrative purposes, we use this:

    (if (< (Integer. width) 500) :1 :2)
    
This is, if the screen width is less than 500px we map this to resolution category 1 and if it's bigger to 2.

The items will then be filtered accordingly, such that the client gets only images suitable for their screensize. The image field of product in response would look like this:

```
"img":{"pd":"http://ivanschuetz.com/img/cs/product_details/r2/blueberries.png","pl":"http://ivanschuetz.com/img/cs/product_list/r2/blueberries.png"}
```


This is a very flexible implementation, since the client doesn't have to be modified when new resolutions are supported, and server can add arbitrary categories or new logic to determine which images fit best a certain screen size. Client only tells server e.g. "I want the images for the products list, and my screen size is 640x960", and uses whatever images the server delivers. This also works with orientation change, without additional changes - the client sends what currently is width and height and the server determines what fits best. It is not necessary to add additional identifiers for orientation or device type. Yet, orientation change opens the need for an improvement, namely that the client should not have to repeat the request only to get the images for the new orientation. This will probably be solved by calculating both resolutions categories interchanging width and height and send the client both images. In this case the request would keep the same, but the processing of the response would have to be adjusted.


##### Payment

Stripe is used as payment system. A Stripe user account is necessary to test payments. The Stripe secret key has to be inserted in the calls. There is a placeholder in handler.clj called "your_stripe_secret_key" for this.

The correspoding public key has to be inserted in the client.

Currently the app suppors a basic credit card payment, using a credit card token. The client application gets the credit card data from the user, sends it to Stripe's api to get credit cart token, and then sends the token to Clojushop, together with the transaction amount and currency. Clojushop, then, calls the Stripe api with this data in order to do the transaction. The transactions show immediately in Stripe's dashboard.


##### Currency

Currently, in order to allow maximal flexibility - and don't limit the api with premature decisions, each product is saved with a currency. This is still a field under construction - may be subject to change, since it's difficult to handle the payment. Currently, the stored currency identifiers are sent to the client, but the client can send only one currency to payment service.


##### TODOs

Needs lots of online shop relevant stuff, like SKUs (currently Mongo id is used as identifier - very bad!), stock/inventory, improved security, validation, internationalization, etc. And of course, more features!




#### License

Copyright (C) 2014 Ivan Schütz

Release under Apache License v2. See LICENSE.txt for the full license, at the top level of this repository.
