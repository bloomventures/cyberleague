package bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Scanner;

public class Bot {

    static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> run(Map<String, Object> input) {
        return Map.of("pong", input.get("ping"));
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
        }

        Map<String, Object> input = mapper.readValue(sb.toString(), Map.class);
        Map<String, Object> output = run(input);
        System.out.println(mapper.writeValueAsString(output));
    }
}
