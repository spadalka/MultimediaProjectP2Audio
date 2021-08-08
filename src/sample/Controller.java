package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Controller {
    final FileChooser fileChooser = new FileChooser();
    private Stage stage;
    private Scene scene;
    private Parent root;

    private int totalFramesRead = 0;

    @FXML
    private Label originalFileSizeLabel;
    @FXML
    private Label compressedFileSizeLabel;
    @FXML
    private Label compressionRateLabel;

    public void openFileAudio(ActionEvent event) throws IOException {
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            originalFileSizeLabel.setText(file.length() + " bytes");
            extractAudioData(file);
        }
    }

    private void extractAudioData(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 1;
            }
            int numChannels = audioInputStream.getFormat().getChannels();
            int numBytes = (int) file.length();
            byte[] audioBytes = new byte[numBytes];
            short[] intervalDiffs = new short[numBytes/2];
            try {
                int numBytesRead = 0;
                int numFramesRead = 0;
                int x = 0;
                int y = 0;
                // Try to read numBytes bytes from the file.
                while (x < numBytes) {
                    numBytesRead = audioInputStream.read(audioBytes);
                    // Calculate the number of frames actually read.
                    numFramesRead = numBytesRead / bytesPerFrame;
                    totalFramesRead += numFramesRead;

                    if (numChannels == 1) {
                        if (x == 0) {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude1 = bb.getShort();
                            intervalDiffs[0] = audioAmplitude1;

                        } else {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x - 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitude2 = bb.getShort();

                            short difference = (short) (audioAmplitude2 - audioAmplitude1);
                            intervalDiffs[y] = difference;
                        }
                        x += 2;
                        y++;
                    } else if (numChannels == 2) {
                        if (x == 0) {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x + 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight = bb.getShort();

                            intervalDiffs[0] = (short) ((audioAmplitudeLeft+audioAmplitudeRight)/2); // mid channel
                            intervalDiffs[1] = (short) ((audioAmplitudeLeft-audioAmplitudeRight)/2); // side channel
                        } else {
                            ByteBuffer bb = ByteBuffer.wrap(audioBytes, x - 4, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x - 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight1 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeLeft2 = bb.getShort();

                            bb = ByteBuffer.wrap(audioBytes, x + 2, 2);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short audioAmplitudeRight2 = bb.getShort();

                            short diffChannel1 = (short) (audioAmplitudeLeft2 - audioAmplitudeLeft1);
                            short diffChannel2 = (short) (audioAmplitudeRight2 - audioAmplitudeRight1);


                            intervalDiffs[y] = (short) ((diffChannel1+diffChannel2)/2); // mid channel
                            intervalDiffs[y+1] = (short) ((diffChannel1-diffChannel2)/2); // side channel
                        }
                        x += 4;
                        y += 2;
                    }
                }

            } catch (Exception ex) {
                System.out.println("no1");
            }
        } catch (Exception e) {
            System.out.println("no2");
        }
    }

    public void compress(ActionEvent event) {
    }
}
