clojushop
=========

Online store backend/api written in Clojure

Learning project. JSON api, currently supports user authentication, product list and cart.

Database is easily interchangeable thanks to data provider layer. This implementation uses MongoDB.


Stripe will be used as payment system. Integration is in progress.


This api is intended to be used by mobile apps or ajax applications.



Unit tests: lein test clojushop.test.handler




Webservice responses have JSON body, with a status flag. Possible codes:

------------- | -------------
0  | Content Cell
1  | Content Cell
1  | Content Cell
1  | Content Cell
1  | Content Cell


Better documentation will follow soon!




Operations list: Get products (paginated), add product, remove product, edit product, register, login, logout, get user details, remove user, edit user, get cart, add to cart, remove from cart, change quantity in cart.
