package com.example.tp62019am;


import  com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceRestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button ButtonFoto;
    int codigoLlamada;
    TextView txtResultado;
    ImageView imgResultado;
    int codigoPedirPermiso;

    SharedPreferences preferencias;
    ProgressDialog dialogoDeProgreso;
    FaceServiceRestClient servicioProcesamientoImagenes;

    String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";
    String subcriptionKey = "293152f2991b40ed8845fd5687d31274";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


         preferencias =getSharedPreferences("LeoLob", Context.MODE_PRIVATE);
         dialogoDeProgreso  = new ProgressDialog(this);

        Log.d("Ejecuto", "ejecuto");
        txtResultado = findViewById(R.id.textViewResultados);
        ButtonFoto = findViewById(R.id.buttonTomarFoto);
        imgResultado = findViewById(R.id.imageViewFoto);






        try {
            servicioProcesamientoImagenes = new FaceServiceRestClient(apiEndpoint, subcriptionKey);
        } catch (Exception error) {
            Log.d("Inicio", "Error en inicialización" + error.getMessage());
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ButtonFoto.setEnabled(false);
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, codigoPedirPermiso);

        } else {
            ButtonFoto.setEnabled(true);
        }
    }
    @Override
    public void onRequestPermissionsResult(int codigoRespuesta, @NonNull String[] nombresPermisos, @NonNull int[] resultadosPermisos) {
        if (codigoRespuesta == codigoPedirPermiso) {
            for (int PunteroPermiso = 0; PunteroPermiso < nombresPermisos.length; PunteroPermiso++) {
                Log.d("permisos pedidos", "permiso" + PunteroPermiso + "-NOmbre:" + nombresPermisos[PunteroPermiso] + "-"
                        + (resultadosPermisos[PunteroPermiso] = PackageManager.PERMISSION_GRANTED));
            }

            Boolean ObtuvoTodosLosPermisos;
            ObtuvoTodosLosPermisos = true;

            for (int punteroPermiso = 0; punteroPermiso < resultadosPermisos.length; punteroPermiso++) {
                if (resultadosPermisos[punteroPermiso] != PackageManager.PERMISSION_GRANTED) {
                    ObtuvoTodosLosPermisos = false;
                }
            }

            if (ObtuvoTodosLosPermisos) {

                ButtonFoto.setEnabled(true);
            }
        }
    }
    public void ButtonTomar(View vista) {
        Intent intentTomar;
        intentTomar = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(intentTomar, codigoLlamada);
    }

    public void ButtonElegir(View vista) {
        Intent intent;
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        Log.d("Foto", "entro");
        intent.setType("image/*");
        Log.d("Foto", "se eligio la foto");
        startActivityForResult(Intent.createChooser(intent, "Seleccione Foto"), codigoLlamada);
        Log.d("Foto", ""+ codigoLlamada);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent DatosRecibidos) {

        super.onActivityResult(requestCode, resultCode, DatosRecibidos);
        txtResultado.setText("procesando...");
        if (requestCode == codigoLlamada && resultCode == RESULT_OK) {
            Bitmap fotoRecibida = (Bitmap) DatosRecibidos.getExtras().get("data");
            imgResultado.setImageBitmap(fotoRecibida);
            procesarImagenObtenida(fotoRecibida);
        }


        if (requestCode == codigoLlamada && resultCode == RESULT_OK && DatosRecibidos != null) {
            Uri ubicacion = DatosRecibidos.getData();
            Bitmap imagenFoto = null;
            try {
                imagenFoto = MediaStore.Images.Media.getBitmap(getContentResolver(), ubicacion);

            } catch (Exception error) {
            }
            if (imagenFoto != null) {
                imgResultado.setImageBitmap(imagenFoto);
                procesarImagenObtenida(imagenFoto);

            }
        }
    }

    public void procesarImagenObtenida(final Bitmap imagenAProcesar) {

        ByteArrayOutputStream streamSalida = new ByteArrayOutputStream();
        imagenAProcesar.compress(Bitmap.CompressFormat.JPEG, 100, streamSalida);
        ByteArrayInputStream streamEntrada = new ByteArrayInputStream(streamSalida.toByteArray());

        class procesarImagen extends AsyncTask<InputStream, String, Face[]> {

            @Override
            protected Face[] doInBackground(InputStream... imagenAProcesar) {
                publishProgress("Detectando caras...");

                Face[] resultado = null;
                try{
                    FaceServiceClient.FaceAttributeType[] atributos;
                    atributos=new FaceServiceClient.FaceAttributeType[]{
                            FaceServiceClient.FaceAttributeType.Age,
                            FaceServiceClient.FaceAttributeType.Glasses,
                            FaceServiceClient.FaceAttributeType.Smile,
                            FaceServiceClient.FaceAttributeType.FacialHair,
                            FaceServiceClient.FaceAttributeType.Gender
                    };
                    resultado=servicioProcesamientoImagenes.detect(imagenAProcesar[0],true,false,atributos);

                }   catch (Exception error){

                }
                return resultado;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialogoDeProgreso.show();
            }

            @Override
            protected void onProgressUpdate(String... mensajeProgreso) {
                super.onProgressUpdate(mensajeProgreso);
                dialogoDeProgreso.setMessage(mensajeProgreso[0]);
            }

            @Override
            protected void onPostExecute(Face[] resultado) {
                super.onPostExecute(resultado);
                dialogoDeProgreso.dismiss();

                if (resultado==null){
                    txtResultado.setText("Error en procesamiento");
                }else {

                    if(resultado.length>0){
                        recuadrarCaras(imagenAProcesar,resultado);

                        procesarResultadosDeCaras(resultado);
                    }else{
                        txtResultado.setText("No se detecto ninguna cara");
                    }
                }
            }


        }
        procesarImagen miTarea = new procesarImagen();
        miTarea.execute(streamEntrada);


    }

    void recuadrarCaras (Bitmap imagenOriginal, Face[] carasARecuadrar){
        Bitmap imagenADibujar;
        imagenADibujar =imagenOriginal.copy(Bitmap.Config.ARGB_8888, true);

        Canvas lienzo;
        lienzo = new Canvas(imagenADibujar);
        Paint pincel;
        pincel = new Paint();

        pincel.setAntiAlias(true);
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setColor(Color.BLUE);
        pincel.setStrokeWidth(5);

        for(Face unaCara:carasARecuadrar){
            FaceRectangle rectanguloUnaCara;
            rectanguloUnaCara=unaCara.faceRectangle;

            lienzo.drawRect(rectanguloUnaCara.left,
                    rectanguloUnaCara.top,
                    rectanguloUnaCara.left+rectanguloUnaCara.width,
                    rectanguloUnaCara.top+rectanguloUnaCara.height,pincel);
        }
        imgResultado.setImageBitmap(imagenADibujar);
    }

    void procesarResultadosDeCaras (Face[] carasAProcesar){
        int cantidadHombres;
        int cantidadMujeres;
        cantidadHombres=preferencias.getInt("cantidadHombres",0);
        cantidadMujeres=preferencias.getInt("cantidadMujeres",0);

        String mensaje;
        mensaje="";

        for (int punteroCara=0; punteroCara<carasAProcesar.length;punteroCara++){
            mensaje+="Edad: "+carasAProcesar[punteroCara].faceAttributes.age;
            mensaje+=" - Sonrisa: "+carasAProcesar[punteroCara].faceAttributes.smile;
            mensaje+=" - Barba: "+carasAProcesar[punteroCara].faceAttributes.facialHair.beard;
            mensaje+=" - Género: "+carasAProcesar[punteroCara].faceAttributes.gender;
            mensaje+=" - Anteojos: "+carasAProcesar[punteroCara].faceAttributes.glasses;

            if (carasAProcesar[punteroCara].faceAttributes.gender.equals("male")){
                cantidadHombres++;
            }else{
                cantidadMujeres++;
            }
            SharedPreferences.Editor editor;
            editor=preferencias.edit();
            editor.putInt("cantidadHombres",cantidadHombres);
            editor.putInt("cantidadMujeres",cantidadMujeres);
            editor.commit();

            if (punteroCara<carasAProcesar.length-1){
                mensaje+="/n";
            }
        }
        mensaje+=" - H:"+cantidadHombres+ " -  M: " +cantidadMujeres;
        txtResultado.setText(mensaje);
    }
}
