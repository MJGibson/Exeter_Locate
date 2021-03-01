package com.riba2reality.wifimapper;

import android.os.AsyncTask;

import com.riba2reality.wifimapper.DataStores.ServerMessage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class PostToServer extends AsyncTask<String, String, String> {

    // class variables

    private final WeakReference<TrackerScanner> trackerscannerContainer;

    //public InputStream is;
    private final WeakReference<InputStream> inputSteamContrainer;

    //private final WeakReference<ServerMessage> serverMessageContrainer;
    private final ServerMessage serverMessage;


    //##############################################################################################
    // functions

    //==============================================================================================
    public PostToServer(TrackerScanner trackerScanner,
                        InputStream is,
                        ServerMessage serverMessage
                        ) {

        this.trackerscannerContainer = new WeakReference<>(trackerScanner);
        this.inputSteamContrainer = new WeakReference<>(is);
        //this.serverMessageContrainer = new WeakReference<>(serverMessage);
        this.serverMessage = serverMessage;

    }
    //==============================================================================================

    //==============================================================================================
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (this.trackerscannerContainer != null) {
            this.trackerscannerContainer.get().sendResult(result);
            this.trackerscannerContainer.get().sendSingleScanResult();
        }


    }
    //==============================================================================================


    //==============================================================================================
    @Override
    protected String doInBackground(String... params) {

        //---------------------------------------------------------------------
        int i = 0;
        //final String address = params[i++];
        //String protocol = params[i++];

        //String useSSLString = params[i++];
        //boolean useSSL = Boolean.parseBoolean(useSSLString);


        //String port = "";


        //String endpoint = "/";

        //String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------
        // check if Result null

//        String message = this.serverMessageContrainer.get().message;
//        String urlString = this.serverMessageContrainer.get().urlString;
//        boolean useSSL = this.serverMessageContrainer.get().useSSL;
//        final String address = this.serverMessageContrainer.get().address;
        String message = this.serverMessage.message;
        String urlString = this.serverMessage.urlString;
        boolean useSSL = this.serverMessage.useSSL;
        final String address = this.serverMessage.address;



        //------------------------------------------------------------------




        //------------------------------------------------------------------

        final String method = "POST";

        try {

            //------------------------------------------------------------------

            //System.out.println(System.getProperty("user.dir"));


            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt

            //InputStream caInput = new BufferedInputStream(new FileInputStream("cert.pem"));

            //InputStream is = this.getResources().openRawResource(R.raw.sample);
            //BufferedReader reader = new BufferedReader(new InputStreamReader(is));


            //InputStream caInput = new BufferedInputStream(new FileInputStream("cert.pem"));
            InputStream caInput = new BufferedInputStream(this.inputSteamContrainer.get());


            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                //System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            //---------------------------------


            // Create an HostnameVerifier that hardwires the expected hostname.
            // Note that is different than the URL's hostname:
            // example.com versus example.org
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    //new StrictHostnameVerifier();

                    //return hv.verify(address, session);

                    //return hv.verify("192.168.0.10", session);


                    //return hv.verify("82.46.100.70", session);

                    //System.out.println(hostname);

                    //hostname.equals("192.168.0.10") ||
                    //        hostname.equals("10.0.2.2")
                    return hostname.equals(address);


                }
            };


            //---------------------------------
            BufferedReader reader = null;
            String uri = urlString;

            //---------


//            if (method.equals("GET")) {
//                uri += "?" + this.getEncodedParams(parameters);
//                //As mentioned before, this only executes if the request method has been
//                //set to GET
//            }

            try {
                URL url = new URL(uri);
                //HttpURLConnection con = (HttpURLConnection) url.openConnection();

                HttpURLConnection con;
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod(method);


                // set time outs
                con.setReadTimeout(5000);
                con.setConnectTimeout(5000);

                if (useSSL) {
                    // set certificate
                    ((HttpsURLConnection) con).setSSLSocketFactory(context.getSocketFactory());

                    // set host name
                    ((HttpsURLConnection) con).setHostnameVerifier(hostnameVerifier);


                }


                if (method.equals("POST")) {
                    con.setDoOutput(true);
                    OutputStreamWriter writer =
                            new OutputStreamWriter(con.getOutputStream());
                    //writer.write(this.getEncodedParams(parameters));

                    //writer.write(new JSONObject(parameters).toString());
                    writer.write(message);

                    writer.flush();
                }

                StringBuilder sb = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String line;

                //System.out.println("blah");

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    //System.out.println(line);
                }

                //System.out.println("blah...");


                return sb.toString();

            } catch (Exception e) {


                // put it back in the queue
//                this.trackerscannerContainer.get().resendQueue.add(
//                        this.serverMessageContrainer.get()
//                );
                this.trackerscannerContainer.get().resendQueue.add( serverMessage );


                e.printStackTrace();
                //return null;
                return "Exception: " + e.getMessage();
            } finally {
                if (reader != null) {
                    try {

                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }


            //return "someting";
        } catch (Exception e) {

            // put it back in the queue
            //this.trackerscannerContainer.get().resendQueue.add(this.serverMessageContrainer.get());
            this.trackerscannerContainer.get().resendQueue.add( serverMessage );

            System.out.println(e.getMessage());
            return "Exception: " + e.getMessage();
        }
    }// end of do in background method
    //==============================================================================================

    //==============================================================================================
    //The method below is only called if the request method has been set to GET
    //GET requests sends data in the url and it has to be encoded correctly in order
    //for the server to understand the request. This method encodes the data in the
    //params variable so that the server can understand the request
    public String getEncodedParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            String value;
            //try {
            //value = URLEncoder.encode(params.get(key), "UTF-8");
            value = params.get(key);
            //} catch (UnsupportedEncodingException e) {
            //    e.printStackTrace();
            //}

            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }
    //==============================================================================================


}//end of Class
