package com.opensource.rct.storage;

import com.opensource.rct.application.ConfigParameter;
import com.opensource.rct.model.MagicNumber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Component
public class Influxdb {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private ConfigParameter config;

    public void postData2Influx(long timestamp, String database, String measurement, String tag, double value)    //tags in Influx are the dimensions
    {
        final String url = "http://" + config.getHostnameInfluxDb() + ":" + config.getPortInfluxDb() + "/write?db=" + database + "&precision=s";

        tag = tag.replaceAll(" ", "\\\\ ");    //Influx needs to escape spaces in tags with a single \ to have it correctly we need double escape the java character
        String postString = measurement + ",dimension=" + tag + " value=" + value + " " + timestamp;

        try {
            HttpURLConnection HttpSessionToken = (HttpURLConnection) new URL(url).openConnection();

            HttpSessionToken.setRequestMethod("POST");
            HttpSessionToken.setDoOutput(true);
            HttpSessionToken.setRequestProperty("Accept", "*/*");

            //Preparing the output stream for POST
            OutputStream postContent = (OutputStream) HttpSessionToken.getOutputStream();
            postContent.write(postString.getBytes("UTF-8"));
            postContent.flush();
            postContent.close();

            int returnCode = HttpSessionToken.getResponseCode();

            if (returnCode == 204)    //Influx DB sends a 204 and not a 200 upon success
            {
                logger.debug("<<<<< DEBUG >>>>> Influxdb::postData2Influx Data sent successfully to Influx DB. Measurement {}, dimension {}, value {}, time {}", measurement, tag, value, timestamp);
            } else {
                logger.debug("<<<<< ERROR >>>>> Influxdb::postData2Influx sending data {} to Influx DB {} returned {}", postContent, url, returnCode);
            }
        } catch (IOException e) {
            logger.error("<<<<< ERROR >>>>> Influxdb::postData2Influx Execption when communicating to server. " + e.getMessage());
        }
    }

    /**
     * Method to initialize the Influx DB.
     * <p>
     * Cycles through the Hashmap containing all magic numbers with their configuration data. For each magic number which should be stored
     * in influx a database needs to be created. The database names are give in that configuration data.
     * <p>
     * returns true if at least one database in Influx was created successfully, returns false if no database was created.
     */
    public boolean initInfluxDB() {
        logger.debug("initInfluxDB");
        List<String> listOfDatabases = new LinkedList<String>();

        for (HashMap.Entry<String, MagicNumber> entry : MagicNumber.magicNumberObjectMap.entrySet()) {
            if (entry.getValue().getDatabaseName() != null && !entry.getValue().getDatabaseName().trim().equalsIgnoreCase("")) {
                String dbName = entry.getValue().getDatabaseName().trim();
                if (!listOfDatabases.contains(dbName)) {
                    final String url = "http://" + config.getHostnameInfluxDb() + ":" + config.getPortInfluxDb() + "/query?q=create%20database%20" + dbName;

                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();

                        urlConnection.setRequestMethod("POST");
                        urlConnection.setDoOutput(true);
                        urlConnection.setRequestProperty("Accept", "*/*");

                        //Preparing the output stream for POST
                        OutputStream postContent = urlConnection.getOutputStream();
                        postContent.flush();
                        postContent.close();

                        int returnCode = urlConnection.getResponseCode();

                        if (returnCode == 200) {
                            logger.debug("<<<<< DEBUG >>>>> Influxdb::initInfluxDB Database {} successfully created.", dbName);
                            listOfDatabases.add(dbName);
                        } else {
                            logger.error("<<<<< ERROR >>>>> Influxdb::initInfluxDB Creating database {} with URL {} returned {}", dbName, url, returnCode);
                        }
                    } catch (IOException e) {
                        logger.error("<<<<< ERROR >>>>> Influxdb::postData2Influx Execption when communicating to server. " + e.getMessage());
                    }
                }
            }

        }
        return listOfDatabases.size() != 0;
    }

}
