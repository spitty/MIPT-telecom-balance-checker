package ru.spitty.telecom;

import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core of balance checking. It uses {@link Jsoup} class (from <a href="http://jsoup.org/">Jsoup</a> library) for obtaining
 * the result of POST request to <a href="http://cabinet.telecom.mipt.ru/">http://cabinet.telecom.mipt.ru/</a> 
 * and parsing.
 * 
 * @author spitty
 */
public class BalanceChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceChecker.class);
    private static final String ENTER_URL = "http://cabinet.telecom.mipt.ru/";
    
    private String login;
    private String password;

    /**
     * Public constructor store <code>login</code> and <code>password</code>
     * @param login {@link String} user login
     * @param password {@link String} user password
     */
    public BalanceChecker(String login, String password) {
        this.login = login;
        this.password = password;
    }
    
    /**
     * Authorizes with given credentials on <a href="http://cabinet.telecom.mipt.ru/">http://cabinet.telecom.mipt.ru/</a>
     * and returns a {@link Double} value of current balance.
     * @return
     * @throws IOException
     * @throws NumberFormatException 
     */
    public Double getBalance() throws IOException, NumberFormatException {
        Connection.Response res = Jsoup
                .connect(ENTER_URL)
                .data("login", login, "password", password)
                .method(Connection.Method.POST)
                .execute();
        Document doc = res.parse();
        Element table = doc.getElementsByClass("tab").first();
        Element balance = table.getElementsByTag("tr").get(1).getElementsByTag("td").get(1).child(0);
        // Now we have string like "100.00 руб"
        String balanceStr = balance.text().replaceFirst("^([0-9.]+).*", "$1");
        final Double doubleValue = Double.valueOf(balanceStr);
        return doubleValue;
    }

}
