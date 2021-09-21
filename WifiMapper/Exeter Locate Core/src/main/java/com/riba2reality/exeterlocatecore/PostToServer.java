package com.riba2reality.exeterlocatecore;

import android.os.AsyncTask;

import com.riba2reality.exeterlocatecore.DataStores.ServerMessage;

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
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class PostToServer extends AsyncTask<String, String, String> {

    // class variables

    private final WeakReference<TrackerScanner> trackerscannerContainer;

    //public InputStream is;
    //private final WeakReference<InputStream> inputSteamContrainer;
    private InputStream is;

    private InputStream _userPFX;

    //private final WeakReference<ServerMessage> serverMessageContrainer;
    private final ServerMessage serverMessage;


    //##############################################################################################
    // functions

    //==============================================================================================
    /**
     * Constructor which takes in a TrackerScanner reference, and InputStream and ServerMessage,
     * initialising these variables.
     *
     * @param trackerScanner If the message fails to be sent correctly, it is returned to the queue
     *                       of this TrackerScanner reference
     * @param is             InputStream containing the necessary server public certificate
     * @param userPFX        InputStream containing the necessary Client certificate/pfx file
     *
     * @param serverMessage  The ServerMessage object to be posted
     */
    public PostToServer(TrackerScanner trackerScanner,
                        InputStream is,
                        InputStream userPFX,
                        ServerMessage serverMessage
                        ) {

        this.trackerscannerContainer = new WeakReference<>(trackerScanner);
        //this.inputSteamContrainer = new WeakReference<>(is);
        this.is = is;

        this._userPFX = userPFX;

        //this.serverMessageContrainer = new WeakReference<>(serverMessage);
        this.serverMessage = serverMessage;

    }// end of PostToServer Constructor
    //==============================================================================================

    //==============================================================================================
    /**
     * Calls Parent onPreExecute
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }// end of onPreExecute
    //==============================================================================================

    //==============================================================================================
    /**
     * Called After the message is sent send the resulting message back to the internal
     * TrackerScanner reference. Also attempts to read the parameters returned by the the server
     *
     * @param result The String result of doInBackground
     */
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (this.trackerscannerContainer != null)
        {

            TrackerScanner _trackerScanner = this.trackerscannerContainer.get();

            String[] splitResult = result.split(";");

            String message = splitResult[0];

            // note here collect parameter data attached and digest
            if(splitResult.length == 10) {
                int index = 1; // as main message is on zero
                int gpsLamda = Integer.parseInt(splitResult[index++].trim());
                int wifiLamda = Integer.parseInt(splitResult[index++].trim());
                int postLamda = Integer.parseInt(splitResult[index++].trim());
                int bleLamda = Integer.parseInt(splitResult[index++].trim());
                int accelLamda = Integer.parseInt(splitResult[index++].trim());
                int magLamda = Integer.parseInt(splitResult[index++].trim());

                int gpsDuration = Integer.parseInt(splitResult[index++].trim());
                int gpsInterval = Integer.parseInt(splitResult[index++].trim());
                int bleDuration = Integer.parseInt(splitResult[index++].trim());

                _trackerScanner.setgPS_lambda(gpsLamda);
                _trackerScanner.setwifi_lambda(wifiLamda);
                _trackerScanner.setpost_lambda(postLamda);
                _trackerScanner.setble_lambda(bleLamda);
                _trackerScanner.setaccel_lambda(accelLamda);
                _trackerScanner.setmag_lambda(magLamda);

                _trackerScanner.setgPS_duration(gpsDuration);
                _trackerScanner.setgPS_scan_interval(gpsInterval);
                _trackerScanner.setBle_duration(bleDuration);


            }// end of if correct number of return elements

            _trackerScanner.sendResult(message);

        }
    }// end of onPostExecute
    //==============================================================================================


    //==============================================================================================
    /**
     * Does the work of sending the message to the server, and returning results.
     *
     * @param params String parameter array, currently note used
     * @return The message returned by the server or error message
     */
    @Override
    protected String doInBackground(String... params) {

        //---------------------------------------------------------------------

        // Extract the data from the message object

        String message = this.serverMessage.message;
        String urlString = this.serverMessage.urlString;
        boolean useSSL = this.serverMessage.useSSL;
        final String address = this.serverMessage.address;

        final String method = "POST";
        final int timeOut = 5000;

        BufferedReader reader = null;


        //------------------------------------------------------------------

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

            //InputStream caInput = new BufferedInputStream(this.inputSteamContrainer.get());
            InputStream caInput = new BufferedInputStream(this.is);


            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                //System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            //------------------------------------------------------------------

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String keyPassphrase = "";
            KeyStore keyStoreClient = KeyStore.getInstance("PKCS12");
            keyStoreClient.load(_userPFX, keyPassphrase.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStoreClient, keyPassphrase.toCharArray());


            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);




            //------------------------------------------------------------------


            // Create an HostnameVerifier that hardwires the expected hostname.
            // Note that is different than the URL's hostname:
            // example.com versus example.org
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // at least check that the server address matches what we expect
                    return hostname.equals(address);

                }// end of verify
            };


            //------------------------------------------------------------------
            // set up connection

//            if (method.equals("GET")) {
//                urlString += "?" + this.getEncodedParams(parameters);
//                //As mentioned before, this only executes if the request method has been
//                //set to GET
//            }

            URL url = new URL(urlString);
            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            // set time outs
            con.setReadTimeout(timeOut);
            con.setConnectTimeout(timeOut);

            if (useSSL) {
                // set certificate
                ((HttpsURLConnection) con).setSSLSocketFactory(context.getSocketFactory());

                // set host name
                ((HttpsURLConnection) con).setHostnameVerifier(hostnameVerifier);


            }

            //------------------------------------------------------------------
            // post the message

            if (method.equals("POST")) {
                con.setDoOutput(true);
                OutputStreamWriter writer =
                        new OutputStreamWriter(con.getOutputStream());

                writer.write(message);

                writer.flush();
            }

            //------------------------------------------------------------------
            // get the returned message

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }// end of looping lines of returned message

            return sb.toString();

        } catch (Exception e) {

            // put it back in the queue
            this.trackerscannerContainer.get().resendQueue.add( serverMessage );

            e.printStackTrace();
            System.out.println(e.getMessage());
            return "Exception: " + e.getMessage();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }// end of try within finally
            }// end of if reader not null
        }// end of try-catch-finally

    }// end of do in background method
    //==============================================================================================

    //==============================================================================================
    /**
     * The method below is only called if the request method has been set to GET
     * GET requests sends data in the url and it has to be encoded correctly in order
     * for the server to understand the request. This method encodes the data in the
     * params variable so that the server can understand the request
     *
     * @param params Map of keys and parameters to be encoded
     */
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
    }// end of getEncodedParams
    //==============================================================================================


}//end of Class
