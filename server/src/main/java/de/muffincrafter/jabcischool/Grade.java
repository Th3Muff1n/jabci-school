package de.muffincrafter.jabcischool;

public class Grade {
    private final String grade;
    private final String subject;

    public Grade(String subject, String grade) {
        this.subject = subject;
        this.grade = grade;
    }

    public String getGrade() {
        return grade;
    }

    public String getSubject() {
        return subject;
    }
}
