package de.neebs;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class VelocityTest {
    @Test
    public void testVelocity() {
        Map<String, Object> map = new HashMap<>();
        map.put("HELLO", "World");
        VelocityContext context = new VelocityContext(map);
        StringWriter stringWriter = new StringWriter();
        Velocity.evaluate(context, stringWriter, "#", "$hello");
        System.out.println(stringWriter.toString());
    }
}
