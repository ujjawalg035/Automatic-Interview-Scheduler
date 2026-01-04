package com.vasitum.interviewscheduler.application.exception;

public class SlotFullyBookedException extends DomainException {

    public SlotFullyBookedException(Long slotId) {
        super("SLOT_FULLY_BOOKED", "Slot " + slotId + " is fully booked.");
    }
}


