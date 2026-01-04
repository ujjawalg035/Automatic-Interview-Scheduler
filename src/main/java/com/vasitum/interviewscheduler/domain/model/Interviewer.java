package com.vasitum.interviewscheduler.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "interviewers")
public class Interviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Maximum interviews allowed per week for this interviewer.
     */
    @Column(nullable = false)
    private Integer maxWeeklyInterviews;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getMaxWeeklyInterviews() {
        return maxWeeklyInterviews;
    }

    public void setMaxWeeklyInterviews(Integer maxWeeklyInterviews) {
        this.maxWeeklyInterviews = maxWeeklyInterviews;
    }
}


