package de.muffincrafter.jabcischool.client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Scanner;

public class Certificate {
    public String getStudent_name() {
        return student_name;
    }

    private String student_name;
    private String student_class;
    private String certificate_year;

    private Grade[] grades;

    public Certificate(String student_name, String student_class, String certificate_year, Grade[] grades) {
        this.student_name = student_name;
        this.student_class = student_class;
        this.certificate_year = certificate_year;

        this.grades = grades;
    }

    public String getJSONString() throws UnsupportedEncodingException {
        JSONObject obj = new JSONObject();

        JSONObject info = new JSONObject();
        info.put("student_name", student_name);
        info.put("student_class", student_class);
        info.put("certificate_year", certificate_year);

        JSONArray grades_array = new JSONArray();
        for (Grade grade : grades) {
            JSONObject grade_obj = new JSONObject();

            grade_obj.put("subject", grade.getSubject());
            grade_obj.put("grade", grade.getGrade());

            grades_array.put(grade_obj);
        }

        obj.put("info", info);
        obj.put("grades", grades_array);

        return obj.toString().replaceAll("\"", "\\\\\"");
    }

    public void printCertificate() {
        System.out.printf("Zeugnis des Schülers %s aus der Klasse %s von %s:%n", student_name, student_class, certificate_year);
        for (Grade grade : grades) {
            System.out.printf("%n\tFach : %s%n", grade.getSubject());
            System.out.printf("\tNote : %s%n", grade.getGrade());
        }
    }

    public static Certificate buildCertificate() {
        System.out.println("Infos zum Zeugnis:");
        Scanner scanner = new Scanner(System.in);

        System.out.print("\tName   : ");
        String student_name = scanner.nextLine();

        System.out.print("\tKlasse : ");
        String student_class = scanner.nextLine();

        System.out.print("\tJahr   : ");
        String certificate_year = scanner.nextLine();


        System.out.println("\nNoten des Schülers:");

        System.out.print("\tAnzahl : ");
        Grade[] grades;
        grades = new Grade[Integer.parseInt(scanner.nextLine())];

        for (int i = 0; i < grades.length; i++) {
            System.out.print("\n\tFach : ");
            String subject_name = scanner.nextLine();

            System.out.print("\tNote : ");
            String subject_grade = scanner.nextLine();

            grades[i] = new Grade(subject_name, subject_grade);
        }

        return new Certificate(student_name, student_class, certificate_year, grades);
    }

    public static Certificate certificateFromJSON(JSONObject json) {
        JSONObject info = json.getJSONObject("info");
        String student_name = info.getString("student_name");
        String student_class = info.getString("student_class");
        String certificate_year = info.getString("certificate_year");

        JSONArray grades_json = json.getJSONArray("grades");
        Grade[] grades = new Grade[grades_json.length()];
        for (int i = 0; i < grades.length; i++) {
            JSONObject grade_obj = grades_json.getJSONObject(i);
            String subject = grade_obj.getString("subject");
            String grade = grade_obj.getString("grade");
            grades[i] = new Grade(subject, grade);
        }

        return new Certificate(student_name, student_class, certificate_year, grades);
    }
}
