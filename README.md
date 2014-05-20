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



##### Status codes

All webservice responses have a status flag. Possible codes:

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

The webservice and dataprovider codes are completely independent in each other. The webservice layer (https://github.com/i-schuetz/clojushop/blob/master/src/clojushop/handler.clj) decides how to handle database result codes.



More documentation will follow soon!




Operations list: Get products (paginated), add product, remove product, edit product, register, login, logout, get user details, remove user, edit user, get cart, add to cart, remove from cart, change quantity in cart.
