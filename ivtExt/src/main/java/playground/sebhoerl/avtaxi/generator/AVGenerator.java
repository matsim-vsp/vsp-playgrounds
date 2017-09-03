package playground.sebhoerl.avtaxi.generator;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;

import java.util.Iterator;

import org.matsim.core.config.ConfigGroup;

public interface AVGenerator extends Iterator<AVVehicle> {
    interface AVGeneratorFactory {
        AVGenerator createGenerator(AVGeneratorConfig generatorConfig);
    }
}
