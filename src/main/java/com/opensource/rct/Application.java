package com.opensource.rct;

import com.opensource.rct.application.ConfigParameter;
import com.opensource.rct.application.Inverter;
import com.opensource.rct.model.MagicNumber;
import com.opensource.rct.timertasks.PollData;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Timer;

/**
 * @author JR
 * <p>
 * TODO:
 * - requestData servlet returns result as plain/text, somehow returning as JSON object throws an exception. Has to be cleaned up one day.
 * - getData für Strings implementieren
 * - building request strings, does escaping change the length variable?
 * - reconnect for inverter connection
 */


@SpringBootApplication
@EnableScheduling
public class Application {

    private static final Logger logger = LogManager.getLogger();
    private static final Timer timer = new Timer(true);

    @Autowired
    private ConfigParameter config;

    @Autowired
    private Inverter inverter;

    @Autowired
    private PollData pollData;

    @Autowired
    MeterRegistry meterRegistry;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        createMagicNumberObjects();
        logger.info("<<<<< INFO >>>>> Application::Main: RCT data server has been started. Going to connect to inverter...");
        inverter.connect();
        if (StringUtils.hasLength(config.getHostnameInfluxDb())) {
            logger.info("InfluxDB active.");
            pollData.activeInfluxDB = true;
        }
    }

    void createMagicNumberObjects() {
        InputStream is = Application.class.getResourceAsStream("/RCT_magic_numbers.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                String[] inputArray = line.split(";");

                MagicNumber magicNumberObject = new MagicNumber();

                if (inputArray.length >= 3) {
                    magicNumberObject.setMagicNumber(inputArray[0]);
                    magicNumberObject.setDescription(inputArray[1]);
                    magicNumberObject.setDataType(inputArray[2]);
                    if (inputArray.length >= 4) {
                        magicNumberObject.setDatabaseName(inputArray[3]);
                    }
                    if (inputArray.length == 5) {
                        magicNumberObject.setMeasurementName(inputArray[4]);
                        if (inputArray[4] != null && !inputArray[4].trim().equalsIgnoreCase("")) {
                            MagicNumber.magicNumbersToBeRead.add(inputArray[0]);    //add magic number to be polled for
                            Gauge.builder(getMetricName(magicNumberObject), magicNumberObject, m -> getValue(m)).register(meterRegistry);
                        }
                    }
                    MagicNumber.magicNumberObjectMap.put(inputArray[0], magicNumberObject);
                } else {
                    logger.error("<<<<< ERROR >>>>> Application::createMagicNumberObjects input line in CSV doesn't have the right amount of entries. " + line);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getValue(MagicNumber m) {
        return m.getDataJson() != null ? Math.round(m.getDataJson().get("value").getAsFloat()) : 0;
    }

    private String getMetricName(MagicNumber magicNumberObject) {
        String prefix = "rct";
        String name = magicNumberObject.getMeasurementName();
        if (Character.isLowerCase(name.charAt(0))) {
            prefix = "rct_";
        }
        return prefix + name.replaceAll("([A-Z])","_$1").toLowerCase();
    }
}
