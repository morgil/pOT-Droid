/*
 * Copyright (C) 2012 mods.de community 
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this software, and changing it is allowed as long as the 
 * name is changed.
 *
 *           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO. 
 */

package com.mde.potdroid.helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

/**
 * All the interaction with the website (login, login check, xml document
 * fetching, calling of sites, edit/write post, getting user id) is done by this
 * class. mHttpClient is the HttpClient which has to be used if the cookies
 * should be recognized by the website.
 * 
 * Instances of this class are saved in the member self.mWebsiteInteraction in
 * activities and can be fetched by calling
 * PotUtils.getWebsiteInteractionInstance()
 */
public class WebsiteInteraction {

    private DefaultHttpClient mHttpClient;
    private Context           mContext;
    private SharedPreferences mSettings;

    public WebsiteInteraction(Context context) {
        mContext    = context;
        mSettings   = PreferenceManager.getDefaultSharedPreferences(mContext);
        mHttpClient = new DefaultHttpClient();
        mHttpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                "Apache-HttpClient/potdroid " + mSettings.getString("unique_uagent", "potdroid"));

        // check if login cookie exists. If so, attach it to the
        // http client
        if (mSettings.contains("cookie_name")) {
            BasicClientCookie cookie = new BasicClientCookie(mSettings.getString("cookie_name",
                    null), mSettings.getString("cookie_value", null));
            cookie.setPath(mSettings.getString("cookie_path", null));
            cookie.setDomain(mSettings.getString("cookie_url", null));
            mHttpClient.getCookieStore().addCookie(cookie);
        }
    }

    /**
     * Get a xml document from the mods.de api
     * @throws NoConnectionException 
     */
    public Document getDocument(String url) throws NoConnectionException {
        Document document;
        
        // no internet connection...
        if (getConnectionType(mContext) == 0) {
            throw new NoConnectionException();
        }

        // our xml parser
        SAXBuilder parser = new SAXBuilder();

        try {
            // get the input stream from fetchContent().
            // return null if the fetching of the document failed
            HttpGet request = new HttpGet(url);
            request.addHeader("Accept-Encoding", "gzip");

            HttpResponse response = mHttpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if ((entity == null) || !entity.isStreaming()) {
                throw new NoConnectionException();
            }
            
            // get the content input stream and take care of gzip encoding
            InputStream instream = entity.getContent();
            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                instream = new GZIPInputStream(instream);
            }

            // build the xml document object
            document = parser.build(instream);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NoConnectionException();
        }
        return document;
    }

    // login
    public Boolean login(String password) throws Exception {

        // first, create new user agent
        // and recreate the httpclient
        SecureRandom random = new SecureRandom();
        String uAgent = new BigInteger(50, random).toString(32);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("unique_uagent", uAgent);
        editor.commit();
        mHttpClient = new DefaultHttpClient();
        mHttpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                "Apache-HttpClient/potdroid " + mSettings.getString("unique_uagent", "potdroid"));

        // add login data
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        String username = mSettings.getString("user_name", "");
        if (username.equals("") || password.equals("")) {
            return false;
        }
        nvps.add(new BasicNameValuePair("login_username", username));
        nvps.add(new BasicNameValuePair("login_password", password));
        nvps.add(new BasicNameValuePair("login_lifetime", PotUtils.COOKIE_LIFETIME));

        // create the request
        HttpPost httpost = new HttpPost(PotUtils.LOGIN_URL);
        httpost.setEntity(new UrlEncodedFormEntity(nvps, PotUtils.DEFAULT_ENCODING));

        // execute the form
        HttpResponse response = mHttpClient.execute(httpost);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity()
                .getContent(), PotUtils.DEFAULT_ENCODING));

        // fetch the result of the http request and save it as a string
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String input = sb.toString();

        // check if the login worked, e.g. one was redirected to SSO.php..
        Pattern pattern = Pattern.compile("http://forum.mods.de/SSO.php\\?UID=([0-9]+)[^']*");
        Matcher m = pattern.matcher(input);
        
        if (m.find()) {
            // set user id
            editor.putInt("user_id", Integer.valueOf(m.group(1)));
            editor.commit();
            
            // url for the setcookie found, send a request
            HttpGet cookieUrl = new HttpGet(m.group(0));
            
            mHttpClient.execute(cookieUrl);

            // store cookie data
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("MDESID")) {
                    editor.putString("cookie_name", cookie.getName());
                    editor.putString("cookie_value", cookie.getValue());
                    editor.putString("cookie_url", cookie.getDomain());
                    editor.putString("cookie_path", cookie.getPath());
                    editor.commit();
                }
            }
            return true;
        }

        return false;
    }

    // 0 -> not connected
    // 1 -> wifi
    // 2 -> else
    public static int getConnectionType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return 0;
        }

        if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return 1;
        }

        return 2;
    }
    
    /**
     * Sends a Post request to the website.
     */
    public Boolean sendPost(String url, List<NameValuePair> params) {
        HttpPost httppost = new HttpPost(url);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, PotUtils.DEFAULT_ENCODING));
            mHttpClient.execute(httppost);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
    }
    
    /**
     * This function just makes a normal get request and returns the html
     * result as a string
     */
    public String callPage(String url) {
        HttpGet req = new HttpGet(url);
        req.addHeader("Accept-Encoding", "gzip");
        
        HttpResponse response;
        try {
            response = mHttpClient.execute(req);
            
            // get the content input stream and take care of gzip encoding
            InputStream instream   = response.getEntity().getContent();
            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                instream = new GZIPInputStream(instream);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, PotUtils.DEFAULT_ENCODING));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String input = sb.toString();
            return input;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * This is the exception that is thrown when internet connection fails.
     */
    public class NoConnectionException extends Exception {
        private static final long serialVersionUID = 1L;

        public NoConnectionException() {
            super("No internet connection!");
        }
    }
}
