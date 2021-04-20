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

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.riba2reality.wifimapper.DataStores.Constants;
import com.riba2reality.wifimapper.R;
import com.riba2reality.wifimapper.TrackerScanner;

import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QRScannerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QRScannerFragment extends Fragment {


    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final int PERMISSION_REQUEST_CAMERA = 0;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private Button qrCodeFoundButton;
    private TextView qrCodeLabel;
    private String qrCode;

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
        qrCodeFoundButton.setVisibility(View.INVISIBLE);
        qrCodeFoundButton.setOnClickListener(codeFoundButtonListerner);
        qrCodeFoundButton.setEnabled(scanCompleted);

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

            // turn off QR code analysis
            requestCamera(false);

            manualScan();




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
    QRCodeImageAnalyzer imageAnalyzer = new QRCodeImageAnalyzer(new QRCodeFoundListener() {
        @Override
        public void onQRCodeFound(String _qrCode) {

            Log.d("Trace", "QRScannerFragment.onQRCodeFound()");

            String qrCheckMessage = getString(R.string.qr_check_message);

            if(_qrCode.startsWith(qrCheckMessage)) {

                qrCode = _qrCode;


                qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_found_scan));

                qrCodeLabel.setTextColor(Color.GREEN);
                qrCodeFoundButton.setOnClickListener(codeFoundButtonListerner);
            }else{

                qrCodeFoundButton.setText(getResources().getString(R.string.qr_code_found_none_riba));
                qrCodeLabel.setTextColor(Color.RED);
                qrCodeFoundButton.setOnClickListener(null);
            }

            qrCodeFoundButton.setVisibility(View.VISIBLE);
            qrCodeLabel.setText(_qrCode);
            qrCodeLabel.setVisibility(View.VISIBLE);

        }

        @Override
        public void qrCodeNotFound() {

            qrCodeFoundButton.setVisibility(View.INVISIBLE);

            qrCodeLabel.setVisibility(View.INVISIBLE);
        }
    });
    //==============================================================================================

    //==============================================================================================
    private void manualScan(){

        Log.d("Trace", "QRScannerFragment.ManualScan()");

        //scanCompleted = false;

        //getActivity().findViewById(R.id.manualScanButton).setEnabled(false);
        qrCodeFoundButton.setEnabled(false);


        //String imageName = getCharForNumber(selectedLocation);
        String message = qrCode;



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

            if(qrCodeFoundButton!=null){
                qrCodeFoundButton.setEnabled(true);
            }

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
            int accelQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_ACCEL_QUEUE_COUNT,-1);

            String buttonMessage = buttonStandardText
                    + " L["+locationQueueSize+ "]"
                    + " W["+wifiQueueSize+ "]"
                    + " M["+magQueueSize+ "]"
                    + " A["+accelQueueSize+ "]"
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