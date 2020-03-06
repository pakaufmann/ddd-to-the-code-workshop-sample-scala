# DDD-to-the-code: Scala edition

This is a reimplementation of <https://github.com/cstettler/ddd-to-the-code-workshop-sample/> in Scala.

## Disclaimer

Like the original project this implementation does not contain production-ready code.

## Implementation

- Favoring immutability wherever possible
- no runtime reflection, neither in domain nor in test code
- using higher-kinded type in the application- and domain-layer to abstract the concrete
  effect away
  - example: `com.github.pkaufmann.dddttc.rental.application.domain.booking.BookBikeService`
- domain describes all possible domain errors in the `Result` return type, no exceptions for domain errors
  - example: `com.github.pkaufmann.dddttc.rental.application.BookingService`
- Compile-time errors in case of missing definitions for published or subscribed events
  - example: `com.github.pkaufmann.dddttc.accounting.infrastructure.event`
- Compile-time errors for missing serialisations and deserialisations (using Circe)
  - example: `com.github.pkaufmann.dddttc.accounting.infrastructure.json`
- using Shapeless in the test code to create and manipulate (internal) domain objects
  - example: `com.github.pkaufmann.dddttc.registration.application.UserRegistrationServiceTests`
- using access-modifiers in Scala to hide domain methods from the infrastructure
  - example: `com.github.pkaufmann.dddttc.rental.application.domain.booking.Booking` method `completeBooking`
- Using doobie to do the transaction and database handling
- Using cats-effect to do the IO handling
- Using http4s, Blaze and Twirl for the web facing interfaces

## Demo

To start the application run the following commands:

- start registration: `sbt registration/run`
- start rental: `sbt rental/run`
- start accounting: `sbt accounting/run`

### Initial State
- list of existing bikes (<http://localhost:8083/rental/bikes>): all bikes available 
- list of wallets (<http://localhost:8082/accounting/wallets>): no wallets existing
- list of bookings (<http://localhost:8083/rental/bookings>): no bookings existing

### Register New User
- start user registration (<http://localhost:8081/user-registration/>)
- enter user handle (e.g. "peter")
- click "Next >" button
- read verification code from console of registration bounded context (6 digits code)
- enter verification code
- click "Next >" button
- enter first and last name (e.g. "Peter" and "Meier")
- click "Complete" button
- check for new wallet (<http://localhost:8082/accounting/wallets>) with initial amount

### Book Bike
- choose bike (<http://localhost:8083/rental/bikes>)
- enter user handle (e.g. "peter")
- click "Book" button
- check list of booking (<http://localhost:8083/rental/bookings>): new booking (still running)
- check list of bikes (<http://localhost:8083/rental/bikes>): chosen bike not available for booking
- wait some time

### Return Bike
- list bookings (<http://localhost:8083/rental/bookings>)
- click "Return Bike" button
- check list of booking (<http://localhost:8083/rental/bookings>): booking is ended
- check list of bikes (<http://localhost:8083/rental/bikes>): previously chosen bike again available
- check list of wallets (<http://localhost:8082/accounting/wallets>): booking fee deducted from wallet
