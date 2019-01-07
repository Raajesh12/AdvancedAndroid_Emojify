/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

class Emojifier {
    private static final String LOG_TAG = Emojifier.class.getSimpleName();

    private enum Emoji {
        BothEyesOpenSmile,
        BothEyesOpenFrown,
        BothEyesClosedSmile,
        BothEyesClosedFrown,
        LeftEyeOpenSmile,
        LeftEyeOpenFrown,
        RightEyeOpenSmile,
        RightEyeOpenFrown
    }

    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static final double SMILE_THRESHOLD = 0.2;
    private static final double LEFT_EYE_THRESHOLD = 0.54;
    private static final double RIGHT_EYE_THRESHOLD = 0.57;

    /**
     * Method for detecting faces in a bitmap.
     *
     * @param context The application context.
     * @param picture The picture in which to detect the faces.
     */
    static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap picture) {

        // Create the face detector, disable tracking and enable classifications
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        Bitmap resultBitmap = picture;

        // Build the frame
        Frame frame = new Frame.Builder().setBitmap(picture).build();

        // Detect the faces
        SparseArray<Face> faces = detector.detect(frame);

        // Log the number of faces
        Log.d(LOG_TAG, "detectFaces: number of faces = " + faces.size());

        // If there are no faces detected, show a Toast message
        if(faces.size() == 0){
            Toast.makeText(context, R.string.no_faces_message, Toast.LENGTH_SHORT).show();
        } else {
            for(int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);
                Emoji e = whichEmoji(face);

                Bitmap emojiBitmap = null;
                switch(e) {
                    case BothEyesOpenSmile:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.smile);
                        break;
                    case BothEyesOpenFrown:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frown);
                        break;
                    case BothEyesClosedSmile:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_smile);
                        break;
                    case BothEyesClosedFrown:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_frown);
                        break;
                    case LeftEyeOpenSmile:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwink);
                        break;
                    case LeftEyeOpenFrown:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwinkfrown);
                        break;
                    case RightEyeOpenSmile:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwink);
                        break;
                    case RightEyeOpenFrown:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwinkfrown);
                        break;
                }

                resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face);
            }
        }

        // Release the detector
        detector.release();

        return resultBitmap;
    }

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

    private static Emoji whichEmoji(Face face) {
        boolean leftEyeOpen;
        if(face.getIsLeftEyeOpenProbability() >= LEFT_EYE_THRESHOLD) {
            leftEyeOpen = true;
        } else {
            leftEyeOpen = false;
        }

        boolean rightEyeOpen;
        if(face.getIsRightEyeOpenProbability() >= RIGHT_EYE_THRESHOLD) {
            rightEyeOpen = true;
        } else {
            rightEyeOpen = false;
        }

        boolean isSmiling;
        if(face.getIsSmilingProbability() >= SMILE_THRESHOLD) {
            isSmiling = true;
        } else {
            isSmiling = false;
        }

        if(leftEyeOpen && rightEyeOpen && isSmiling) {
            return Emoji.BothEyesOpenSmile;
        } else if (leftEyeOpen && rightEyeOpen && !isSmiling) {
            return Emoji.BothEyesOpenFrown;
        } else if (!leftEyeOpen && !rightEyeOpen && isSmiling) {
            return Emoji.BothEyesClosedSmile;
        } else if (!leftEyeOpen && !rightEyeOpen && !isSmiling) {
            return Emoji.BothEyesClosedFrown;
        } else if (leftEyeOpen && !rightEyeOpen && isSmiling) {
            return Emoji.LeftEyeOpenSmile;
        } else if (leftEyeOpen && !rightEyeOpen && !isSmiling) {
            return Emoji.LeftEyeOpenFrown;
        } else if (!leftEyeOpen && rightEyeOpen && isSmiling) {
            return Emoji.RightEyeOpenSmile;
        } else {
            return Emoji.RightEyeOpenFrown;
        }
    }
}
