package com.example.facialscanner;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;


public class MainActivity extends AppCompatActivity {
    private Interpreter tflite;
    private FirebaseFirestore db;
    private PreviewView previewView;
    private TextView resultText;
    private ImageButton imageButton;
    private Mode currentMode = Mode.REGISTER;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    enum Mode { REGISTER, RECOGNIZE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Load TFLite model
        try {
            tflite = new Interpreter(loadModelFile("facenet.tflite"));
            Tensor input = tflite.getInputTensor(0);
            Tensor output = tflite.getOutputTensor(0);
            Log.d("TFLite", "Input shape=" + Arrays.toString(input.shape()) +
                    ", type=" + input.dataType());
            Log.d("TFLite", "Output shape=" + Arrays.toString(output.shape()) +
                    ", type=" + output.dataType());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Firestore instance
        db = FirebaseFirestore.getInstance();

        // UI elements
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnRecognize = findViewById(R.id.btnRecognize);
        resultText = findViewById(R.id.resultText);
        previewView = findViewById(R.id.previewView);
        imageButton = findViewById(R.id.switchCameraButton);

        // Mode toggle
        btnRegister.setOnClickListener(v -> {
            currentMode = Mode.REGISTER;
            Toast.makeText(this, "Register mode enabled", Toast.LENGTH_SHORT).show();
        });

        btnRecognize.setOnClickListener(v -> {
            currentMode = Mode.RECOGNIZE;
            Toast.makeText(this, "Recognize mode enabled", Toast.LENGTH_SHORT).show();
        });


        // ✅ Runtime camera permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> startCamera(lensFacing),
                    ContextCompat.getMainExecutor(this));
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
        }

