import org.json.JSONObject;

public class Main {
    static public void main(String[] Argv) {
        String input = new java.util.Scanner(System.in).useDelimiter("\\A").next();
        JSONObject obj = new JSONObject(input);

        String pingValue = obj.optString("ping");

        if (pingValue != null && pingValue.length() > 0) {
            System.out.println("{\"pong\":\"" + pingValue + "\"}");
        }

        System.out.println(Game.play(obj));
    }
}
