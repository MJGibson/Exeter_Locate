package com.riba2reality.wifimapper.ui.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.wifimapper.R;
import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QRScannerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QRScannerFragment extends Fragment {


    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String OUTPUT_FORMAT = "%-20s:%s";

    private static final String HASH_ALGORITHM = "SHA-224";

    private static final int PERMISSION_REQUEST_CAMERA = 0;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private Button qrCodeFoundButton;
    private TextView qrCodeLabel;
    private String qrCode;
    private String plainCode;

    boolean scanCompleted = true;


    //==============================================================================================
    public QRScannerFragment() {
        // Required empty public constructor
    }
    //==============================================================================================



    //==============================================================================================
    public static QRScannerFragment newInstance(int index) {
        QRScannerFragment fragment = new QRScannerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_q_r_scanner, container, false);

        previewView = rootView.findViewById(R.id.activity_main_previewView);


        qrCodeFoundButton = rootView.findViewById(R.id.activity_main_qrCodeFoundButton);
        //qrCodeFoundButton.setVisibility(View.INVISIBLE);
        qrCodeFoundButton.setOnClickListener(codeFoundButtonListerner);
        //qrCodeFoundButton.setEnabled(scanCompleted);

        qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_not_found));
        qrCodeFoundButton.setEnabled(false);

        qrCodeLabel = rootView.findViewById(R.id.qrScannerFragmet_textView);
        qrCodeLabel.setVisibility(View.INVISIBLE);



        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());

        requestCamera(true);

        //-------------------------



        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver((receiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_RESULT)
        );

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver((updateReceiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE)
        );


        return rootView;
    }// end of onCreateView
    //==============================================================================================



    //==============================================================================================
    View.OnClickListener codeFoundButtonListerner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //Toast.makeText(getActivity().getApplicationContext(), qrCode, Toast.LENGTH_SHORT).show();
            Log.i(QRScannerFragment.class.getSimpleName(), "QR Code Found: " + qrCode);



            //manualScan();




        }// end of onClick
    };
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(true);
            } else {
                Toast.makeText(
                        getActivity(),
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    //==============================================================================================

    //==============================================================================================
    private void requestCamera(boolean analysis) {
        if (ActivityCompat.checkSelfPermission(
                getActivity().getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(analysis);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    getActivity(), Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            } else {
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }
        }
    }// end of requestCamera
    //==============================================================================================

    //==============================================================================================
    private void startCamera(boolean analysis) {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider, analysis);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(
                        getActivity(),
                        "Error starting camera " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(getActivity()));
    }// end of startCamera
    //==============================================================================================


    //==============================================================================================
    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider, boolean analysis) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();



        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getActivity()),imageAnalyzer);


        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        cameraProvider.unbindAll();

        if(analysis) {
            Camera camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) getActivity(),
                    cameraSelector,
                    imageAnalysis,
                    preview);

        }else{

            Camera camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) getActivity(),
                    cameraSelector,
                    preview);
        }


    }// end of bindCameraPreview
    //==============================================================================================


    //==============================================================================================
    public static byte[] digest(byte[] input, String algorithm) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        byte[] result = md.digest(input);
        return result;
    }
    //==============================================================================================

    //==============================================================================================
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    //==============================================================================================


    //==============================================================================================
    public String checkdigit(String idWithoutCheckdigit) {


        String outputHash = bytesToHex(digest(idWithoutCheckdigit.getBytes(UTF_8),HASH_ALGORITHM));
        return outputHash.substring(outputHash.length()-3);

    }
    //==============================================================================================


    //==============================================================================================
    private void validQRCodeFound(String _qrCode){
        if(scanCompleted){
            qrCodeFoundButton.setEnabled(true);
        }

        qrCode = _qrCode;


        qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_found_scan));

        qrCodeLabel.setTextColor(Color.GREEN);
        qrCodeFoundButton.setOnClickListener(codeFoundButtonListerner);

        manualScan();

    }
    //==============================================================================================


    //==============================================================================================
    private void invalidQRCodeFound(){
        qrCodeFoundButton.setEnabled(false);

        qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_found_none_riba));
        qrCodeLabel.setTextColor(Color.RED);
        qrCodeFoundButton.setOnClickListener(null);
    }
    //==============================================================================================



    //==============================================================================================
    public static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }
    //==============================================================================================


    //==============================================================================================
    QRCodeImageAnalyzer imageAnalyzer = new QRCodeImageAnalyzer(new QRCodeFoundListener() {
        @Override
        public void onQRCodeFound(String _qrCode) {

            Log.d("Trace", "QRScannerFragment.onQRCodeFound()");


            //qrCodeFoundButton.setVisibility(View.VISIBLE);
            qrCodeLabel.setText(_qrCode);
            qrCodeLabel.setVisibility(View.VISIBLE);


            String qrCheckMessage = getString(R.string.qr_check_message);




            if(_qrCode.startsWith(qrCheckMessage)) {

                // check the check digit:


                boolean success = false;
                Map<String, List<String>> stringListMap = null;
                try {
                    stringListMap = splitQuery(new URL(_qrCode));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    success = false;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    success = false;
                }

                List<String> qr_id_list = stringListMap.get("qr");

                String id = qr_id_list.get(0);

                String checkDigit = "";
                if (id.length() < 3) {
                    //throw new IllegalArgumentException("id less than 3 check digits required");
                    success = false;
                } else {
                    //
                    checkDigit = id.substring(id.length() - 3);
                }

                //String checkDigit = String.valueOf(id.charAt(id.length()-1));

                //String plainID = id.substring(0,id.length()-2);

                String plainID;

                // remove check digits
                plainID = id.substring(0,id.length() - 3);
                // remove building id (everything after the -)
                String[] idSubArray = plainID.split("-");
                if(idSubArray.length < 2){
                    //throw new IllegalArgumentException("id less than 3 check digits required");
                    success = false;
                }else {
                    plainID = idSubArray[1];
                }

                //----------------------------

                String outputCheckDigit = checkdigit(plainID);

                plainCode = plainID;

                //----------------------------

                Log.d("Trace-qr", "checkDigit:"+checkDigit);
                Log.d("Trace-qr", "plainID:"+plainID);
                Log.d("Trace-qr", "outputCheckDigit:"+outputCheckDigit);

                if(checkDigit.equals(outputCheckDigit)){
                    success = true;
                }

                if(success) {
                    validQRCodeFound(_qrCode);
                }else {

                    Log.d("Trace-qr", "FAIL!");

                    invalidQRCodeFound();
                }

            }else{

                invalidQRCodeFound();
            }



        }

        @Override
        public void qrCodeNotFound() {

            //qrCodeFoundButton.setVisibility(View.INVISIBLE);
            qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_not_found));
            qrCodeFoundButton.setEnabled(false);


            qrCodeLabel.setVisibility(View.INVISIBLE);
        }
    });
    //==============================================================================================

    //==============================================================================================
    private void manualScan(){

        Log.d("Trace", "QRScannerFragment.ManualScan()");

        // turn off QR code analysis
        requestCamera(false);

        scanCompleted = false;

        //getActivity().findViewById(R.id.manualScanButton).setEnabled(false);
        qrCodeFoundButton.setEnabled(false);


        //String imageName = getCharForNumber(selectedLocation);
        //String message = qrCode;
        String message = plainCode;



        String[] server_values = getResources().getStringArray(R.array.server_values);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String serverAddress = SP.getString("ServerAddress", server_values[1]);

        //System.out.println("ServerAddress: "+serverAddress);

        if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
            Toast.makeText(getActivity(), "Please set Server Address", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_SINGLE_SCAN);

        intent.putExtra("message", message);

        //----------------------------------------------------------


        int manualScanDuration = getResources().getInteger(R.integer.defaultVal_manual_scan);
        manualScanDuration = SP.getInt("duration_manual_scan", manualScanDuration);

        intent.putExtra("duration", manualScanDuration);


        //----------------------------------------------------------


        getActivity().startService(intent);



    }// end of manual scan
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Trace", "QRScannerFragment.receiver.onReceive");

            scanCompleted = true;

            //Button scanButton = getActivity().findViewById(R.id.manualScanButton);
            //qrCodeFoundButton

