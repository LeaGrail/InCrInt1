package com.example.incrint.audio;

import android.content.Context;
import android.media.AudioRecord;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CryInterpreter {

    private static final String TAG = CryInterpreter.class.getSimpleName();
    private static final String MODEL_PATH = "rfgbmmetamodel.tflite";
    private static final float PROBABILITY_THRESHOLD = 0.20f;

    private final AudioClassifier classifier;
    private TensorAudio tensor;
    private AudioRecord record;
    private TimerTask timerTask;
    private final BarChart barChart;
    private final List<BarEntry> barEntries = new ArrayList<>();

    // Define the cry labels
    private final String[] cryLabels = {"Tired", "Burping", "Discomfort", "Hungry", "Belly Pain"};

    public CryInterpreter(Context context, BarChart barChart) {
        this.barChart = barChart;
        initializeChart();
        classifier = loadModel(context);
    }

    private void initializeChart() {
        XAxis xAxis = barChart.getXAxis();
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(false); // Hide grid lines on X-axis
        xAxis.setValueFormatter(new IndexAxisValueFormatter(cryLabels)); // Set the cry labels on the X-axis

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawGridLines(false); // Hide grid lines on left Y-axis

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setDrawLabels(true);
        rightAxis.setDrawGridLines(false); // Hide grid lines on right Y-axis
    }

    private AudioClassifier loadModel(Context context) {
        try {
            return AudioClassifier.createFromFile(context, MODEL_PATH);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
            return null; // Handle model loading failure appropriately
        }
    }

    public void startRecording() {
        try {
            tensor = classifier.createInputTensorAudio();
            record = classifier.createAudioRecord();
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord initialization failed");
            }
            record.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            return;
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                // Load new audio data for classification
                tensor.load(record);
                List<Classifications> output = classifier.classify(tensor);
                for (Classifications classifications : output) {
                    processOutput(classifications);
                }
            }
        };
        new Timer().scheduleAtFixedRate(timerTask, 1, 1000);
    }

    private void processOutput(Classifications output) {
        List<Category> finalOutput = new ArrayList<>();
        for (Category category : output.getCategories()) {
            if (category.getScore() > PROBABILITY_THRESHOLD) {
                finalOutput.add(category);
            }
        }
        updateChart(finalOutput);
    }

    private void updateChart(List<Category> finalOutput) {
        barEntries.clear();

        // Ensure that finalOutput is not empty before proceeding
        if (finalOutput.isEmpty()) {
            return; // No predictions to display
        }

        for (int i = 0; i < finalOutput.size(); i++) {
            Category category = finalOutput.get(i);
            // Check if the category label matches any of the predefined labels
            for (int j = 0; j < cryLabels.length; j++) {
                if (category.getLabel().equalsIgnoreCase(cryLabels[j])) {
                    barEntries.add(new BarEntry(j, category.getScore() * 100)); // Convert to percentage
                    break; // Exit the loop once the label is found
                }
            }
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Cry Classification");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        barDataSet.setValueFormatter(new PercentFormatter());
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.invalidate(); // Refresh the chart
    }

    public void stopRecording() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (record != null) {
            record.stop();
            record.release();
        }
    }
}



