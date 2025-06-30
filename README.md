# Context Monitor

## Overview
Context Monitor is an Android application designed to measure and monitor vital signs like heart rate and respiratory rate. Utilizing smartphone sensors and the camera, it captures health data and stores it locally, providing insights into physiological conditions.

## Features
- **Heart Rate Measurement:** Uses CameraX to record and analyze heart rate.
- **Respiratory Rate Measurement:** Utilizes accelerometer for capturing respiratory patterns.
- **Symptom Tracking:** Allows users to log symptoms using an interactive UI.
- **Local Data Storage:** Saves health data using Room Database.

## Technologies Used
- **Kotlin**: For Android development.
- **CameraX**: For camera functionalities.
- **Room**: Local database solution for data persistence.
- **View & Data Binding**: For efficient UI management.

## Setup Instructions

### Prerequisites
- Android Studio installed
- Minimum Android SDK level 29

### Getting Started
1. **Clone the Repository**
   ```sh
   git clone https://github.com/your_username/contextMonitor.git
   ```
2. **Open in Android Studio**
   - Launch Android Studio.
   - Select 'Open an existing Android Studio project' and choose the cloned directory.
3. **Build the Project**
   - Sync the project with Gradle files.
   - Run the app on an Android emulator or a physical device.

## Usage
- **Measure Heart Rate**: Navigate to the main screen and tap 'Heart Rate'.
- **Record Respiratory Rate**: Tap on 'Respiratory' to start measuring via accelerometer.
- **Log Symptoms**: Go to the 'Symptoms' section and provide entries for various symptoms.

## Future Enhancements
- **Cloud Sync**: Integration with cloud services for data backup.
- **Advanced Analytics**: Employing machine learning for predictive insights.
