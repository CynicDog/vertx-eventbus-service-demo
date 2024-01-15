package example;

import example.resolver.RxVertxParameterResolver;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class, RxVertxParameterResolver.class})
public class RxSensorDataClientTest {

    private example.reactivex.SensorDataService rxDataService;

    // TODO: Event publishing point to be moved somewhere else...
    @BeforeEach
    void prepare(Vertx vertx, VertxTestContext ctx) {
        vertx.deployVerticle(new DataVerticle(), ctx.succeeding(id -> {
            rxDataService = SensorDataService.createProxy(vertx, "sensor.data-service");

            JsonObject m1 = new JsonObject()
                    .put("id", "abc").put("temp", 21.0d);
            JsonObject m2 = new JsonObject()
                    .put("id", "def").put("temp", 23.0d);

            vertx.eventBus()
                    .publish("sensor.updates", m1)
                    .publish("sensor.updates", m2);

            ctx.completeNow();
        }));
    }

    @Test
    void withSensors(VertxTestContext ctx) {
        Checkpoint getValue = ctx.checkpoint();
        Checkpoint goodAvg = ctx.checkpoint();

        { // Testing on `rxValueFor(String sensorId)`
            Single<JsonObject> value = rxDataService.rxValueFor("abc");
            TestObserver<JsonObject> testObserver_1 = value.test();

            // Wait for the observable to emit a value or complete
            ctx.verify(() -> testObserver_1.awaitTerminalEvent());

            testObserver_1.assertValue(payload ->
                    payload.getString("sensorId").equals("abc") && payload.getDouble("value").equals(21.0d)
            );
            getValue.flag();
        }

        { // Testing on `rxAverage()`
            Single<JsonObject> avg = rxDataService.rxAverage();
            TestObserver<JsonObject> testObserver_2 = avg.test();

            // Wait for the observable to emit a value or complete
            ctx.verify(() -> testObserver_2.awaitTerminalEvent());

            testObserver_2.assertValue(payload -> payload.getDouble("average").equals(22.0d));
            goodAvg.flag();
        }

        ctx.completeNow();
    }
}