        // Switch camera button
        imageButton.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;
            startCamera(lensFacing);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera(lensFacing);
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
        }
    }
    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelName);
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }



    private void startCamera(int lensFacing) {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

            // Preview setup
            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // ImageAnalysis setup
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                @android.annotation.SuppressLint("UnsafeOptInUsageError")
                android.media.Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    com.google.mlkit.vision.common.InputImage image =
                            com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees()
                            );

                    // Run face detection
                    FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .build();
                    FaceDetector detector = FaceDetection.getClient(options);

                    detector.process(image)
                            .addOnSuccessListener(faces -> {
                                for (Face face : faces) {
                                    // 1️⃣ Get bounding box
                                    Rect bounds = face.getBoundingBox();
                                    FaceOverlayView overlay = findViewById(R.id.faceOverlay);

                                    // 2️⃣ Update overlay here
                                    runOnUiThread(() -> {
                                        overlay.setFace(bounds, FaceOverlayView.Status.REGISTERING);
                                    });

                                    Bitmap fullBitmap = toBitmap(mediaImage);
                                    // 4️⃣ Safe crop
                                    Bitmap faceBitmap = safeCropFace(fullBitmap, bounds);
                                    if (faceBitmap == null) {

                                        continue;
                                    }


                                    // ✅ Use a unique variable name
                                    ByteBuffer inputBuffer = preprocessFace(faceBitmap);

                                    // Allocate output buffer based on model spec
                                    Tensor output = tflite.getOutputTensor(0);
                                    int embeddingSize = output.shape()[1];
                                    float[][] embedding = new float[1][embeddingSize];
                                    tflite.run(inputBuffer, embedding);

                                    handleEmbedding(embedding[0]);

                                    Log.d("Embedding", "Generated embedding: " + Arrays.toString(embedding[0]));

                                    if (currentMode == Mode.REGISTER) {
                                        runOnUiThread(() -> {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Enter Name");

                                            final EditText nameInput = new EditText(MainActivity.this);
                                            nameInput.setHint("Person's name");
                                            builder.setView(nameInput);

                                            builder.setPositiveButton("Save", (dialog, which) -> {
                                                String name = nameInput.getText().toString().trim();
                                                if (!name.isEmpty()) {
                                                    Map<String, Object> user = new HashMap<>();
                                                    user.put("name", name);
                                                    user.put("embedding", EmbeddingHelper.toFloatList(embedding[0]));
                                                    user.put("createdAt", FieldValue.serverTimestamp());

                                                    db.collection("users")
                                                            .add(user)
                                                            .addOnSuccessListener(docRef ->
                                                                    runOnUiThread(() -> resultText.setText("Saved: " + name)))
                                                            .addOnFailureListener(e ->
                                                                    runOnUiThread(() -> resultText.setText("Error saving face")));
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                                            builder.show();
                                        });
                                    } else if (currentMode == Mode.RECOGNIZE) {
                                        recognizeFace(embedding[0]); // pass 512‑dimensional vector
                                    }


                                }
                            })
                            .addOnFailureListener(e -> Log.e("FaceDetection", "Detection failed", e))
                            .addOnCompleteListener(task -> imageProxy.close());

                } else {
                    imageProxy.close();
                }
            });

            // Bind camera to lifecycle
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {
            Log.e("CameraX", "Failed to start camera", e);
        }
    }

    private Bitmap safeCropFace(Bitmap fullBitmap, Rect bounds) {
        int x = Math.max(bounds.left, 0);
        int y = Math.max(bounds.top, 0);
        int width = Math.min(bounds.width(), fullBitmap.getWidth() - x);
        int height = Math.min(bounds.height(), fullBitmap.getHeight() - y);

        if (width > 0 && height > 0) {
            return Bitmap.createBitmap(fullBitmap, x, y, width, height);
        } else {
            return null;
        }
    }
    private Bitmap toBitmap(android.media.Image image) {
        // Convert YUV_420_888 image to NV21 byte array
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // Convert NV21 to Bitmap
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    private ByteBuffer preprocessFace(Bitmap faceBitmap) {
        Tensor input = tflite.getInputTensor(0);
        int width = input.shape()[1];
        int height = input.shape()[2];
        DataType type = input.dataType();

        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, width, height, true);

        if (type == DataType.FLOAT32) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 3 * 4);
            buffer.order(ByteOrder.nativeOrder());
            int[] pixels = new int[width * height];
            resized.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int pixel : pixels) {
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                buffer.putFloat((r - 0.5f) * 2.0f);
                buffer.putFloat((g - 0.5f) * 2.0f);
                buffer.putFloat((b - 0.5f) * 2.0f);
            }
            buffer.rewind();
            return buffer;
        } else { // UINT8
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 3);
            buffer.order(ByteOrder.nativeOrder());
            int[] pixels = new int[width * height];
            resized.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
            }
            buffer.rewind();
            return buffer;
        }
    }



    private void handleEmbedding(float[] embedding) {
        Log.d("Embedding", "Generated embedding: " + Arrays.toString(embedding));

        if (currentMode == Mode.REGISTER) {
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter Name");

                final EditText nameInput = new EditText(MainActivity.this);
                nameInput.setHint("Person's name");
                builder.setView(nameInput);

                builder.setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("embedding", EmbeddingHelper.toFloatList(embedding));
                        user.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users")
                                .add(user)
                                .addOnSuccessListener(docRef ->
                                        runOnUiThread(() -> resultText.setText("Saved: " + name)))
                                .addOnFailureListener(e ->
                                        runOnUiThread(() -> resultText.setText("Error saving face")));
                    } else {
                        Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            });
        } else if (currentMode == Mode.RECOGNIZE) {
            recognizeFace(embedding); // pass float[] directly
        }
    }

    /**
     * Compare the given embedding against stored embeddings in Firestore.
     * This is a stub — you can implement cosine similarity or Euclidean distance.
     */
    private void recognizeFace(float[] embedding) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String bestMatch = "Unknown";
                    double bestScore = Double.MAX_VALUE;

                    for (var doc : querySnapshot.getDocuments()) {
                        List<Double> storedEmbedding = (List<Double>) doc.get("embedding");
                        if (storedEmbedding != null && storedEmbedding.size() == embedding.length) {
                            double distance = calculateEuclideanDistance(embedding, storedEmbedding);
                            if (distance < bestScore) {
                                bestScore = distance;
                                bestMatch = (String) doc.get("name");
                            }
                        }
                    }

                    final String result = "Recognized: " + bestMatch + " (distance=" + bestScore + ")";
                    runOnUiThread(() -> resultText.setText(result));
                })
                .addOnFailureListener(e ->
                        runOnUiThread(() -> resultText.setText("Error recognizing face")));
    }

    /**
     * Simple Euclidean distance between two embeddings.
     */
    private double calculateEuclideanDistance(float[] embedding, List<Double> storedEmbedding) {
        double sum = 0.0;
        for (int i = 0; i < embedding.length; i++) {
            double diff = embedding[i] - storedEmbedding.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private float[] runInference(ByteBuffer inputBuffer) {
        Tensor output = tflite.getOutputTensor(0);
        org.tensorflow.lite.DataType outType = output.dataType();
        int[] outShape = output.shape();        // e.g., [1, 128] or [1, 512]
        int embeddingSize = (outShape.length == 2) ? outShape[1] : outShape[outShape.length - 1];

        if (outType == org.tensorflow.lite.DataType.FLOAT32) {
            float[][] out = new float[1][embeddingSize];
            tflite.run(inputBuffer, out);
            return out[0];
        } else { // UINT8 quantized
            byte[][] out = new byte[1][embeddingSize];
            tflite.run(inputBuffer, out);
            float[] embedding = new float[embeddingSize];
            for (int i = 0; i < embeddingSize; i++) {
                embedding[i] = (out[0][i] & 0xFF) / 255.0f; // normalize to [0,1]
            }
            return embedding;
        }
    }





}