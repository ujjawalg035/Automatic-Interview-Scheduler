package com.vasitum.interviewscheduler.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_candidate_slot", columnNames = {"candidateEmail", "slot_id"})
        })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private InterviewSlot slot;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String candidateEmail;

    /**
     * For simpler updates, we track confirmation as a boolean.
     */
    @Column(nullable = false)
    private boolean confirmed = true;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewSlot getSlot() {
        return slot;
    }

    public void setSlot(InterviewSlot slot) {
        this.slot = slot;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}


