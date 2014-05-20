clojushop
=========

Online store backend/api written in Clojure

Learning project. JSON api, currently supports user authentication, product list and cart.

Database is easily interchangeable thanks to data provider layer. This implementation uses MongoDB.


Stripe will be used as payment system. Integration is in progress.


This api is intended to be used by mobile apps or ajax applications.


Start server:
```
lein ring server-headless
```



Unit tests: 
```
lein test clojushop.test.handler
```




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

The webservice and dataprovider codes are completely independent in each other. The webservice layer decides how to react to database result codes.



Better documentation will follow soon!




Operations list: Get products (paginated), add product, remove product, edit product, register, login, logout, get user details, remove user, edit user, get cart, add to cart, remove from cart, change quantity in cart.
