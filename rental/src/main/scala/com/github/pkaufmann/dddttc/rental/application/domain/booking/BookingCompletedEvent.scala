package com.github.pkaufmann.dddttc.rental.application.domain.booking

import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.stereotypes.DomainEvent

@DomainEvent
case class BookingCompletedEvent private(bookingId: BookingId, numberPlate: NumberPlate, userId: UserId, bikeUsage: BikeUsage)

private[domain] object BookingCompletedEvent