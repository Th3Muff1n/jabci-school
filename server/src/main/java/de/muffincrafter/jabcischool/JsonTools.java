package de.muffincrafter.jabcischool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonTools {
    public static boolean checkJSON(String json) {
        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        if (obj.has("info") && obj.has("grades")) {
            JSONObject info = obj.getJSONObject("info");
            JSONArray grades = obj.getJSONArray("grades");
            if (info.has("student_name") && info.has("student_class") && info.has("certificate_year") && grades.length() >= 1) {
                return true;
            }
        }

        return false;
    }

    public void getInfo(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        JSONObject info = obj.getJSONObject("info");
        JSONArray grades = obj.getJSONArray("grades");

        String student_name = info.getString("student_name");
        String student_class = info.getString("student_class");
        String certificate_year = info.getString("certificate_year");

        List<Grade> student_grades = new ArrayList<>();
        for (Object grade : grades) {
            String subject_name = ((JSONObject)grade).getString("subject");
            String subject_grade = ((JSONObject)grade).getString("grade");
            student_grades.add(new Grade(subject_name, subject_grade));
        }
    }
}
