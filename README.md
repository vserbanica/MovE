# MovE
Assistance system for people with visual impairments.

This repository contains the source code for a prototype Android application that together with a smartwatch device is meant to send information to the wearer to protect him from everyday obstacles.

The information is received in 2 ways: 
 - different haptic feedback paaterns trough the watch
 - audio feedback using vocal messages from the phone

Android app - takes pictures once every second, sends them to an TenserFlow machine learning model for processing, the recognised objects are then filtered and send to the TextToSpeech service and a local WebSocket Server

Watch app - reads the Json data from the Server, parses the data and gives haptic output

# Prereqs

1. Make sure you have the latest version of Android Studio Installed and the latest Android SDK: https://developer.android.com/studio/index.html If you have never used Android Studio before, it should prompt you to download and install an Android SDK the first time you open the app.
2. Register for a Fitbit Dev Account: Go to https://dev.fitbit.com/apps and create and account. You'll need to use a valid email address and confirm it before continuing.
3. Open https://dev.fitbit.com/apps/new
4. Fill out the fields about your app. The first five fields (app name, description, website, organization, and organization website) will be specific to your organization and will be what users see in the app when giving permissions.

# Open the project using Android Studio
1. Clone the repo using git clone <this repo url>
2. Open Android Studio
3. Select Open an existing Android Studio project on the splash screen (or go to File → Open if the splash screen does not appear)
4. Open and wait for Android Studio to index the project

# Run!
Click on the "Bug" button to run the app in debug mode ( Run ). If you do not have a physical device, this will start an emulator and run the app there. If you do have a physical device and would like to use it for testing, follow this guide here: https://developer.android.com/studio/run/device.html
