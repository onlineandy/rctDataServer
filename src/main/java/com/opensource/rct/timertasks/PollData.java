package com.opensource.rct.timertasks;

import com.opensource.rct.application.ConfigParameter;
import com.opensource.rct.application.Helper;
import com.opensource.rct.model.MagicNumber;
import com.opensource.rct.storage.Influxdb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;


/**
 * @author JR
 * <p>
 * Timer task to poll data regularly from inverter to store them in a database, currently only influx is supported. Data which is being polled
 * for is configured in Constants.magicNumbersToBeRead where this map is being is being filled from the csv with all magic numbers having a
 * measurement assigned.
 */
@Component
public class PollData {

    private static final Logger logger = LogManager.getLogger();
    private final ConfigParameter config;
    private final Influxdb influxdb;
    public boolean activeInfluxDB = false;

    public PollData(ConfigParameter config, Influxdb influxdb) {
        this.config = config;
        this.influxdb = influxdb;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void run() {
        logger.debug("<<<<< DEBUG >>>>> PollData::run Timer triggered.");

        for (String magicNumber : MagicNumber.magicNumbersToBeRead) {
            MagicNumber.magicNumberObjectMap.get(magicNumber).setDataReady(false);    //before requesting data set data ready to false

            byte[] requestBytes = Helper.buildRequestByteArrayDataLoggerShort(magicNumber);
            boolean newDataReceived = sendRequestToInverter(requestBytes, magicNumber); //finally communicate with inverter

            if (activeInfluxDB && newDataReceived) {
                logger.debug("<<<<< DEBUG >>>>> PollData::run Received new data to be sent to Influx DB.");
                influxdb.postData2Influx(
                        MagicNumber.magicNumberObjectMap.get(magicNumber).getDataJson().get("timestamp").getAsLong(),
                        MagicNumber.magicNumberObjectMap.get(magicNumber).getDatabaseName(),
                        MagicNumber.magicNumberObjectMap.get(magicNumber).getMeasurementName(),
                        MagicNumber.magicNumberObjectMap.get(magicNumber).getDescription(),
                        MagicNumber.magicNumberObjectMap.get(magicNumber).getDataJson().get("value").getAsFloat()
                );
            }
        }
    }


    private boolean sendRequestToInverter(byte[] inputByteArray, String magicNumber) {
        Socket inverterSocket = null;
        DataOutputStream dOut = null;


        try {
            inverterSocket = new Socket(config.getHostname(), config.getPort());
            dOut = new DataOutputStream(inverterSocket.getOutputStream());
            dOut.write(inputByteArray);
            dOut.flush();
            logger.debug("<<<<< DEBUG >>>>> PollData::sendRequestToInverter Data has been requested from inverter.");
        } catch (IOException e) {
            logger.error("<<<<< ERROR >>>>> PollData::sendRequestToInverter Cannot connect to inverter. Check hostname and port.");
        } finally {
            if (inverterSocket == null && dOut == null) {
                logger.debug("<<<<< ERROR >>>>> PollData::sendRequestToInverter Socket and stream are both null.");
                return false;
            }

            try {
                dOut.close();
            } catch (Exception e) {
                logger.error("<<<<< ERROR >>>>> PollData::sendRequestToInverter Cannot close data output stream." + e.getLocalizedMessage());
            } finally {
                try {
                    inverterSocket.close();
                } catch (Exception e) {
                    logger.error("<<<<< ERROR >>>>> PollData::sendRequestToInverter Cannot close TCP socket." + e.getMessage());
                }
            }
        }


        Long startWaitTime = Calendar.getInstance().getTimeInMillis();
        boolean requestTimeOut = false;

        while (!MagicNumber.magicNumberObjectMap.get(magicNumber).isDataReady() && !requestTimeOut) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("<<<<< ERROR >>>>> PollData::getData Thread sleep interrupted.");
                e.printStackTrace();
            }

            if (startWaitTime + config.getTimeoutConverter() < Calendar.getInstance().getTimeInMillis()) {
                requestTimeOut = true;
                logger.warn("<<<<< WARN >>>>> PollData::getRctData " + magicNumber + " timeout " + (Calendar.getInstance().getTimeInMillis() - startWaitTime));
            }
        }

        if (requestTimeOut) {
            return false;
        } else if (!MagicNumber.magicNumberObjectMap.get(magicNumber).getDataJson().get("sucess").getAsBoolean()) {
            return false;    //sanity check failed so don't write to InfluxDB
        } else {
            logger.debug("<<<<< DEBUG >>>>> PollData::getRctData Writing data to database. {}", MagicNumber.magicNumberObjectMap.get(magicNumber).getDataJson().toString());

            return true;
        }
    }
}
