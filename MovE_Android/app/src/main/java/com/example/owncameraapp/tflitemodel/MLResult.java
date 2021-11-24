package com.example.owncameraapp.tflitemodel;

/**
 * Results that can be send to Watch and TextToSpeech
 */
public enum MLResult {
    RESULT_GREEN_TRAFFIC_LIGHT(0),
    RESULT_RED_TRAFFIC_LIGHT(1),
    RESULT_OBSTACLE_AVOID_LEFT(2),
    RESULT_OBSTACLE_AVOID_RIGHT(3),
    RESULT_OBSTACLE_UNAVOIDABLE(4),
    RESULT_UNDEFINED(5);

    private int intValue;
    private MLResult(int value) {
        intValue = value;
    }

    public int toInt() {
        return intValue;
    }
}