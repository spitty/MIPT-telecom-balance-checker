/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.spitty.telecom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to send notification to user in cases when balance changes
 * dramatically or previous notification had been send too old.<br/>
 * Properties file must contains <code>login</code> and <code>password</code>.<br/>
 * For example:<br/>
 * <code>
 * login=123456<br/>
 * password=XXXXXX<br/>
 * </code>
 * <br/>
 * Properties file path is specified with system property <code>balanceChekerPropertiesFile</code>. For example:<br/>
 * <code>java -DbalanceChekerPropertiesFile=/tmp/balanceChecker.properties -jar %JAR_NAME%</code>
 *
 * @author spitty
 */
public class BalanceNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceNotifier.class);
    private static final String DEFAULT_PROPERTIES_FILE = ".balance_checker/checker.properties";
    //
    private static final String BALANCE_CHEKER_PROPERTIES_FILE = "balanceChekerPropertiesFile";
    private static final String LOGIN_PROP_KEY = "login";
    private static final String PASSWORD_PROP_KEY = "password";
    private static final String NOTIFICATION_STEP_PROP_KEY = "notification_step";
    private static final String NOTIFICATION_TIMEOUT_PROP_KEY = "notification_timeout";
    private static final String LAST_CHEKED_VALUE_PROP_KEY = "last_cheked_value";
    private static final String LAST_CHECK_TIME_PROP_KEY = "last_check_time";
    //
    private static final String DEFAULT_NOTIFICATION_STEP = "10";
    private static final String DEFAULT_NOTIFICATION_TIMEOUT = "3600000";

    private Properties prop;
    private String propFilename;
    private BalanceChecker balanceChecker;

    public static void main(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            showHelp();
            return;
        }

        BalanceNotifier balanceNotifier = new BalanceNotifier();
        try {
            balanceNotifier.process();
        } catch (IOException e) {
            LOGGER.error("Some error occurs", e);
        }
    }

    private static void showHelp() {
        System.out.println("Usage: java [ -D" + BALANCE_CHEKER_PROPERTIES_FILE + "=%PROPERTIES_FILE_PATH% ] -jar %JAR_NAME%\n"
                + "Where:\n"
                + "  %PROPERTIES_FILE_PATH% - path to properties file. It must contain at least two properties:\n"
                + "        login and password.\n"
                + "        For example:\n"
                + "          login=123456\n"
                + "          password=XXXXXX");
    }

    public BalanceNotifier() {
        prop = new Properties();
        loadProperties();
        String login = prop.getProperty(LOGIN_PROP_KEY);
        String password = prop.getProperty(PASSWORD_PROP_KEY);
        if (login == null || password == null) {
            LOGGER.error("Please specify login and password by properties in \"{}\"", new File(propFilename).getAbsolutePath());
            throw new IllegalArgumentException("Login or password is not set in \"" + new File(propFilename).getAbsolutePath() + "\"");
        }
        balanceChecker = new BalanceChecker(login, password);
    }

    /**
     * It is used to check current balance and show a notification if 
     * required conditions are passed.
     * @throws IOException if some error occurs
     */
    public void process() throws IOException {
        doCheck();
        storeProperties();
    }

    private void loadProperties() {
        propFilename = System.getProperty(BALANCE_CHEKER_PROPERTIES_FILE);
        if (propFilename == null) {
            propFilename = System.getProperty("user.home") + "/" + DEFAULT_PROPERTIES_FILE;
            LOGGER.info("Property -DbalanceChekerPropertiesFile is not set. Use default file path \"{}\"", propFilename);
        }
        File propFile = new File(propFilename);
        if (propFile.exists()) {
            try (FileReader fileReader = new FileReader(propFile)) {
                prop.load(fileReader);
            } catch (IOException e) {
                LOGGER.error("Can't load properties from file {}", propFilename, e);
            }
        }
    }

    private void storeProperties() throws IOException {
        File f = new File(propFilename);
        if (!f.exists()) {
            LOGGER.debug("Properties file absents. Create it {}", f.getAbsolutePath());
            File parent = f.getParentFile();
            parent.mkdirs();
            f.createNewFile();
        }
        try (FileWriter fw = new FileWriter(f)) {
            prop.store(fw, "Balance checker properties");
        }
        LOGGER.debug("Properties successfully written to {}", f.getAbsolutePath());
    }

    private void doCheck() throws IOException {

        Double balance = balanceChecker.getBalance();
        Long currentTime = System.currentTimeMillis();

        if (!prop.containsKey(LAST_CHEKED_VALUE_PROP_KEY) || !prop.containsKey(LAST_CHECK_TIME_PROP_KEY)) {
            LOGGER.info("There is not previous check result found");
            notifyAndStore(balance, currentTime);
            return;
        }

        Double lastCheckedValue = Double.valueOf(prop.getProperty(LAST_CHEKED_VALUE_PROP_KEY));
        Long lastCheckTime = Long.valueOf(prop.getProperty(LAST_CHECK_TIME_PROP_KEY));

        // Get notification difference value
        if (!prop.containsKey(NOTIFICATION_STEP_PROP_KEY)) {
            LOGGER.debug("Fill value for \"{}\" with default value \"{}\"", NOTIFICATION_STEP_PROP_KEY, DEFAULT_NOTIFICATION_STEP);

            prop.put(NOTIFICATION_STEP_PROP_KEY, DEFAULT_NOTIFICATION_STEP);
        }
        Double notificationStep = Double.valueOf(prop.getProperty(NOTIFICATION_STEP_PROP_KEY));

        // Get timeout 
        if (!prop.containsKey(NOTIFICATION_TIMEOUT_PROP_KEY)) {
            LOGGER.debug("Fill value for \"{}\" with default value \"{}\"", NOTIFICATION_TIMEOUT_PROP_KEY, DEFAULT_NOTIFICATION_TIMEOUT);
            prop.put(NOTIFICATION_TIMEOUT_PROP_KEY, DEFAULT_NOTIFICATION_TIMEOUT);
        }
        Long notificationTimeout = Long.valueOf(prop.getProperty(NOTIFICATION_TIMEOUT_PROP_KEY));
        final double limitValue = lastCheckedValue - notificationStep;
        final long limitTime = lastCheckTime + notificationTimeout;

        if (balance <= limitValue
                || currentTime >= limitTime) {
            if (balance <= limitValue) {
                LOGGER.info("Balance value \"{}\" exceeded limit value \"{}\"", balance, limitValue);
            } else {
                LOGGER.info("Time value \"{}\" exceeded limit time value \"{}\"", currentTime, limitTime);
            }
            notifyAndStore(balance, currentTime);
            return;
        }
        LOGGER.info("Given values (Balance = {}, Time = {}) do not exceed "
                + "limit values (Limit balance = {}, Limit time = {})",
                balance, currentTime, limitValue, limitTime);
    }

    private void notifyAndStore(Double balance, Long currentTime) {
        prop.put(LAST_CHEKED_VALUE_PROP_KEY, String.valueOf(balance));
        prop.put(LAST_CHECK_TIME_PROP_KEY, String.valueOf(currentTime));
        NotifySender.sendNotification("Current balance : " + balance);
    }
}
