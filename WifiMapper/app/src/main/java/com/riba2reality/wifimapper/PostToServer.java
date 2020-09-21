package com.riba2reality.wifimapper;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class PostToServer extends AsyncTask<String, String, String> {


    public PostToServer(){
        //set context variables if required
    }

    public InputStream is;




    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        String urlString = params[0]; // URL to call
        String data = params[1]; //data to post
        OutputStream out = null;

        try {


            /*
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            out = new BufferedOutputStream(urlConnection.getOutputStream());

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            out.close();

            urlConnection.connect();

             */



            /*
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("firstParam", data);
                    //.appendQueryParameter("secondParam", paramValue2)
                    //.appendQueryParameter("thirdParam", paramValue3);
            String query = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
*/

            //---------------------------------

            //System.out.println(System.getProperty("user.dir"));


            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt

            //InputStream caInput = new BufferedInputStream(new FileInputStream("cert.pem"));

            //InputStream is = this.getResources().openRawResource(R.raw.sample);
            //BufferedReader reader = new BufferedReader(new InputStreamReader(is));




            //InputStream caInput = new BufferedInputStream(new FileInputStream("cert.pem"));
            InputStream caInput = new BufferedInputStream(this.is);




            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
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
                    HostnameVerifier hv =
                            HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify("192.168.0.10", session);
                    //return hv.verify("82.46.100.70", session);

                    //System.out.println(hostname);
/*
                    if(
                            hostname.equals("192.168.0.10") ||
                                    hostname.equals("10.0.2.2")

                    )
                        return true;
                    else
                        return false;

 */
                }
            };








            //---------------------------------
            BufferedReader reader = null;
            String uri = urlString;
            String method = "GET";
            Map<String, String> parameters = new HashMap<>();

            method = "POST";

            //---------
            /*
            // dummy data

            List<String> macAddressList = new ArrayList<>();

            macAddressList.add("000.111.222.444");
            macAddressList.add("777");
            macAddressList.add("ABC");

            parameters.put("TIME","12:01");
            parameters.put("X","42");
            parameters.put("Y","7");

            String macAddressJson = new Gson().toJson(macAddressList );


            //parameters.put("MacAddresses",macAddressList.toString());
            parameters.put("MacAddressesJson",macAddressJson);


*/

            //---------


            if (method.equals("GET")) {
                uri += "?" + this.getEncodedParams(parameters);
                //As mentioned before, this only executes if the request method has been
                //set to GET
            }

            try {
                URL url = new URL(uri);
                //HttpURLConnection con = (HttpURLConnection) url.openConnection();
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod(method);

                // set certificate
                con.setSSLSocketFactory(context.getSocketFactory());

                // set host name
                con.setHostnameVerifier(hostnameVerifier);



                if (method.equals("POST")) {
                    con.setDoOutput(true);
                    OutputStreamWriter writer =
                            new OutputStreamWriter(con.getOutputStream());
                    //writer.write(this.getEncodedParams(parameters));

                    //writer.write(new JSONObject(parameters).toString());
                    writer.write(data);

                    writer.flush();
                }

                StringBuilder sb = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                    System.out.println(line);
                }





                return sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
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
            System.out.println(e.getMessage());
            return null;
        }
    }// end of do in background method


    //The method below is only called if the request method has been set to GET
    //GET requests sends data in the url and it has to be encoded correctly in order
    //for the server to understand the request. This method encodes the data in the
    //params variable so that the server can understand the request

    public String getEncodedParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            String value = null;
            //try {
            //value = URLEncoder.encode(params.get(key), "UTF-8");
            value = params.get(key);
            //} catch (UnsupportedEncodingException e) {
            //    e.printStackTrace();
            //}

            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key + "=" + value);
        }
        return sb.toString();
    }







}//end of Class