//            if(qrCodeFoundButton!=null){
//                qrCodeFoundButton.setEnabled(true);
//            }

            requestCamera(true);



        }// end of onRecieve
    };// end of new BroadcastReceiver
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Trace", "QRScannerFragment.updateReceiver.onReceive");

            //

            String buttonStandardText = getResources().getString(R.string.qr_code_found_scan);

            double remainingDuration = ((double)intent.getLongExtra(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_REMAINING,-1))/1000.0;

            int locationQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_LOCATION_QUEUE_COUNT,-1);
            int wifiQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_WIFI_QUEUE_COUNT,-1);
            int magQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_MAG_QUEUE_COUNT,-1);
//            int accelQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_ACCEL_QUEUE_COUNT,-1);
            int bleQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_BLE_QUEUE_COUNT,-1);

            String buttonMessage = buttonStandardText
                    + " L["+locationQueueSize+ "]"
                    + " W["+wifiQueueSize+ "]"
                    + " M["+magQueueSize+ "]"
//                    + " A["+accelQueueSize+ "]"
                    + " b["+bleQueueSize+ "]"
                    + " Time remaining["+remainingDuration+ "]";



            //Button scanButton = getActivity().findViewById(R.id.manualScanButton);

            if(qrCodeFoundButton!=null){
                //scanButton.setEnabled(true);

                qrCodeFoundButton.setText(buttonMessage);

            }




        }// end of onRecieve
    };// end of new BroadcastReceiver
    //==============================================================================================


}//end of class