package com.example.incrint.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.incrint.R;
import com.example.incrint.audio.CryInterpreter;
import com.github.mikephil.charting.charts.BarChart;

public class InterpreterFragment extends Fragment {

    private BarChart barChart;
    private Button buttonStartRecording;
    private Button buttonStopRecording;
    private CryInterpreter cryInterpreter;
    private boolean permissionToRecordAccepted = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    permissionToRecordAccepted = true;
                    cryInterpreter = new CryInterpreter(requireContext(), barChart);
                } else {
                    permissionToRecordAccepted = false;
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interpreter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.barChart);
        buttonStartRecording = view.findViewById(R.id.buttonStartRecording);
        buttonStopRecording = view.findViewById(R.id.buttonStopRecording);

        // Request permission to record audio
        requestAudioPermission();

        buttonStartRecording.setOnClickListener(v -> {
            if (permissionToRecordAccepted) {
                buttonStartRecording.setEnabled(false);
                buttonStopRecording.setEnabled(true);
                if (cryInterpreter != null) {
                    cryInterpreter.startRecording();
                }
            } else {
                requestAudioPermission();
            }
        });

        buttonStopRecording.setOnClickListener(v -> {
            buttonStartRecording.setEnabled(true);
            buttonStopRecording.setEnabled(false);
            if (cryInterpreter != null) {
                cryInterpreter.stopRecording();
            }
        });

        // Disable the stop button initially
        buttonStopRecording.setEnabled(false);
    }

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            permissionToRecordAccepted = true;
            cryInterpreter = new CryInterpreter(requireContext(), barChart);
        }
    }
}



