package example;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DataVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        new ServiceBinder(vertx)
                .setAddress("sensor.data-service")
                .register(SensorDataService.class, SensorDataService.create(vertx));
    }
}
