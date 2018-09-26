package de.muffincrafter.jabcischool.client;

public class Grade {
    private final String subject;
    private final String grade;

    public Grade(String subject, String grade) {
        this.subject = subject;
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public String getGrade() {
        return grade;
    }
}
