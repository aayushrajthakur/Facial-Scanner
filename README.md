Face Recognition Android App

📖 Project Description

This Android application enables real‑time face detection, registration, and recognition using a combination of CameraX, ML Kit, TensorFlow Lite (FaceNet), and Firebase Firestore.

The app captures live camera frames, detects faces, generates embeddings using a deep learning model, and either stores them in Firestore (Register mode) or compares them against existing embeddings (Recognize mode). It is designed to demonstrate how modern mobile apps can integrate AI‑powered facial recognition with cloud storage for identity management.

✨ Key Features
CameraX Integration

Real‑time camera preview with front/back switching.

Efficient frame analysis using ImageAnalysis.

ML Kit Face Detection

Detects faces in live camera frames.

Provides bounding boxes for cropping and overlay visualization.

Face Preprocessing

Crops detected faces from the frame.

Resizes to 160×160 pixels and normalizes RGB values for model input.

TensorFlow Lite (FaceNet)

Generates 128‑dimensional embeddings for each face.

Embeddings represent unique facial features for comparison.

Firebase Firestore Integration

Register Mode → Prompts user for a name, saves embedding + timestamp.

Recognize Mode → Compares embeddings against Firestore using cosine similarity.

Threshold‑based matching (≥ 0.8 similarity).

UI Feedback

TextView overlay shows recognition results live (“Matched: [name]”, “No match found”).

Optional bounding box overlay (FaceOverlayView) highlights detected faces.

🏗️ Architecture
MainActivity

Handles camera lifecycle, mode switching, and Firestore integration.

CameraX Analyzer

Processes frames, detects faces, crops, preprocesses, and generates embeddings.

Helper Methods

toBitmap() → Converts YUV image to RGB bitmap.

preprocessFace() → Normalizes face bitmap for model input.

loadModelFile() → Loads FaceNet .tflite model from assets.

recognizeFace() → Compares embeddings with Firestore data.

cosineSimilarity() → Measures similarity between embeddings.

Firestore Schema Example

json
{
  "name": "John Doe",
  "embedding": [0.123, -0.456, ...], // 128 floats
  "createdAt": <timestamp>
}
🚀 How It Works
Launch the app → Camera preview starts automatically.

Switch camera → Tap the toggle button to switch front/back.

Register a face → Tap Register, enter a name in the dialog, and save.

Recognize a face → Tap Recognize, point the camera at a registered face, and see results live.

Firestore → Embeddings are stored and retrieved in real time.

📦 Requirements
Android Studio (latest version)

Minimum SDK: 23+

Dependencies:

CameraX

ML Kit Face Detection

TensorFlow Lite

Firebase Firestore

Permissions:

CAMERA

INTERNET

🔮 Future Enhancements (Optional)
Multi‑face detection with multiple bounding boxes.

Color‑coded overlays (blue for registering, green for matched, red for unknown).

Performance optimizations for low‑end devices.

User management (update/delete registered faces).

🧑‍💻 Author
Developed by Aayush Raj Thakur — Founder of Defencloud, architecting intelligent platforms blending AI, automation, and real‑time systems.
