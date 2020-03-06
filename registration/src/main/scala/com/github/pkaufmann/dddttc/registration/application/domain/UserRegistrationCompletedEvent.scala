package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.DomainEvent

@DomainEvent
case class UserRegistrationCompletedEvent private(userRegistrationId: UserRegistrationId, userHandle: UserHandle, phoneNumber: PhoneNumber, fullName: FullName)

private[domain] object UserRegistrationCompletedEvent
