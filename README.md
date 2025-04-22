# Screenshot Uploader App

This Android application runs in the background (without showing an app icon in the drawer) and automatically takes screenshots at regular intervals, uploading them to Firebase Storage.

## Key Features

- Background operation without visible app icon
- Automatic screenshots every 5 seconds
- Uploads all screenshots to Firebase Storage
- Starts automatically on device boot
- Tracks upload statistics

## Firebase Configuration

The app is already configured with Firebase. The google-services.json file is already in place in the app directory, so no additional setup is required for Firebase integration.

### Firebase Storage Configuration

If you need to modify the Firebase Storage settings:

1. In the Firebase console, go to "Storage"
2. You can update security rules for your storage as needed (the current rules should already be configured)

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;  // WARNING: For testing only! Change this in production
    }
  }
}
```

## How to Launch the App

Since the app doesn't have an icon in the app drawer, you can launch it using one of these methods:

1. **Using ADB (during development):**
   ```
   adb shell content query --uri content://com.settings.info.launcher/launch
   ```

2. **After device reboot:**
   The app will automatically start when the device boots up.

## Permissions Required

- `FOREGROUND_SERVICE` - For running the screenshot service in the foreground
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - For using media projection in a foreground service (Android 14+)
- `SYSTEM_ALERT_WINDOW` - For overlay permissions
- `WRITE_EXTERNAL_STORAGE` & `READ_EXTERNAL_STORAGE` - For saving screenshots locally
- `RECEIVE_BOOT_COMPLETED` - For starting on boot
- `INTERNET` & `ACCESS_NETWORK_STATE` - For uploading to Firebase

## Important Notes

1. This app requires manual permission granting for media projection after each device reboot.
2. This app is designed for legitimate use cases like remote monitoring with proper consent.

## Troubleshooting

If screenshots are not being uploaded to Firebase:

1. Verify that your Firebase Storage is correctly configured
2. Check for internet connectivity issues
3. Review the logcat output for any error messages (filter by "ScreenshotService" or "UploadTracker")

# Rubik's Cube Solver with Webcam

This application uses computer vision to detect and solve a Rubik's cube using your webcam. It overlays the solution steps on the screen to guide you through solving the cube.

## Requirements

- Python 3.7 or higher
- Webcam
- Required Python packages (install using `pip install -r requirements.txt`)

## Installation

1. Clone this repository
2. Install the required packages:
```bash
pip install -r requirements.txt
```

## Usage

1. Run the application:
```bash
python rubiks_cube_solver.py
```

2. Hold your Rubik's cube in front of the webcam
3. The application will show a 3x3 grid overlay on the video feed
4. Press 's' to capture the current face of the cube
5. Rotate the cube to show different faces and capture each face
6. Once all faces are captured, the solution will be displayed
7. Press 'q' to quit the application

## Controls

- 's': Capture the current face of the cube
- 'q': Quit the application

## How it Works

1. The application uses OpenCV to capture video from your webcam
2. It processes each frame to detect the colors of the cube faces
3. Using the Kociemba algorithm, it generates the optimal solution
4. The solution is displayed using Pygame, showing the sequence of moves needed to solve the cube

## Notes

- Ensure good lighting conditions for accurate color detection
- Hold the cube steady when capturing faces
- Make sure the cube fills most of the frame for better detection
- The application works best with a standard Rubik's cube with solid colors 