package ru.spitty.telecom;

import java.io.Console;
import java.io.IOException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner which is used to parse CLI arguments with 
 * <a href="http://commons.apache.org/proper/commons-cli/">Apache Commons CLI</a>
 * and configure {@link BalanceNotifier}
 * 
 * @author spitty
 */
public class BalanceNotifierRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceNotifierRunner.class);
    private static final String DEFAULT_PROPERTIES_FILE = ".balance_checker/checker.properties";
    private static final String LOGIN_OPTION_NAME = "login";
    private static final String PASSWORD_OPTION_NAME = "password";
    private static final String PROP_FILE_OPTION_NAME = "propFile";
    private static final String HELP_OPTION_NAME = "help";

    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption(LOGIN_OPTION_NAME, true, "login to connect");
            options.addOption(PASSWORD_OPTION_NAME, true, "password to connect");
            options.addOption(PROP_FILE_OPTION_NAME, true, "properties file. Default is ~/" + DEFAULT_PROPERTIES_FILE);
            options.addOption(HELP_OPTION_NAME, false, "show this help");

            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(HELP_OPTION_NAME)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar MIPT_telecom_balance_checker.jar", options);
                return;
            }

            String propFileName = cmd.getOptionValue(PROP_FILE_OPTION_NAME, System.getProperty("user.home") + "/" + DEFAULT_PROPERTIES_FILE);

            BalanceNotifier balanceNotifier;
            if (cmd.hasOption(LOGIN_OPTION_NAME)) {
                String login = cmd.getOptionValue(LOGIN_OPTION_NAME);
                String password;
                if (cmd.hasOption(PASSWORD_OPTION_NAME)) {
                    password = cmd.getOptionValue(PASSWORD_OPTION_NAME);
                } else {
                    Console cons;
                    char[] passwd;
                    if ((cons = System.console()) != null
                            && (passwd = cons.readPassword("%s", "Password:")) != null) {
                        password = new String(passwd);
                    } else {
                        LOGGER.error("Can't get password");
                        throw new IllegalArgumentException("Can't get password");
                    }
                }
                balanceNotifier = new BalanceNotifier(propFileName, login, password);
            } else {
                balanceNotifier = new BalanceNotifier(propFileName);
            }
            balanceNotifier.process();

        } catch (ParseException ex) {
            LOGGER.error("CLI arguments parsing error", ex);
        } catch (IOException ex) {
            LOGGER.error("Error during balance check", ex);
        }
    }
}
