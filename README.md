clojushop
=========

Online store backend/api written in Clojure


JSON api, currently supports user authentication, product list and cart.

Database is easily interchangeable thanks to data provider layer. Currently MongoDB is used.

Stripe will be used as payment system. Integration is in progress.

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

Note that the api is in early development. This is just to give an overview of what the api is supposed to do. There are many of functions, checks, parameters, etc. that are not implemented yet. Current parameters are likely to change soon / this list may be outdated.


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

The img element of a product in the database would look like this (this implementation will be improved to avoid repeating the base path):

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

Here :pl stands for product-list and :pd for product-details. :1 is our low res category and :2 is high res.

In total, we have 4 images. 2 use cases (list and details), and 2 available resolutions for each use case.

When we make a request to get items that contain images, we send the screen size as "scsz" parameter. Example:
"scsz":"640x960"

In the function get-res-cat [screen-size] in https://github.com/i-schuetz/clojushop/blob/master/src/clojushop/handler.clj, we determine which resolution category we want to map this screen size to. An oversimplified implementation for our 2 available categories could look like this:

    (if (< (Integer. width) 500) :1 :2)
    
This is, if the screen width is less than 500px we map this to resolution category 1 and if it's bigger to 2.

The items will then be filtered accordingly from the database, such that the client gets only images suitable for their screensize. The image element of product in response would look like this:

```
"img":{"pd":"http://ivanschuetz.com/img/cs/product_details/r2/blueberries.png","pl":"http://ivanschuetz.com/img/cs/product_list/r2/blueberries.png"}
```


This is a very flexible implementation, since the client doesn't have to be modified when new resolutions are supported, and server can add arbitrary categories or new logic to determine which images fit best a certain screen size. Client only tells server e.g. "I want the images for the products list, and my screen size is 640x960", and uses whatever images the server delivers. This also works with orientation change, without additional changes - the client sends what currently is width and height and the server determines what fits best. It is not necessary to add additional identifiers for orientation or device type. Yet, orientation change opens the need for an improvement, namely that the client should not have to repeat the request only to get the images for the new orientation. This will probably be solved by calculating both resolutions categories interchanging width and height and send the client both images. In this case the request would keep the same, but the processing of the response would have to be adjusted.








#### License

Copyright (C) 2014 Ivan Schütz

Release under Apache License v2. See LICENSE.txt for the full license, at the top level of this repository.
