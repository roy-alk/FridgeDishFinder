# Fridge Dish Finder
## Description
Reduces your mealtime stress with just a quick scan of your fridge!

The app is made with Kotlin & the backend utilizes Python's Flask (I used Android Studio).
### How To Use:

#### First: you must set up the connection with the backend server.
Open the app folder, and under kotlin + java, open com.example.mycooking 

Under com.example.mycooking, open MainActivity.kt where you need to navigate to the UploadImage function.

Since the app utilizes the camera to scan your fridge, you need to replace this part in UploadImage() below with your local ip to run on flask's server 

#### val request = Request.Builder()
            // * REPLACE WITH "http://YOUR_IP:PORT/upload" TO RUN LOCALLY, 
            // FLASK SHOWS IT IN TERMINAL AFTER RUNNING *
            .url("http://YOUR_IP:5000/upload")
            .post(requestBody)
            .build()

#### Finally, we need the server to process the images to scan. To do that, open backend_server folder and in app_server.py, you need to input your API_KEY.

**Enjoy!**
