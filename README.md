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



More documentation will follow soon!





#### License

Copyright (C) 2014 Ivan Schütz

Release under Apache License v2. See LICENSE.txt for the full license, at the top level of this repository.
