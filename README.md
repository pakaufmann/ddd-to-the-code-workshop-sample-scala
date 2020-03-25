# DDD-to-the-code: Scala edition

This is a reimplementation of <https://github.com/cstettler/ddd-to-the-code-workshop-sample/> in Scala.

## Disclaimer

Like the original project this implementation does not contain production-ready code.

## Implementation

This implementation is using an alternative approach to the one in the "master" Branch.
Instead of using a more object-oriented approach with traits defining the external dependencies in
the domain and implementations of said traits providing the concrete instances, this
one uses only functions to implement all external dependencies.

The code is still divided between an application/domain part and an infrastructure part.
The domain part contains all relevant business logic, while the infrastructure part contains
all external dependencies, like persistence, web controllers etc.

Instead of the usual way of defining external dependencies as trait, this approach uses 
type definitions to define external dependencies as well as external interfaces as functions.

Example from the `UserRegistrationService` in `Registration`:

```
type VerifyPhoneNumber[F[_]] = (UserRegistrationId, VerificationCode) => Result[F, VerificationError, Unit]

def verifyPhoneNumber[F[_] : Monad]
(
getUser: UserRegistrationRepository.Get[F], updateUser: UserRegistrationRepository.Update[F]
): VerifyPhoneNumber[F] = {
...
}
```

The function takes all external dependencies as parameters and returns a function which
implements the interface `VerifyPhoneNumber`, which in turn is also a function, taking
a `UserRegistrationId` and a `VerificationCode` as input and return a `Result[F, VerificationError, Unit]`.

Dependencies like `UserRegistrationRepository.Get[F]` are also type definitions for functions.

From `UserRegistrationRepository`:

```
type Update[F[_]] = UserRegistration => Result[F, UserRegistrationNotExistingError, Unit]

type Get[F[_]] = UserRegistrationId => Result[F, UserRegistrationNotExistingError, UserRegistration]
```

Implementations for external dependencies lie in the `infrastructure` package. For the functions
above they lie in the `JdbcUserRegistrationRepository`.

Implementation for `UserRegistrationRepository.Get[F]`:

```
val get: UserRegistrationRepository.Get[ConnectionIO] = {
    userRegistrationId => {
      sql"SELECT data FROM user_registration WHERE id = $userRegistrationId"
        .query[UserRegistration]
        .option
        .map(_.toRight(UserRegistrationNotExistingError(userRegistrationId)))
        .asResult
    }
}
```

As this takes no external dependencies, the implementation can be a variable.

To tie everything together, the main class does the dependency injection.

```
val verifyPhoneNumber = UserRegistrationService.verifyPhoneNumber[ConnectionIO](
    JdbcUserRegistrationRepository.get,
    JdbcUserRegistrationRepository.update
)
```

External interfaces like web controllers are also functions, which take domain interface
functions as dependencies. Example from the `UserRegistrationController`:

```
def verify[F[_] : Sync](verify: UserRegistrationService.VerifyPhoneNumber[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
        ...
    }
```

Those then can be also tied together in the Main-function through dependency injection:

```
Server
    .create[IO](
        config.port,
        defaultErrorHandler[IO],
        "/user-registration" -> (
            UserRegistrationController.verify(verifyPhoneNumber.andThen(_.transact[IO]))
        )
    )
```

As seen here, concerns like transaction handling can be done on function level.
Other cross-cutting concerns, like logging, metrics or retrying can also be done on function level.

Example of the SendVerificationCodeEventHandler:

```
val sendVerificationHandler = SendVerificationCodeEventHandler
    .onPhoneNumberVerified[TraceReader[IO]#R](
      LoggingSmsNotificationSender.sendSmsTo[IO]
        .chaosMonkey(0.2)
        .logErrors(org.log4s.getLogger("SendSMSLogger"))
        .retry(3, 500.millis)
    )
    .measure(time => IO(println(s"Send verification had: $time ns")).liftTrace)
```

As seen, the `sendSmsTo` function has a `chaosMonkey`. This just simulates an error in 20%
of the cases. Also if the function fails errors are logged to the console and the function is automatically
retried for 3 times, in an interval of 500 milliseconds.

The function `onPhoneNumberVerified` is also measuered and the result also logged to the console.

All those concerns can be defined at injection time through function composition.

Testing of domain code is easily done, as dependencies in the tests can just
be injected directly into the constructor functions, no mocks required.

Example of a `UserRegistrationService` test for a successful `verifyPhoneNumber` call.
Also, as all code in the `application` package does only rely on `cats.Monad`
tests can be run with the `Id` monad, not relying on a `IO` or `SyncIO`
implementation.

```
it should "verify the phone number if an existing user registration and a valid verification code is provided" in {
    val registrationId = UserRegistrationId("user-registration-id-1")
    val verificationCode = VerificationCode("123456")
    val userRegistration = TestRegistrations.default
    
    val verify = UserRegistrationService.verifyPhoneNumber[Id](
      { case UserRegistrationId("user-registration-id-1") => Right(userRegistration).asResult[UserRegistrationNotExistingError, Id] },
      always(Right(()).asResult[UserRegistrationNotExistingError, Id])
    )
    
    val result = verify(registrationId, verificationCode)
    result.value.isRight shouldBe true
}
```

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
