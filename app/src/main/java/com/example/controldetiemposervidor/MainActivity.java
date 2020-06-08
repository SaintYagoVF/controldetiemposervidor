package com.example.controldetiemposervidor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {


    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private ProgressBar progressBar;


    private TextView txtTituloNombre, txtTituloContenido;

    //FIRESTORE
    private static final String TAG="MainActivity";
    private static final String KEY_OTP="otp";
    private static final String KEY_LOGIN="login";
    private static final String KEY_NOMBRE="nombre";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            //barcodeView.setStatusText(result.getText());
            beepManager.playBeepSoundAndVibrate();

            //Added preview of scanned barcode
           // ImageView imageView = findViewById(R.id.barcodePreview);
            //imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));

            Long valorTiempoQR;
            String cedulaQR;

            String[] stringQR = lastText.split("@");








            Long valorTiempoActual= System.currentTimeMillis();



            if(stringQR.length!=2){
                barcodeView.setStatusText("¡El código QR no es válido!");
            }else{

                Log.d("Cadena1: ",stringQR[0]);
                Log.d("Cadena2: ",stringQR[1]);
                Log.d("CadenaActual: ",valorTiempoActual.toString());

                cedulaQR= stringQR[0];
                valorTiempoQR=Long.parseLong(stringQR[1]);

                if(valorTiempoActual>=valorTiempoQR-15000 && valorTiempoActual<=valorTiempoQR+15000){

                    buscarFirebase(cedulaQR);
                   barcodeView.setStatusText("¡QR válido!");
                }else {

                    barcodeView.setStatusText("¡El código QR ha expirado!");

                }
            }

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        txtTituloNombre=(TextView)findViewById(R.id.TituloNombre);
        txtTituloContenido=(TextView)findViewById(R.id.TituloContenido);
        progressBar=(ProgressBar)findViewById(R.id.progressBar);

        barcodeView = findViewById(R.id.zxing_barcode_scanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);

        barcodeView.setStatusText("Ubique el código QR de su App celular.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }


    private void buscarFirebase(String cedulaQR){


        String cedula=cedulaQR;



        db.collection("Usuario").document(cedula).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        if(documentSnapshot.exists()){

                            progressBar.setVisibility(View.VISIBLE);

                            //Map<String,Object> usuario=documentSnapshot.getData();

                            String nombre_firebase=documentSnapshot.getString(KEY_NOMBRE);

                            final Integer random = new Random().nextInt(9999);

                            String randomFinal=String.format("%04d", random);

                            txtTituloNombre.setText("BIENVENIDO/A ¡"+nombre_firebase+"!");
                            txtTituloContenido.setText("Por favor, ingresa el siguiente código en tu App: "+randomFinal);



                            final Map<String,Object> usuario=new HashMap<>();

                            usuario.put(KEY_OTP,randomFinal);
                            usuario.put(KEY_LOGIN,false);


                            db.collection("Usuario").document(cedula).set(usuario, SetOptions.merge())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Toast.makeText(MainActivity.this,"Esperando OTP respuesta",Toast.LENGTH_LONG).show();

                                            db.collection("Usuario").document(cedula).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                                                    @Nullable FirebaseFirestoreException e) {
                                                    if (e != null) {
                                                        Log.w("CambioenBase", "Listen failed.", e);
                                                        return;
                                                    }

                                                    if (snapshot != null && snapshot.exists()) {
                                                        Log.d("CambioenBase", "Current data: " + snapshot.getData());
                                                        Log.d("CambioenBase", "Login: " + snapshot.getData().containsValue(true));

                                                        if(snapshot.getData().containsValue(true)){
                                                            Toast.makeText(MainActivity.this,"¡ASISTENCIA REGISTRADA!",Toast.LENGTH_LONG).show();
                                                            progressBar.setVisibility(View.INVISIBLE);
                                                            barcodeView.setStatusText("Ubique el código QR de su App celular.");
                                                            txtTituloNombre.setText("");
                                                            txtTituloContenido.setText("BIENVENIDO/A");
                                                            return;
                                                        }


                                                    } else {
                                                        Log.d("CambioenBase", "Current data: null");
                                                    }
                                                }
                                            });


                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(MainActivity.this,"Error al registrar usuario",Toast.LENGTH_LONG).show();
                                            Log.d(TAG,e.toString());

                                        }
                                    });



                            }else{
                                Toast.makeText(MainActivity.this,"Compruebe su conexión a internet",Toast.LENGTH_LONG).show();
                                //hideDialog();
                            }





                        }




                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,"Error: Verifique su conexión a Internet",Toast.LENGTH_LONG).show();
                        Log.d(TAG,e.toString());
                       // hideDialog();
                    }
                });

    }
}
