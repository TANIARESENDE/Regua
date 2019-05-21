package com.example.a80126650.regua;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    static String mCurrentPhotoPath;

    ArrayList<Float> arrayList1 = new ArrayList<>();
    ArrayList<Float> arrayList2 = new ArrayList<>();
    Vector3 point1, point2;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA = 0;
    private boolean mUserRequestedInstall = true;
    private Session mSession;
    private ArFragment arFragment;
    private ArSceneView arSceneView;
    private ModelRenderable SphereRenderable;
    private ModelRenderable LineRenderable;
    private TextView text;
    private ViewRenderable testViewRenderable;
    private ViewRenderable textRenderable;
    private GestureDetector gestureDetector;
    private AnchorNode lastAnchorNode;
    private int permissionCheck;
    private float distanceCm;
    private float d;
    private ViewRenderable mTextView;
    private ViewRenderable mArModelCreator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        FloatingActionButton fab_clear = findViewById(R.id.fab_clear);

        fab.setOnClickListener(view -> takePhoto());

        fab_clear.setOnClickListener(v -> onClear());
    }

    private void onClear() {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    Objects.requireNonNull(((AnchorNode) node).getAnchor()).detach();
                }
            }
        }

        arrayList1.clear();
        arrayList2.clear();
        point1 = null;
        point2 = null;
        distanceCm = 0;
        lastAnchorNode = null;
    }

    private void getPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        else
            dispatchTakePictureIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ARCore requer permissão de camera para funcionar

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        //Verificar se ARCore está instalada e atualizada para a última versão
        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Sucesso, criou a ARCORE session.
                        mSession = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        //Garante que a próxima chamada de requestInstall () retornará
                        // INSTALADO ou lance uma exceção.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            // Exibe uma mensagem apropriada para o usuário e retorna normalmente.
            Toast.makeText(this, "Erro" + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (Exception e) {  // Declarações catch atuais
            Log.e("ERRO","Erro");
            return;  // mSession ainda é nulo.
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        assert arFragment != null;
        arSceneView = arFragment.getArSceneView();
        mSession = arSceneView.getSession();

        arFragment.getArSceneView().getPlaneRenderer().getMaterial().thenAccept(material -> material.setFloat3(PlaneRenderer.MATERIAL_COLOR,new Color(android.graphics.Color.WHITE)));
        arFragment.getArSceneView().getPlaneRenderer().getMaterial().thenAccept(material -> material.setFloat(PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS,10f));
        arFragment.getArSceneView().getPlaneRenderer().getMaterial().thenAccept(material -> material.setFloat2(PlaneRenderer.MATERIAL_UV_SCALE,10.0f,10.0f));
        arFragment.getArSceneView().getPlaneRenderer().setShadowReceiver(false);

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            SphereRenderable = ShapeFactory.makeSphere(0.0125f, new Vector3(0.0f, 0.01875f, 0.0f), material);
                            SphereRenderable.setShadowReceiver(false);
                            SphereRenderable.setShadowCaster(false);
                        });


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                    if (lastAnchorNode == null) {
                        Anchor anchor = hitResult.createAnchor();
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        Pose pose = anchor.getPose();
                        if (arrayList1.isEmpty()) {
                            arrayList1.add(pose.tx());
                            arrayList1.add(pose.ty());
                            arrayList1.add(pose.tz());
                        }
                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setParent(anchorNode);
                        transformableNode.setRenderable(SphereRenderable);
                        transformableNode.select();

                        lastAnchorNode = anchorNode;
                    } else {

                        int val = motionEvent.getActionMasked();
                        float axisVal = motionEvent.getAxisValue(MotionEvent.AXIS_X, motionEvent.getPointerId(motionEvent.getPointerCount() - 1));
                        Log.e("Values:", String.valueOf(val) + String.valueOf(axisVal));

                        Anchor anchor = hitResult.createAnchor();
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        Pose pose = anchor.getPose();

                        if (arrayList2.isEmpty()) {
                            arrayList2.add(pose.tx());
                            arrayList2.add(pose.ty());
                            arrayList2.add(pose.tz());
                            d = getDistanceMeters(arrayList1, arrayList2);
                            distanceCm = ((int)(getDistanceMeters(arrayList1, arrayList2) * 1000))/10.0f;

                        } else {
                            arrayList1.clear();
                            arrayList1.addAll(arrayList2);
                            arrayList2.clear();
                            arrayList2.add(pose.tx());
                            arrayList2.add(pose.ty());
                            arrayList2.add(pose.tz());
                            d = getDistanceMeters(arrayList1, arrayList2);
                            distanceCm = ((int)(getDistanceMeters(arrayList1, arrayList2) * 1000))/10.0f;
                        }

                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setParent(anchorNode);
                        transformableNode.setRenderable(SphereRenderable);
                        transformableNode.select();

                        Vector3 point1, point2;
                        point1 = lastAnchorNode.getWorldPosition();
                        point2 = anchorNode.getWorldPosition();

                        final Vector3 difference = Vector3.subtract(point1, point2);
                        final Vector3 directionFromTopToBottom = difference.normalized();
                        final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

                        ViewRenderable.builder()
                                .setView(arFragment.getContext(), R.layout.test_view)
                                .build()
                                .thenAccept(renderable -> mArModelCreator = renderable);

                        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.RED))
                                .thenAccept(
                                        material -> {
                                            LineRenderable = ShapeFactory.makeCube(
                                                    new Vector3(.0025f, .0025f, difference.length()),
                                                    Vector3.zero(), material);
                                            Node lineNode = new Node();
                                            lineNode.setParent(lastAnchorNode);
                                            lineNode.setRenderable(LineRenderable);
                                            lineNode.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                                            lineNode.setWorldRotation(rotationFromAToB);
                                                                                    }
                                );

                       ViewRenderable.builder()
                                .setView(arFragment.getContext(), R.layout.test_view)
                                .build()
                                .thenAccept(renderable ->  {
                                    testViewRenderable = renderable;
                                    DpToMetersViewSizer viewSizer = new DpToMetersViewSizer(500);
                                    testViewRenderable.setSizer(viewSizer);
                                    testViewRenderable.setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER);
                                    testViewRenderable.setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER);
                                    ((TextView)testViewRenderable.getView().findViewById(R.id.Card))
                                            .setText("  "+String.valueOf(Math.round(distanceCm))+ " cm ");
                                    TransformableNode textNode = new TransformableNode(arFragment.getTransformationSystem());
                                    textNode.setRenderable(testViewRenderable);
                                    textNode.setParent(lastAnchorNode);
                                    textNode.setWorldScale(new Vector3(1f, 1f, 1f).scaled(0));

                                    Vector3 positionLine = new Vector3(Vector3.add(point1, point2).scaled(.5f));
                                    float x = positionLine.x;
                                    float y = positionLine.y*0.9f;
                                    float z = positionLine.z;

                                    Vector3 positionText = new Vector3(x,y,z);
                                    textNode.setWorldPosition(positionText);

                                    textNode.setWorldRotation(Quaternion.multiply(rotationFromAToB,
                                            Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), 90)));

                                });

                        lastAnchorNode = anchorNode;
                    }
                });

        // Set up a tap gesture detector.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
    }

    private float getDistanceMeters(ArrayList<Float> arayList1, ArrayList<Float> arrayList2) {
        float distanceX = arayList1.get(0) - arrayList2.get(0);
        float distanceY = arayList1.get(1) - arrayList2.get(1);
        float distanceZ = arayList1.get(2) - arrayList2.get(2);
        return (float) Math.sqrt(distanceX * distanceX +
                distanceY * distanceY +
                distanceZ * distanceZ);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Não vai funcionar!!!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile("PHOTOAPP", ".jpg", storageDir);
                mCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();
            }
            catch(IOException ex){
                Toast.makeText(getApplicationContext(), "Erro ao tirar a foto", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < 24) {
            Log.e(TAG, "A funcionalidade Régua requer Android 7.0 ou superior");
            Toast.makeText(activity, "A funcionalidade Régua requer Android 7.0 ou superior", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }

        return true;
    }

    private String generateFilename() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());

        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + "/camera/" +File.separator + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            throw new IOException("Falha ao salvar foto no aparelho.", ex);
        }
    }

    private void takePhoto() {
        final String filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView();

        // Cria um bitmap do tamanho da scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Cria um thread de manipulador para descarregar o processamento da imagem.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Faça uma requisição/pedido (request) para copiar.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(MainActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                Toast toast = Toast.makeText(MainActivity.this, "Foto salva!",
                        Toast.LENGTH_LONG);
                toast.show();
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Foto salva", Snackbar.LENGTH_LONG);
                snackbar.setAction("Abra em Fotos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                            MainActivity.this.getPackageName() + ".ar.codelab.name.provider",
                            photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(MainActivity.this,
                        "Falha ao tirar foto! " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

}
