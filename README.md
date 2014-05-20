clojushop
=========

Online store backend/api written in Clojure

This is a learning project - currently supports user authentication, product list and cart. It uses MongoDB as database but data provider layer allows to change this easily.



Stripe will be used as payment system. Integration is in progress.


This api is intended to be used by mobile apps or ajax applications.



Unit tests: lein test clojushop.test.handler



Better documentation will follow soon!




Operations list: Get products (paginated), add product, remove product, edit product, register, login, logout, get user details, remove user, edit user, get cart, add to cart, remove from cart, change quantity in cart.
